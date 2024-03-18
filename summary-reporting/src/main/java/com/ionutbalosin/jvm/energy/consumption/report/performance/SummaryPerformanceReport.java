/*
 * JVM Energy Consumption
 *
 * MIT License
 *
 * Copyright (c) 2023-2024 Ionut Balosin, Ko Turk
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
package com.ionutbalosin.jvm.energy.consumption.report.performance;

import static com.ionutbalosin.jvm.energy.consumption.util.EnergyUtils.ARCH;
import static com.ionutbalosin.jvm.energy.consumption.util.EnergyUtils.BASE_PATH;
import static com.ionutbalosin.jvm.energy.consumption.util.EnergyUtils.JDK_VERSION;
import static com.ionutbalosin.jvm.energy.consumption.util.EnergyUtils.OS;
import static java.nio.file.Files.newBufferedWriter;

import com.ionutbalosin.jvm.energy.consumption.formulas.PowerFormulas;
import com.ionutbalosin.jvm.energy.consumption.stats.ExecutionType;
import com.ionutbalosin.jvm.energy.consumption.stats.performance.PerformanceStats;
import com.ionutbalosin.jvm.energy.consumption.stats.performance.ReportPerformanceStats;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SummaryPerformanceReport extends AbstractPerformanceReport {

  final String REFERENCE_JVM = "openjdk-hotspot-vm";

  PowerFormulas energyFormulas;

  public SummaryPerformanceReport(String module) {
    this.energyFormulas = new PowerFormulas();
    this.basePath =
        String.format("%s/%s/results/jdk-%s/%s/%s", BASE_PATH, module, JDK_VERSION, ARCH, OS);
  }

  @Override
  public void parseRawStats(ExecutionType perfType) {
    // intentionally left blank
    // Note: this report does not parse anything, it just receives all other power stats reports
  }

  @Override
  public void reportRawStats(String outputFilePath) throws IOException {
    // Note: this report does not print anything
  }

  @Override
  public void processRawStats() {
    resetProcessedStats();

    if (rawStats.isEmpty()) {
      return;
    }

    final Set<String> categories = getCategories(rawStats);
    final Set<String> runIdentifiers = getRunIdentifiers(rawStats);
    final Set<String> invalidResultTypes = getInvalidResultTypes(rawStats);
    for (String category : categories) {
      for (String runIdentifier : runIdentifiers) {
        final List<PerformanceStats> performanceStats =
            getPerfStats(rawStats, category, runIdentifier);
        if (performanceStats.isEmpty()) {
          continue;
        }
        final List<PerformanceStats> filteredPerformanceStats =
            filterInvalidPerformanceStats(performanceStats, invalidResultTypes);

        final double perfGeometricMean = energyFormulas.getGeometricMean(filteredPerformanceStats);
        processedStats.add(
            new ReportPerformanceStats(
                category, runIdentifier, filteredPerformanceStats.size(), perfGeometricMean));
      }
    }
  }

  @Override
  public void reportProcessedStats(String outputFilePath) throws IOException {
    if (processedStats.isEmpty()) {
      return;
    }

    try (PrintWriter writer = new PrintWriter(newBufferedWriter(Paths.get(outputFilePath)))) {
      writer.printf(
          "%18s;%16s;%13s;%23s;%23s\n",
          "Category",
          "Run Identifier",
          "Total Tests",
          "Throughput (ops/sec)",
          "Normalised Throughput");

      final double referenceValue = getReferenceValue(processedStats, REFERENCE_JVM);
      for (ReportPerformanceStats report : processedStats) {
        writer.printf(
            "%18s;%16s;%13d;%23.3f;%23.3f\n",
            report.descriptor.category,
            report.descriptor.runIdentifier,
            report.samples,
            report.value,
            report.value / referenceValue);
      }
      writer.printf("\n# Note: The reference value for normalized data is '%s'", REFERENCE_JVM);
    }

    System.out.printf("Report %s was successfully created\n", outputFilePath);
  }

  private List<PerformanceStats> getPerfStats(
      List<PerformanceStats> performanceStats, String category, String runIdentifier) {
    return performanceStats.stream()
        .filter(
            perfStat ->
                category.equals(perfStat.descriptor.category)
                    && runIdentifier.equals(perfStat.descriptor.runIdentifier))
        .collect(Collectors.toList());
  }

  private static Set<String> getCategories(List<PerformanceStats> performanceStats) {
    return performanceStats.stream()
        .map(performanceStat -> performanceStat.descriptor.category)
        .collect(Collectors.toSet());
  }

  private static Set<String> getRunIdentifiers(List<PerformanceStats> performanceStats) {
    return performanceStats.stream()
        .map(performanceStat -> performanceStat.descriptor.runIdentifier)
        .collect(Collectors.toSet());
  }

  // This method retrieves the types of invalid results. Invalid results are those where the energy
  // consumption is zero, indicating errors during test execution.
  private static Set<String> getInvalidResultTypes(List<PerformanceStats> performanceStats) {
    return performanceStats.stream()
        .filter(ps -> ps.value == 0)
        .map(ps -> ps.descriptor.type)
        .collect(Collectors.toSet());
  }

  // Filter the invalid tests based on their type.
  // Note: This method assumes that the type is unique across all tests. If there might be
  // duplications, the module or category should also be included in the comparison.
  private static List<PerformanceStats> filterInvalidPerformanceStats(
      List<PerformanceStats> performanceStats, Set<String> invalidResultTypes) {
    return performanceStats.stream()
        .filter(ps -> !invalidResultTypes.contains(ps.descriptor.type))
        .collect(Collectors.toList());
  }

  private static double getReferenceValue(
      List<ReportPerformanceStats> processedStats, String category) {
    return processedStats.stream()
        .filter(
            reportPerformanceStat ->
                category.equalsIgnoreCase((reportPerformanceStat.descriptor.category)))
        .map(reportPerformanceStat -> reportPerformanceStat.value)
        .findAny()
        .orElseThrow(() -> new RuntimeException("Unable to find any category for " + category));
  }
}
