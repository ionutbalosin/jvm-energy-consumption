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
import static com.ionutbalosin.jvm.energy.consumption.EnergyReportCalculator.OS;
import static java.nio.file.Files.newBufferedWriter;

import com.ionutbalosin.jvm.energy.consumption.formulas.PowerFormulas;
import com.ionutbalosin.jvm.energy.consumption.stats.PowerStats;
import com.ionutbalosin.jvm.energy.consumption.stats.ReportStats;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;

public class BaselineReport extends AbstractReport {

  PowerFormulas powerFormulas;
  public double baselinePower;

  public BaselineReport(String module) {
    this.module = module;
    this.powerFormulas = new PowerFormulas();
    this.basePath = String.format("%s/%s/results/%s/%s", BASE_PATH, this.module, ARCH, OS);
  }

  @Override
  public void reportRawPowerStats(String outputFilePath) {
    // Note: this report does not print anything
  }

  @Override
  public void createReportStats() {
    resetReportStats();

    if (powerStats.isEmpty()) {
      return;
    }

    for (PowerStats powerStat : powerStats) {
      baselinePower = powerFormulas.getGeometricMean(powerStat);
      reportStats.add(new ReportStats(powerStat.category, powerStat.samples.size(), baselinePower));
    }
  }

  @Override
  public void printReportStats(String outputFilePath) throws IOException {
    if (reportStats.isEmpty()) {
      return;
    }

    try (PrintWriter writer = new PrintWriter(newBufferedWriter(Paths.get(outputFilePath)))) {
      writer.printf("%18s;%15s;%29s\n", "Category", "Power Samples", "Power Geometric Mean (Watt)");

      for (ReportStats reportStat : reportStats) {
        writer.printf(
            "%18s;%15d;%29.3f\n",
            reportStat.category, reportStat.samples, reportStat.geoMeanEnergy);
      }
    }

    System.out.printf("Report stats %s was successfully created\n", outputFilePath);
  }
}
