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
package com.ionutbalosin.jvm.energy.consumption.report;

import static com.ionutbalosin.jvm.energy.consumption.rapl.report.EnergyReportCalculator.ARCH;
import static com.ionutbalosin.jvm.energy.consumption.rapl.report.EnergyReportCalculator.BASE_PATH;
import static com.ionutbalosin.jvm.energy.consumption.rapl.report.EnergyReportCalculator.JDK_VERSION;
import static com.ionutbalosin.jvm.energy.consumption.rapl.report.EnergyReportCalculator.OS;
import static java.nio.file.Files.newBufferedWriter;

import com.ionutbalosin.jvm.energy.consumption.formulas.geomean.AbstractFormulas;
import com.ionutbalosin.jvm.energy.consumption.formulas.geomean.EnergyFormulas;
import com.ionutbalosin.jvm.energy.consumption.formulas.geomean.TimeElapsedFormulas;
import com.ionutbalosin.jvm.energy.consumption.stats.ReportStats;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SummaryReport extends AbstractReport {

  List<String> TEST_CATEGORIES =
      List.of(
          "openjdk-hotspot-vm",
          "graalvm-ce",
          "graalvm-ee",
          "native-image",
          "eclipse-openj9-vm",
          "azul-prime-vm");

  AbstractFormulas energyFormulas;
  AbstractFormulas timeElapsedFormulas;
  public List<ReportStats> reportStatsSummary;

  public SummaryReport(String module, List<ReportStats> reportStats) {
    this.module = module;
    this.energyFormulas = new EnergyFormulas();
    this.timeElapsedFormulas = new TimeElapsedFormulas();
    this.reportStats = reportStats;
    this.reportStatsSummary = new ArrayList<>();
    this.basePath =
        String.format("%s/%s/results/%s/%s/jdk-%s", BASE_PATH, this.module, OS, ARCH, JDK_VERSION);
  }

  @Override
  public void printRawPerfStatsReport(String outputFilePath) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void createReportStats() {
    // filter "renaissance" reports since they were not run for all JVMs (e.g., native image)
    List<ReportStats> filteredReportStats = excludeReportStatsByModule(reportStats, "renaissance");

    // for each test category calculate the geometric mean
    for (String testCategory : TEST_CATEGORIES) {
      List<ReportStats> reportStatsByCategory =
          getReportStatsByCategory(filteredReportStats, testCategory);
      double energyGeometricMean = energyFormulas.getGeometricMean(reportStatsByCategory);
      double timeElapsedGeometricMean = timeElapsedFormulas.getGeometricMean(reportStatsByCategory);
      reportStatsSummary.add(
          new ReportStats(
              testCategory,
              reportStatsByCategory.size(),
              energyGeometricMean,
              timeElapsedGeometricMean));
    }
  }

  @Override
  public void printReportStats(String outputFilePath) throws IOException {
    try (PrintWriter writer = new PrintWriter(newBufferedWriter(Paths.get(outputFilePath)))) {
      writer.printf(
          "%18s;%9s;%34s;%34s;%30s;%35s\n",
          "Test Category",
          "Samples",
          "Energy Geometric Mean (Watt⋅sec)",
          "Normalized Energy Geometric Mean",
          "Elapsed Geometric Mean (sec)",
          "Normalized Elapsed Geometric Mean");

      ReportStats referenceReportStat =
          getReportStatByCategory(reportStatsSummary, "openjdk-hotspot-vm");
      for (ReportStats reportStat : reportStatsSummary) {
        writer.printf(
            "%18s;%9d;%34.3f;%34.3f;%30.3f;%35.3f\n",
            reportStat.testCategory,
            reportStat.samples,
            reportStat.geoMeanEnergy,
            reportStat.geoMeanEnergy / referenceReportStat.geoMeanEnergy,
            reportStat.geoMeanTimeElapsed,
            reportStat.geoMeanTimeElapsed / referenceReportStat.geoMeanTimeElapsed);
      }
      writer.printf(
          "\n"
              + "# Note: '%s' was used as the reference value for calculating the normalized"
              + " geometric mean",
          referenceReportStat.testCategory);
    }

    System.out.printf("Report stats %s was successfully created\n", outputFilePath);
  }

  private List<ReportStats> excludeReportStatsByModule(
      List<ReportStats> reportStats, String module) {
    return reportStats.stream()
        .filter(reportStat -> !module.equals(reportStat.module))
        .collect(Collectors.toList());
  }

  private List<ReportStats> getReportStatsByCategory(
      List<ReportStats> reportStats, String testCategory) {
    return reportStats.stream()
        .filter(reportStat -> testCategory.equals(reportStat.testCategory))
        .collect(Collectors.toList());
  }

  private ReportStats getReportStatByCategory(List<ReportStats> reportStats, String testCategory) {
    return reportStats.stream()
        .filter(reportStat -> testCategory.equals(reportStat.testCategory))
        .findAny()
        .orElseThrow(
            () ->
                new RuntimeException(
                    "Unable to find any report stat for category " + testCategory));
  }
}
