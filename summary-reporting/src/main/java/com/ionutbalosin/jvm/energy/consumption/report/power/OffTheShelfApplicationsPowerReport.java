/**
 * JVM Energy Consumption
 *
 * Copyright (C) 2023-2025 Ionut Balosin
 * Website:      www.ionutbalosin.com
 * Social Media:
 *   LinkedIn:   ionutbalosin
 *   Bluesky:    @ionutbalosin.bsky.social
 *   X:          @ionutbalosin
 *   Mastodon:   ionutbalosin@mastodon.social
 *
 *  MIT License
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 *
 */
package com.ionutbalosin.jvm.energy.consumption.report.power;

import static com.ionutbalosin.jvm.energy.consumption.util.EnergyUtils.ARCH;
import static com.ionutbalosin.jvm.energy.consumption.util.EnergyUtils.BASE_PATH;
import static com.ionutbalosin.jvm.energy.consumption.util.EnergyUtils.JDK_VERSION;
import static com.ionutbalosin.jvm.energy.consumption.util.EnergyUtils.OS;
import static java.nio.file.Files.newBufferedWriter;

import com.ionutbalosin.jvm.energy.consumption.formulas.PowerFormulas;
import com.ionutbalosin.jvm.energy.consumption.stats.power.PowerStats;
import com.ionutbalosin.jvm.energy.consumption.stats.power.ReportPowerStats;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;

public class OffTheShelfApplicationsPowerReport extends AbstractPowerReport {

  PowerFormulas powerFormulas;
  double baselinePower;

  public OffTheShelfApplicationsPowerReport(String module, double baselinePower) {
    this.baselinePower = baselinePower;
    this.powerFormulas = new PowerFormulas();
    this.basePath =
        String.format(
            "%s/off-the-shelf-applications/%s/results/jdk-%s/%s/%s",
            BASE_PATH, module, JDK_VERSION, ARCH, OS);
  }

  @Override
  public void reportRawStats(String outputFilePath) throws IOException {
    if (rawStats.isEmpty()) {
      return;
    }

    try (PrintWriter writer = new PrintWriter(newBufferedWriter(Paths.get(outputFilePath)))) {
      writer.printf(
          "%18s;%16s;%19s;%14s;%14s\n",
          "Category", "Run Identifier", "Sample Identifier", "Score", "Score Metric");

      for (PowerStats powerStats : rawStats) {
        for (int i = 0; i < powerStats.samples.size(); i++) {
          PowerStats.PowerSample sample = powerStats.samples.get(i);
          writer.printf(
              "%18s;%16s;%19d;%14.3f;%14s\n",
              powerStats.descriptor.category,
              powerStats.descriptor.runIdentifier,
              i + 1,
              sample.watts,
              "Power (Watt)");
        }
      }
    }

    System.out.printf("Report %s was successfully created\n", outputFilePath);
  }

  @Override
  public void processRawStats() {
    resetProcessedStats();

    for (PowerStats powerStats : rawStats) {
      powerStats.energy = powerFormulas.getEnergy(powerStats, baselinePower);

      processedStats.add(
          new ReportPowerStats(
              powerStats.descriptor.category,
              powerStats.descriptor.runIdentifier,
              powerStats.samples.size(),
              powerStats.energy));
    }
  }

  @Override
  public void reportProcessedStats(String outputFilePath) throws IOException {
    if (processedStats.isEmpty()) {
      return;
    }

    try (PrintWriter writer = new PrintWriter(newBufferedWriter(Paths.get(outputFilePath)))) {
      writer.printf(
          "%18s;%16s;%16s;%14s;%19s\n",
          "Category", "Run Identifier", "Energy Samples", "Score", "Score Metric");

      for (ReportPowerStats report : processedStats) {
        writer.printf(
            "%18s;%16s;%16d;%14.3f;%19s\n",
            report.descriptor.category,
            report.descriptor.runIdentifier,
            report.samples,
            report.energy / 3600,
            "Energy (Watt⋅hour)");
      }
      writer.printf(
          "\n# Note: The power reference baseline has already been excluded from the total energy");
    }

    System.out.printf("Report %s was successfully created\n", outputFilePath);
  }
}
