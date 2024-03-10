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

import static com.ionutbalosin.jvm.energy.consumption.util.EnergyUtils.ARCH;
import static com.ionutbalosin.jvm.energy.consumption.util.EnergyUtils.BASE_PATH;
import static com.ionutbalosin.jvm.energy.consumption.util.EnergyUtils.OS;
import static java.nio.file.Files.newBufferedWriter;

import com.ionutbalosin.jvm.energy.consumption.formulas.PowerFormulas;
import com.ionutbalosin.jvm.energy.consumption.stats.power.PowerStats;
import com.ionutbalosin.jvm.energy.consumption.stats.power.ReportPowerStats;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;

public class BaselinePowerReport extends AbstractPowerReport {

  PowerFormulas powerFormulas;
  public double baselinePower;

  public BaselinePowerReport(String module) {
    this.powerFormulas = new PowerFormulas();
    this.basePath = String.format("%s/%s/results/%s/%s", BASE_PATH, module, ARCH, OS);
  }

  @Override
  public void reportRawPowerStats(String outputFilePath) {
    // Note: this report does not print anything
  }

  @Override
  public void createReportStats() {
    resetReportPowerStats();

    if (powerStats.isEmpty()) {
      return;
    }

    for (PowerStats powerStat : powerStats) {
      baselinePower = powerFormulas.getGeometricMean(powerStat);
      reportPowerStats.add(
          new ReportPowerStats(
              powerStat.descriptor.category, powerStat.samples.size(), baselinePower));
    }
  }

  @Override
  public void reportPowerStats(String outputFilePath) throws IOException {
    if (reportPowerStats.isEmpty()) {
      return;
    }

    try (PrintWriter writer = new PrintWriter(newBufferedWriter(Paths.get(outputFilePath)))) {
      writer.printf("%18s;%15s;%29s\n", "Category", "Power Samples", "Power Geometric Mean (Watt)");

      for (ReportPowerStats report : reportPowerStats) {
        writer.printf(
            "%18s;%15d;%29.3f\n", report.descriptor.category, report.samples, report.geoMeanEnergy);
      }
    }

    System.out.printf("Report stats %s was successfully created\n", outputFilePath);
  }
}
