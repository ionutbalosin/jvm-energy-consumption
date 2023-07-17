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
import static java.util.Optional.ofNullable;

import com.ionutbalosin.jvm.energy.consumption.formulas.AbstractFormulas;
import com.ionutbalosin.jvm.energy.consumption.formulas.EnergyFormulas;
import com.ionutbalosin.jvm.energy.consumption.formulas.TimeElapsedFormulas;
import com.ionutbalosin.jvm.energy.consumption.stats.PerfStats;
import com.ionutbalosin.jvm.energy.consumption.stats.ReportStats;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
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

  public SummaryReport(String module, List<PerfStats> perfStats, double meanPowerBaseline) {
    this.module = module;
    this.perfStats = perfStats;
    this.energyFormulas = new EnergyFormulas(meanPowerBaseline);
    this.timeElapsedFormulas = new TimeElapsedFormulas();
    this.basePath =
        String.format("%s/%s/results/%s/%s/jdk-%s", BASE_PATH, this.module, OS, ARCH, JDK_VERSION);
  }

  public void parseRawPerfStats() {
    // intentionally left blank
  }

  @Override
  public void printRawPerfStatsReport(String outputFilePath) throws IOException {
    try (PrintWriter writer = new PrintWriter(newBufferedWriter(Paths.get(outputFilePath)))) {
      writer.printf(
          "%18s;%26s;%16s;%27s;%23s;%15s\n",
          "Test Category",
          "Test Type",
          "Run Identifier",
          "Energy Package (Watt⋅sec)",
          "Energy RAM (Watt⋅sec)",
          "Elapsed (sec)");

      for (PerfStats perfStat : perfStats) {
        writer.printf(
            "%18s;%26s;%16s;%27.3f;%23.3f;%15.3f\n",
            perfStat.testCategory,
            ofNullable(perfStat.testType).orElse("NA"),
            perfStat.testRunIdentifier,
            perfStat.pkg,
            perfStat.ram,
            perfStat.elapsed);
      }
    }

    System.out.printf("Raw perf stats report %s was successfully created\n", outputFilePath);
  }

  @Override
  public void createReportStats() {
    for (String testCategory : TEST_CATEGORIES) {
      // exclude the "renaissance" tests since they did not run for all JVMs
      List<PerfStats> filteredPerfStats = getPerfStats(perfStats, "renaissance", testCategory);

      double energyGeometricMean = energyFormulas.getGeometricMean(filteredPerfStats);
      double energy = energyFormulas.getSum(filteredPerfStats);
      double carbonDioxide = energyFormulas.getCarbonDioxide(filteredPerfStats);
      reportStats.add(
          new ReportStats(
              testCategory, filteredPerfStats.size(), energy, energyGeometricMean, carbonDioxide));
    }
  }

  @Override
  public void printReportStats(String outputFilePath) throws IOException {
    try (PrintWriter writer = new PrintWriter(newBufferedWriter(Paths.get(outputFilePath)))) {
      writer.printf(
          "%18s;%9s;%19s;%34s;%34s;%22s\n",
          "Test Category",
          "Samples",
          "Energy (Watt⋅sec)",
          "Energy Geometric Mean (Watt⋅sec)",
          "Normalized Energy Geometric Mean",
          "CO₂ Emissions (gCO₂)");

      ReportStats referenceReportStat = getReportStatByCategory(reportStats, "openjdk-hotspot-vm");
      for (ReportStats reportStat : reportStats) {
        writer.printf(
            "%18s;%9d;%19.3f;%34.3f;%34.3f;%22.3f\n",
            reportStat.testCategory,
            reportStat.samples,
            reportStat.energy,
            reportStat.geoMeanEnergy,
            reportStat.geoMeanEnergy / referenceReportStat.geoMeanEnergy,
            reportStat.carbonDioxide);
      }
      writer.printf(
          "\n# Note1: The reference baseline has already been excluded from the energy scores");
      writer.printf(
          "\n"
              + "# Note2: '%s' was used as the reference value for calculating the normalized"
              + " geometric mean",
          referenceReportStat.testCategory);
      writer.printf(
          "\n# Note3: The carbon emission factor used was '%s'",
          AbstractFormulas.CARBON_DIOXIDE_EMISSION_FACTOR);
    }

    System.out.printf("Report stats %s was successfully created\n", outputFilePath);
  }

  private List<PerfStats> getPerfStats(
      List<PerfStats> perfStats, String module, String testCategory) {
    return perfStats.stream()
        .filter(perfStat -> !module.equals(perfStat.module))
        .filter(perfStat -> testCategory.equals(perfStat.testCategory))
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
