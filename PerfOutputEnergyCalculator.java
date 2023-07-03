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
package scripts;

import java.io.BufferedReader;
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

public class PerfOutputEnergyCalculator {

    private static final String BASE_PATH = Paths.get(".").toAbsolutePath().normalize().toString();
    private static final String OUTPUT_FOLDER = "summary";
    private static final String JDK_VERSION = "17";
    private static final String ARCH = "x86_64";
    private static final List<String> JVM_BASED_APPLICATION_LIST = List.of("spring-petclinic", "quarkus-hibernate-orm-panache-quickstart", "renaissance");
    private static final List<String> NON_JVM_BASED_APPLICATION_LIST = List.of("baseline-idle-os");

    public static void main(String[] args) throws IOException {
        for (String application : JVM_BASED_APPLICATION_LIST) {
            System.out.printf("Calculate consumed energy for '%s'\n", application);
            calculateJvmBasedSummaryReport(String.format("%s/%s/results/jdk-%s/%s/perf", BASE_PATH, application, JDK_VERSION, ARCH));
        }

        for (String application : NON_JVM_BASED_APPLICATION_LIST) {
            System.out.printf("Calculate consumed energy for '%s'\n", application);
            calculateNonJvmBasedSummaryReport(application, String.format("%s/%s/results/%s/perf", BASE_PATH, application, ARCH));
        }
    }

    private static void calculateJvmBasedSummaryReport(String path) throws IOException {
        String parentSummaryPath = path + "/../" + OUTPUT_FOLDER;
        List<PerfStats> stats = readFiles(path);
        Files.createDirectories(Paths.get(parentSummaryPath));

        Map<String, List<PerfStats>> statsByJvmName = stats.stream().collect(groupingBy(perfStat -> perfStat.jvmName, TreeMap::new, mapping(identity(), toList())));
        double referenceJvmGeometricMean = geometricMean(statsByJvmName.get("openjdk-hotspot-vm"));
        try (PrintWriter writer = new PrintWriter(newBufferedWriter(Paths.get(parentSummaryPath + "/power-consumption.csv")))) {
            writer.printf("%18s;%33s;%26s\n", "JVM", "Geometric Mean (Watt per second)", "Normalized Geometric Mean");
            for (Map.Entry<String, List<PerfStats>> pair : statsByJvmName.entrySet()) {
                double jvmGeometricMean = geometricMean(pair.getValue());
                writer.printf("%18s;%33.3f;%26.3f\n", pair.getKey(), jvmGeometricMean, jvmGeometricMean / referenceJvmGeometricMean);
            }
        }
    }

    private static void calculateNonJvmBasedSummaryReport(String testType, String path) throws IOException {
        String parentSummaryPath = path + "/../" + OUTPUT_FOLDER;
        List<PerfStats> stats = readFiles(path);
        Files.createDirectories(Paths.get(parentSummaryPath));

        try (PrintWriter writer = new PrintWriter(newBufferedWriter(Paths.get(parentSummaryPath + "/power-consumption.csv")))) {
            writer.printf("%18s;%33s\n", "Test", "Geometric Mean (Watt per second)");
            double geometricMean = geometricMean(stats);
            writer.printf("%18s;%33.3f\n", testType, geometricMean);
        }
    }

    private static List<PerfStats> readFiles(String parentFolder) throws IOException {
        return Files.walk(Paths.get(parentFolder)).filter(Files::isRegularFile).map(PerfOutputEnergyCalculator::parseStats).collect(toList());
    }

    private static PerfStats parseStats(Path filePath) {
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

            // extract the jvm name and test type from the file name
            String fileName = filePath.getFileName().toString();
            perfStats.jvmName = fileName.substring(0, fileName.indexOf("-run-"));
            perfStats.testRunIdentifier = fileName.substring(fileName.lastIndexOf("-") + 1, fileName.indexOf(".stats"));

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

    public static double geometricMean(List<PerfStats> perfStats) {
        double prod = 1;
        for (PerfStats perfStat : perfStats) {
            double watts = (perfStat.pkg + perfStat.ram) / perfStat.elapsed;
            prod *= watts;
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
        String jvmName;
        String testRunIdentifier;
    }
}
