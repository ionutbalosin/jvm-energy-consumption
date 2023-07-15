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
import static com.ionutbalosin.jvm.energy.consumption.rapl.report.EnergyReportCalculator.OS;
import static java.nio.file.Files.newBufferedWriter;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import com.ionutbalosin.jvm.energy.consumption.formulas.AbstractFormulas;
import com.ionutbalosin.jvm.energy.consumption.formulas.PowerFormulas;
import com.ionutbalosin.jvm.energy.consumption.stats.PerfStats;
import com.ionutbalosin.jvm.energy.consumption.stats.ReportStats;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class BaselineReport extends AbstractReport {

  AbstractFormulas powerFormulas;
  // the baseline mean power to be subtracted from any other measurements
  public double meanPower;

  public BaselineReport(String module) {
    this.module = module;
    this.powerFormulas = new PowerFormulas();
    this.reportStats = new ArrayList<>();
    this.basePath = String.format("%s/%s/results/%s/%s", BASE_PATH, this.module, OS, ARCH);
  }

  @Override
  public void setPerfStats(List<PerfStats> perfStats) {
    this.perfStats =
        perfStats.stream()
            .collect(
                groupingBy(
                    perfStat -> perfStat.testCategory,
                    TreeMap::new,
                    mapping(identity(), toList())));
  }

  @Override
  public void createReportStats() {
    // Note: there should be only one key entry in the map
    for (Map.Entry<String, List<PerfStats>> pair : perfStats.entrySet()) {
      meanPower = powerFormulas.getMean(pair.getValue());
      double meanErrorPower = powerFormulas.getMeanError(pair.getValue());
      reportStats.add(
          new ReportStats(
              this.module, pair.getKey(), pair.getValue().size(), meanPower, meanErrorPower));
    }
  }

  @Override
  public void printReportStats(String outputFilePath) throws IOException {
    try (PrintWriter writer = new PrintWriter(newBufferedWriter(Paths.get(outputFilePath)))) {
      writer.printf(
          "%18s;%9s;%19s;%27s\n",
          "Test Category", "Samples", "Power Mean (Watt)", "Power Score Error (90.0%)");

      for (ReportStats reportStat : reportStats) {
        writer.printf(
            "%18s;%9d;%19.3f;%27.3f\n",
            reportStat.testCategory,
            reportStat.samples,
            reportStat.meanPower,
            reportStat.meanErrorPower);
      }
    }

    System.out.printf("Report stats %s was successfully created\n", outputFilePath);
  }

  @Override
  public void printRawPerfStatsReport(String outputFilePath) throws IOException {
    try (PrintWriter writer = new PrintWriter(newBufferedWriter(Paths.get(outputFilePath)))) {
      writer.printf(
          "%18s;%16s;%27s;%23s;%15s\n",
          "Test Category",
          "Run Identifier",
          "Energy Package (Watt⋅sec)",
          "Energy RAM (Watt⋅sec)",
          "Elapsed (sec)");

      for (Map.Entry<String, List<PerfStats>> pair : perfStats.entrySet()) {
        for (PerfStats perfStat : pair.getValue()) {
          writer.printf(
              "%18s;%16s;%27.3f;%23.3f;%15.3f\n",
              perfStat.testCategory,
              perfStat.testRunIdentifier,
              perfStat.pkg,
              perfStat.ram,
              perfStat.elapsed);
        }
      }
    }

    System.out.printf("Raw perf stats report %s was successfully created\n", outputFilePath);
  }
}
