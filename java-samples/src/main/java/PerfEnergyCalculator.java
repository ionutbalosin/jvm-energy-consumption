/*
 * JVM Energy Consumption
 *
 * MIT License
 *
 * Copyright (c) 2023 Ionut Balosin
 * Copyright (c) 2023 Ko Turk
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.newBufferedWriter;
import static java.util.function.Function.identity;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

public class PerfEnergyCalculator {

    private static final String BASE_PATH = Paths.get(".").toAbsolutePath().normalize().toString();
    private static final String OUTPUT_FOLDER = "summary";
    private static final String GEOMETRIC_MEAN_OUTPUT_FILE = "power-consumption.csv";
    private static final String PERF_STATS_OUTPUT_FILE = "power-consumption-perf-stats.csv";
    private static final String OS = "linux";
    private static final String ARCH = "x86_64";
    private static final String JDK_VERSION = "17";

    private static final Application OS_BASELINE = new Application("baseline-idle-os");
    private static final List<Application> OFF_THE_SHELF_APPLICATIONS = List.of(new Application("spring-petclinic", "openjdk-hotspot-vm"), new Application("quarkus-hibernate-orm-panache-quickstart", "openjdk-hotspot-vm"), new Application("renaissance", "openjdk-hotspot-vm"));
    private static final List<Application> JAVA_SAMPLES = List.of(new Application("ThrowExceptionPatterns", "openjdk-hotspot-vm-override_fist"), new Application("MemoryAccessPatterns", "openjdk-hotspot-vm-linear"), new Application("LoggingPatterns", "openjdk-hotspot-vm-lambda_heap"));

    public static void main(String[] args) throws IOException {
        calculateEnergy(new OsBaselineEnergyReport(OS_BASELINE));

        for (Application application : OFF_THE_SHELF_APPLICATIONS) {
            calculateEnergy(new OffTheShelfApplicationEnergyReport(application));
        }

        for (Application application : JAVA_SAMPLES) {
            calculateEnergy(new JavaSamplesEnergyReport(application));
        }
    }

    private static void calculateEnergy(EnergyReportCreator energyReport) throws IOException {
        System.out.printf("Calculate energy for '%s'\n", energyReport.application.name);

        String perfStatsPath = energyReport.getPerfStatsPath();
        List<PerfStats> perfStats = readFiles(perfStatsPath);

        String outputPath = new File(perfStatsPath + "/../" + OUTPUT_FOLDER).getCanonicalPath();
        Files.createDirectories(Paths.get(outputPath));

        String perfStatsOutputFile = outputPath + "/" + PERF_STATS_OUTPUT_FILE;
        energyReport.createPerfStatsReport(perfStats, perfStatsOutputFile);

        String geometricMeanOutputFile = outputPath + "/" + GEOMETRIC_MEAN_OUTPUT_FILE;
        energyReport.createGeometricMeanReport(perfStats, geometricMeanOutputFile);

        System.out.println();
    }

    private static List<PerfStats> readFiles(String parentFolder) throws IOException {
        return Files.walk(Paths.get(parentFolder)).filter(Files::isRegularFile).map(PerfStatsParser::parseStats).collect(toList());
    }

    public static double geometricMean(List<PerfStats> perfStats) {
        double prod = 1;
        for (PerfStats perfStat : perfStats) {
            // pkg includes the cores and gpu
            // Note: on laptop battery the psys counters does not display proper stats
            double energy = perfStat.pkg + perfStat.ram;
            prod *= energy;
        }
        return Math.pow(prod, 1.0 / perfStats.size());
    }

    static class PerfStats {
        double cores;
        double gpu;
        double pkg;
        double psys;
        double ram;
        double elapsed;
        String jvmIdentifier;
        String testType;
        String testRunIdentifier;
    }

    static class PerfStatsParser {
        public static PerfStats parseStats(Path filePath) {
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath.toFile()), UTF_8))) {

                PerfStats perfStats = new PerfStats();
                bufferedReader.lines().map(String::trim).filter(not(String::isBlank)).map(line -> line.split(" ")).filter(lines -> lines.length > 2).forEach(words -> {
                    switch (words[2]) {
                        case "power/energy-cores/":
                            perfStats.cores = stringToDouble(words[0]);
                            break;
                        case "power/energy-gpu/":
                            perfStats.gpu = stringToDouble(words[0]);
                            break;
                        case "power/energy-pkg/":
                            perfStats.pkg = stringToDouble(words[0]);
                            break;
                        case "power/energy-psys/":
                            perfStats.psys = stringToDouble(words[0]);
                            break;
                        case "power/energy-ram/":
                            perfStats.ram = stringToDouble(words[0]);
                            break;
                        case "time":
                            perfStats.elapsed = stringToDouble(words[0]);
                            break;
                    }
                });

                // extract the jvm identifier, test type and test run identifier from the output file name
                // Note: this entire logic relies on a very specific file name convention: "<jvm_identifier>-run-<test_type>-<test_run_identifier>.stats"
                // Example:
                //  -filename: "openjdk-hotspot-vm-run-guarded_parametrized-1.log"
                //  - jvm identifier: "openjdk-hotspot-vm"
                //  - test type: "guarded_parametrized"
                //  - test run identifier: "1"
                String fileName = filePath.getFileName().toString();
                int runIndex = fileName.indexOf("-run-");
                int statsIndex = fileName.indexOf(".stats");
                int lastDashIndex = fileName.lastIndexOf("-");
                int beforeLastDashIndex = fileName.lastIndexOf("-", lastDashIndex - 1);
                perfStats.jvmIdentifier = fileName.substring(0, runIndex);
                // add test type only if they exist in the file name format
                if (runIndex != beforeLastDashIndex) {
                    perfStats.testType = fileName.substring(beforeLastDashIndex + 1, lastDashIndex);
                }
                perfStats.testRunIdentifier = fileName.substring(lastDashIndex + 1, statsIndex);

                return perfStats;

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private static double stringToDouble(String statValue) {
            try {
                DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
                symbols.setDecimalSeparator(',');
                symbols.setGroupingSeparator('.');
                DecimalFormat format = new DecimalFormat("#,###.##", symbols);
                return format.parse(statValue).doubleValue();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    static class Application {
        String name;
        String refGeometricMean;

        public Application(String name) {
            this.name = name;
        }

        public Application(String name, String refGeometricMean) {
            this.name = name;
            this.refGeometricMean = refGeometricMean;
        }
    }

    static abstract class EnergyReportCreator {
        Application application;

        public EnergyReportCreator(Application application) {
            this.application = application;
        }

        abstract String getPerfStatsPath();

        abstract void createPerfStatsReport(List<PerfStats> perfStats, String outputFilePath) throws IOException;

        abstract void createGeometricMeanReport(List<PerfStats> perfStats, String outputFilePath) throws IOException;
    }

    static class OsBaselineEnergyReport extends EnergyReportCreator {
        public OsBaselineEnergyReport(Application application) {
            super(application);
        }

        @Override
        String getPerfStatsPath() {
            // Note: this is a specific path format for this application type.
            return String.format("%s/%s/results/%s/%s/perf", BASE_PATH, application.name, OS, ARCH);
        }

        @Override
        void createGeometricMeanReport(List<PerfStats> perfStats, String outputFilePath) throws IOException {
            try (PrintWriter writer = new PrintWriter(newBufferedWriter(Paths.get(outputFilePath)))) {
                writer.printf("%18s;%30s;%9s\n", "Test Type", "Geometric Mean (Watt second)", "Samples");
                double geometricMean = geometricMean(perfStats);
                writer.printf("%18s;%30.3f;%9d\n", perfStats.get(0).jvmIdentifier, geometricMean, perfStats.size());
            }

            System.out.printf("Geometric mean report %s was successfully created\n", outputFilePath);
        }

        @Override
        void createPerfStatsReport(List<PerfStats> perfStats, String outputFilePath) throws IOException {
            try (PrintWriter writer = new PrintWriter(newBufferedWriter(Paths.get(outputFilePath)))) {
                writer.printf("%18s;%16s;%30s;%26s;%19s\n", "Test", "Run Identifier", "Energy package (Watt second)", "Energy RAM (Watt second)", "Elapsed (seconds)");
                for (PerfStats perfStat : perfStats) {
                    writer.printf("%18s;%16s;%30.3f;%26.3f;%19.3f\n", perfStat.jvmIdentifier, perfStat.testRunIdentifier, perfStat.pkg, perfStat.ram, perfStat.elapsed);
                }
            }

            System.out.printf("Perf stats report %s was successfully created\n", outputFilePath);
        }
    }

    static class OffTheShelfApplicationEnergyReport extends EnergyReportCreator {
        public OffTheShelfApplicationEnergyReport(Application application) {
            super(application);
        }

        @Override
        String getPerfStatsPath() {
            // Note: this is a specific path format for this application type.
            return String.format("%s/%s/results/%s/%s/jdk-%s/perf", BASE_PATH, application.name, OS, ARCH, JDK_VERSION);
        }

        @Override
        void createGeometricMeanReport(List<PerfStats> perfStats, String outputFilePath) throws IOException {
            Map<String, List<PerfStats>> statsByJvmName = perfStats.stream().collect(groupingBy(perfStat -> perfStat.jvmIdentifier, TreeMap::new, mapping(identity(), toList())));
            double refGeometricMean = geometricMean(statsByJvmName.get(application.refGeometricMean));

            try (PrintWriter writer = new PrintWriter(newBufferedWriter(Paths.get(outputFilePath)))) {
                writer.printf("%18s;%30s;%27s;%9s\n", "JVM", "Geometric Mean (Watt second)", "Normalized Geometric Mean", "Samples");
                for (Map.Entry<String, List<PerfStats>> pair : statsByJvmName.entrySet()) {
                    double geometricMean = geometricMean(pair.getValue());
                    writer.printf("%18s;%30.3f;%27.3f;%9d\n", pair.getKey(), geometricMean, geometricMean / refGeometricMean, pair.getValue().size());
                }
                writer.printf("\n# Note: The reference value '%s' was considered for the normalized geometric mean", application.refGeometricMean);
            }

            System.out.printf("Geometric mean report %s was successfully created\n", outputFilePath);
        }

        @Override
        void createPerfStatsReport(List<PerfStats> perfStats, String outputFilePath) throws IOException {
            try (PrintWriter writer = new PrintWriter(newBufferedWriter(Paths.get(outputFilePath)))) {
                writer.printf("%18s;%16s;%30s;%26s;%19s\n", "JVM", "Run Identifier", "Energy package (Watt second)", "Energy RAM (Watt second)", "Elapsed (seconds)");

                Map<String, List<PerfStats>> statsByJvmName = perfStats.stream().collect(groupingBy(perfStat -> perfStat.jvmIdentifier, TreeMap::new, mapping(identity(), toList())));
                for (Map.Entry<String, List<PerfStats>> pair : statsByJvmName.entrySet()) {
                    for (PerfStats perfStat : pair.getValue()) {
                        writer.printf("%18s;%16s;%30.3f;%26.3f;%19.3f\n", perfStat.jvmIdentifier, perfStat.testRunIdentifier, perfStat.pkg, perfStat.ram, perfStat.elapsed);
                    }
                }
            }

            System.out.printf("Perf stats report %s was successfully created\n", outputFilePath);
        }
    }

    static class JavaSamplesEnergyReport extends EnergyReportCreator {
        public JavaSamplesEnergyReport(Application application) {
            super(application);
        }

        @Override
        String getPerfStatsPath() {
            // Note: this is a specific path format for this application type.
            return String.format("%s/java-samples/results/%s/%s/jdk-%s/%s/perf", BASE_PATH, OS, ARCH, JDK_VERSION, application.name);
        }

        @Override
        void createGeometricMeanReport(List<PerfStats> perfStats, String outputFilePath) throws IOException {
            Map<String, List<PerfStats>> statsByJvmNameAndType = perfStats.stream().collect(groupingBy(perfStat -> perfStat.jvmIdentifier + "-" + perfStat.testType, TreeMap::new, mapping(identity(), toList())));
            double refGeometricMean = geometricMean(statsByJvmNameAndType.get(application.refGeometricMean));

            try (PrintWriter writer = new PrintWriter(newBufferedWriter(Paths.get(outputFilePath)))) {
                writer.printf("%18s;%26s;%30s;%27s;%9s\n", "JVM", "Test Type", "Geometric Mean (Watt second)", "Normalized Geometric Mean", "Samples");
                for (Map.Entry<String, List<PerfStats>> pair : statsByJvmNameAndType.entrySet()) {
                    double geometricMean = geometricMean(pair.getValue());
                    writer.printf("%18s;%26s;%30.3f;%27.3f;%9d\n", pair.getValue().get(0).jvmIdentifier, pair.getValue().get(0).testType, geometricMean, geometricMean / refGeometricMean, pair.getValue().size());
                }
                writer.printf("\n# Note: The reference value '%s' was considered for the normalized geometric mean", application.refGeometricMean);
            }

            System.out.printf("Geometric mean report %s was successfully created\n", outputFilePath);
        }

        @Override
        void createPerfStatsReport(List<PerfStats> perfStats, String outputFilePath) throws IOException {
            try (PrintWriter writer = new PrintWriter(newBufferedWriter(Paths.get(outputFilePath)))) {
                writer.printf("%18s;%26s;%16s;%30s;%26s;%19s\n", "JVM", "Test Type", "Run Identifier", "Energy package (Watt second)", "Energy RAM (Watt second)", "Elapsed (seconds)");

                Map<String, List<PerfStats>> statsByJvmNameAndType = perfStats.stream().collect(groupingBy(perfStat -> perfStat.jvmIdentifier + "-" + perfStat.testType, TreeMap::new, mapping(identity(), toList())));
                for (Map.Entry<String, List<PerfStats>> pair : statsByJvmNameAndType.entrySet()) {
                    for (PerfStats perfStat : pair.getValue()) {
                        writer.printf("%18s;%26s;%16s;%30.3f;%26.3f;%19.3f\n", perfStat.jvmIdentifier, perfStat.testType, perfStat.testRunIdentifier, perfStat.pkg, perfStat.ram, perfStat.elapsed);
                    }
                }
            }

            System.out.printf("Perf stats report %s was successfully created\n", outputFilePath);
        }
    }

}
