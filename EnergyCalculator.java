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

public class EnergyCalculator {

    private static final String BASE_PATH = Paths.get(".").toAbsolutePath().normalize().toString();
    private static final List<String> APPLICATION_LIST = List.of("spring-petclinic", "quarkus-hibernate-orm-panache-quickstart");
    private static final String JDK_VERSION = "17";
    private static final String ARCH = "x86_64";

    public static void main(String[] args) throws IOException {
        for (String application : APPLICATION_LIST) {
            System.out.printf("Calculate consumed energy for the '%s' application\n", application);
            calculateSummaryReport(String.format("%s/%s/results/jdk-%s/%s/perf", BASE_PATH, application, JDK_VERSION, ARCH));
        }
    }

    private static void calculateSummaryReport(String path) throws IOException {
        String parentSummaryPath = path + "/../summary";
        List<PerfStats> stats = readFiles(path);
        Files.createDirectories(Paths.get(parentSummaryPath));

        Map<String, List<PerfStats>> statsByJvmName = stats.stream().collect(groupingBy(perfStat -> perfStat.jvmName, TreeMap::new, mapping(identity(), toList())));
        double openJdkHotSpotGeometricMean = geometricMean(statsByJvmName.get("openjdk-hotspot")); // reference geometric mean
        try (PrintWriter writer = new PrintWriter(newBufferedWriter(Paths.get(parentSummaryPath + "/jvm.power")))) {
            writer.printf("JVM distribution; Power consumption (Watt per second); Power consumption (normalized)\n");
            for (Map.Entry<String, List<PerfStats>> pair : statsByJvmName.entrySet()) {
                double jvmGeometricMean = geometricMean(pair.getValue());
                writer.printf("%16s;%36.3f;%31.3f\n", pair.getKey(), jvmGeometricMean, jvmGeometricMean / openJdkHotSpotGeometricMean);
            }
        }

        Map<String, List<PerfStats>> statsByJvmNameAndType = stats.stream().collect(groupingBy(perfStat -> perfStat.jvmName + "-run-" + perfStat.testRunIdentifier, TreeMap::new, mapping(identity(), toList())));
        try (PrintWriter writer = new PrintWriter(newBufferedWriter(Paths.get(parentSummaryPath + "/jvm-benchmark.power")))) {
            writer.printf("JVM distribution (run identifier); Power consumption (Watt per second)\n");
            for (Map.Entry<String, List<PerfStats>> pair : statsByJvmNameAndType.entrySet()) {
                double jvmGeometricMean = geometricMean(pair.getValue());
                writer.printf("%33s;%36.3f\n", pair.getKey(), jvmGeometricMean);
            }
        }
    }

    private static List<PerfStats> readFiles(String parentFolder) throws IOException {
        return Files.walk(Paths.get(parentFolder)).filter(Files::isRegularFile).map(EnergyCalculator::parseStats).collect(toList());
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
        return perfStats.size() == 1 ? prod : Math.pow(prod, 1.0 / perfStats.size());
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
