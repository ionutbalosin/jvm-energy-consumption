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
import static com.ionutbalosin.jvm.energy.consumption.util.EnergyUtils.JDK_VERSION;
import static com.ionutbalosin.jvm.energy.consumption.util.EnergyUtils.OS;
import static java.nio.file.Files.newBufferedWriter;
import static java.util.Optional.ofNullable;

import com.ionutbalosin.jvm.energy.consumption.formulas.PowerFormulas;
import com.ionutbalosin.jvm.energy.consumption.stats.power.PowerStats;
import com.ionutbalosin.jvm.energy.consumption.stats.power.ReportPowerStats;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;

public class JavaSamplesPowerReport extends AbstractPowerReport {

  PowerFormulas powerFormulas;
  double baselinePower;

  public JavaSamplesPowerReport(String module, String category, double baselinePower) {
    this.baselinePower = baselinePower;
    this.powerFormulas = new PowerFormulas();
    this.basePath =
        String.format(
            "%s/%s/results/jdk-%s/%s/%s/%s", BASE_PATH, module, JDK_VERSION, ARCH, OS, category);
  }

  @Override
  public void reportRawPowerStats(String outputFilePath) {
    // Note: this report does not print anything
  }

  @Override
  public void createReportStats() {
    resetReportPowerStats();

    for (PowerStats powerStat : powerStats) {
      powerStat.energy = powerFormulas.getEnergy(powerStat, baselinePower);

      reportPowerStats.add(
          new ReportPowerStats(
              powerStat.descriptor.category,
              powerStat.descriptor.type,
              powerStat.descriptor.runIdentifier,
              powerStat.samples.size(),
              powerStat.energy));
    }
  }

  @Override
  public void reportPowerStats(String outputFilePath) throws IOException {
    if (reportPowerStats.isEmpty()) {
      return;
    }

    try (PrintWriter writer = new PrintWriter(newBufferedWriter(Paths.get(outputFilePath)))) {
      writer.printf(
          "%18s;%26s;%16s;%16s;%24s\n",
          "Category", "Type", "Run Identifier", "Energy Samples", "Total Energy (Watt⋅sec)");

      for (ReportPowerStats report : reportPowerStats) {
        writer.printf(
            "%18s;%26s;%16s;%16d;%24.3f\n",
            report.descriptor.category,
            ofNullable(report.descriptor.type).orElse("N/A"),
            report.descriptor.runIdentifier,
            report.samples,
            report.energy);
      }
      writer.printf(
          "\n# Note: The reference baseline has already been excluded from the energy scores");
    }

    System.out.printf("Report stats %s was successfully created\n", outputFilePath);
  }
}
