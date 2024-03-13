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

import com.ionutbalosin.jvm.energy.consumption.stats.performance.PerformanceStats;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;

public class OffTheShelfApplicationsPerformanceReport extends AbstractPerformanceReport {

  public OffTheShelfApplicationsPerformanceReport(String module) {
    this.basePath =
        String.format(
            "%s/off-the-shelf-applications/%s/results/jdk-%s/%s/%s/wrk",
            BASE_PATH, module, JDK_VERSION, ARCH, OS);
  }

  public void reportRawStats(String outputFilePath) throws IOException {
    if (rawStats.isEmpty()) {
      return;
    }

    try (PrintWriter writer = new PrintWriter(newBufferedWriter(Paths.get(outputFilePath)))) {
      writer.printf("%18s;%16s;%22s\n", "Category", "Run Identifier", "Throughput (Ops/sec)");

      for (PerformanceStats performanceStats : rawStats) {
        writer.printf(
            "%18s;%16s;%22.3f\n",
            performanceStats.descriptor.category,
            performanceStats.descriptor.runIdentifier,
            performanceStats.value);
      }
    }

    System.out.printf("Report %s was successfully created\n", outputFilePath);
  }
}
