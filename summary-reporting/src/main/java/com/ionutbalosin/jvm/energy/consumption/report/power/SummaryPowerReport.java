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
package com.ionutbalosin.jvm.energy.consumption.report.power;

import static com.ionutbalosin.jvm.energy.consumption.formulas.PowerFormulas.CARBON_DIOXIDE_EMISSION_FACTOR;
import static com.ionutbalosin.jvm.energy.consumption.util.EnergyUtils.ARCH;
import static com.ionutbalosin.jvm.energy.consumption.util.EnergyUtils.BASE_PATH;
import static com.ionutbalosin.jvm.energy.consumption.util.EnergyUtils.JDK_VERSION;
import static com.ionutbalosin.jvm.energy.consumption.util.EnergyUtils.OS;
import static java.nio.file.Files.newBufferedWriter;
import static java.util.Optional.ofNullable;

import com.ionutbalosin.jvm.energy.consumption.formulas.PowerFormulas;
import com.ionutbalosin.jvm.energy.consumption.stats.ExecutionType;
import com.ionutbalosin.jvm.energy.consumption.stats.power.PowerStats;
import com.ionutbalosin.jvm.energy.consumption.stats.power.ReportPowerStats;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SummaryPowerReport extends AbstractPowerReport {
  PowerFormulas energyFormulas;
  double baselinePower;

  public SummaryPowerReport(String module, double baselinePower) {
    this.baselinePower = baselinePower;
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
    if (rawStats.isEmpty()) {
      return;
    }

    try (PrintWriter writer = new PrintWriter(newBufferedWriter(Paths.get(outputFilePath)))) {
      writer.printf(
          "%18s;%26s;%16s;%29s;%15s\n",
          "Category", "Type", "Run Identifier", "Total Energy (Watt⋅sec)", "Elapsed (sec)");

      for (PowerStats powerStats : rawStats) {
        writer.printf(
            "%18s;%26s;%16s;%29.3f;%15.3f\n",
            powerStats.descriptor.category,
            ofNullable(powerStats.descriptor.type).orElse("N/A"),
            powerStats.descriptor.runIdentifier,
            powerStats.energy,
            powerStats.elapsed);
      }
    }

    System.out.printf("Report %s was successfully created\n", outputFilePath);
  }

  @Override
  public void processRawStats() {
    resetProcessedStats();

    if (rawStats.isEmpty()) {
      return;
    }

    final Set<String> categories = getCategories(rawStats);
    final Set<String> runIdentifiers = getRunIdentifiers(rawStats);
    for (String category : categories) {
      for (String runIdentifier : runIdentifiers) {
        List<PowerStats> categoryPerfStats = getPerfStats(rawStats, category, runIdentifier);
        if (categoryPerfStats.isEmpty()) {
          continue;
        }

        final double totalEnergy = energyFormulas.getEnergy(categoryPerfStats);
        final double carbonDioxide = energyFormulas.getCarbonDioxide(totalEnergy);
        processedStats.add(
            new ReportPowerStats(
                category, runIdentifier, categoryPerfStats.size(), totalEnergy, carbonDioxide));
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
          "%18s;%16s;%14s;%25s;%22s\n",
          "Category",
          "Run Identifier",
          "Test Samples",
          "Total Energy (Watt⋅sec)",
          "CO₂ Emissions (gCO₂)");

      for (ReportPowerStats report : processedStats) {
        writer.printf(
            "%18s;%16s;%14d;%25.3f;%22.3f\n",
            report.descriptor.category,
            report.descriptor.runIdentifier,
            report.samples,
            report.energy,
            report.carbonDioxide);
      }
      writer.printf(
          "\n"
              + "# Note1: The power reference baseline has already been excluded from the energy"
              + " scores");
      writer.printf(
          "\n# Note2: The carbon emission factor used was '%s'", CARBON_DIOXIDE_EMISSION_FACTOR);
    }

    System.out.printf("Report %s was successfully created\n", outputFilePath);
  }

  private List<PowerStats> getPerfStats(
      List<PowerStats> powerStats, String category, String runIdentifier) {
    return powerStats.stream()
        .filter(
            perfStat ->
                category.equals(perfStat.descriptor.category)
                    && runIdentifier.equals(perfStat.descriptor.runIdentifier))
        .collect(Collectors.toList());
  }

  private static Set<String> getCategories(List<PowerStats> powerStats) {
    return powerStats.stream()
        .map(powerStat -> powerStat.descriptor.category)
        .collect(Collectors.toSet());
  }

  private static Set<String> getRunIdentifiers(List<PowerStats> powerStats) {
    return powerStats.stream()
        .map(powerStat -> powerStat.descriptor.runIdentifier)
        .collect(Collectors.toSet());
  }
}
