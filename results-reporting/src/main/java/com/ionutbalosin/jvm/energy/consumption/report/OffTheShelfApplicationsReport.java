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
package com.ionutbalosin.jvm.energy.consumption.report;

import static com.ionutbalosin.jvm.energy.consumption.EnergyReportCalculator.ARCH;
import static com.ionutbalosin.jvm.energy.consumption.EnergyReportCalculator.BASE_PATH;
import static com.ionutbalosin.jvm.energy.consumption.EnergyReportCalculator.JDK_VERSION;
import static com.ionutbalosin.jvm.energy.consumption.EnergyReportCalculator.OS;
import static java.nio.file.Files.newBufferedWriter;

import com.ionutbalosin.jvm.energy.consumption.formulas.PowerFormulas;
import com.ionutbalosin.jvm.energy.consumption.stats.PowerStats;
import com.ionutbalosin.jvm.energy.consumption.stats.ReportStats;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;

public class OffTheShelfApplicationsReport extends AbstractReport {

  PowerFormulas powerFormulas;
  double baselinePower;

  public OffTheShelfApplicationsReport(String module, double baselinePower) {
    this.module = module;
    this.baselinePower = baselinePower;
    this.powerFormulas = new PowerFormulas();
    this.basePath =
        String.format(
            "%s/off-the-shelf-applications/%s/results/jdk-%s/%s/%s",
            BASE_PATH, this.module, JDK_VERSION, ARCH, OS);
  }

  @Override
  public void reportRawPowerStats(String outputFilePath) {
    // Note: this report does not print anything
  }

  @Override
  public void createReportStats() {
    resetReportStats();

    for (PowerStats powerStat : powerStats) {
      powerStat.energy = powerFormulas.getEnergy(powerStat, baselinePower);

      reportStats.add(
          new ReportStats(
              powerStat.category,
              powerStat.runIdentifier,
              powerStat.samples.size(),
              powerStat.energy));
    }
  }

  @Override
  public void printReportStats(String outputFilePath) throws IOException {
    if (reportStats.isEmpty()) {
      return;
    }

    try (PrintWriter writer = new PrintWriter(newBufferedWriter(Paths.get(outputFilePath)))) {
      writer.printf(
          "%18s;%16s;%16s;%29s\n",
          "Category", "Run Identifier", "Energy Samples", "Total Energy (Watt⋅sec)");

      for (ReportStats reportStat : reportStats) {
        writer.printf(
            "%18s;%16s;%16d;%29.3f\n",
            reportStat.category, reportStat.runIdentifier, reportStat.samples, reportStat.energy);
      }
      writer.printf(
          "\n# Note: The reference baseline has already been excluded from the total energy");
    }

    System.out.printf("Report stats %s was successfully created\n", outputFilePath);
  }
}
