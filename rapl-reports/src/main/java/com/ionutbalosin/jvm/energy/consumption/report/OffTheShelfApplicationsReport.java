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

import static com.ionutbalosin.jvm.energy.consumption.rapl.report.EnergyReportCalculator.ARCH;
import static com.ionutbalosin.jvm.energy.consumption.rapl.report.EnergyReportCalculator.BASE_PATH;
import static com.ionutbalosin.jvm.energy.consumption.rapl.report.EnergyReportCalculator.JDK_VERSION;
import static com.ionutbalosin.jvm.energy.consumption.rapl.report.EnergyReportCalculator.OS;
import static java.nio.file.Files.newBufferedWriter;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import com.ionutbalosin.jvm.energy.consumption.formulas.AbstractFormulas;
import com.ionutbalosin.jvm.energy.consumption.formulas.EnergyFormulas;
import com.ionutbalosin.jvm.energy.consumption.formulas.TimeElapsedFormulas;
import com.ionutbalosin.jvm.energy.consumption.stats.PerfStats;
import com.ionutbalosin.jvm.energy.consumption.stats.ReportStats;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class OffTheShelfApplicationsReport extends AbstractReport {

  AbstractFormulas energyFormulas;
  AbstractFormulas timeElapsedFormulas;

  public OffTheShelfApplicationsReport(String module, double meanPowerBaseline) {
    this.module = module;
    this.energyFormulas = new EnergyFormulas(meanPowerBaseline);
    this.timeElapsedFormulas = new TimeElapsedFormulas();
    this.basePath =
        String.format("%s/%s/results/%s/%s/jdk-%s", BASE_PATH, this.module, OS, ARCH, JDK_VERSION);
  }

  public OffTheShelfApplicationsReport(String module, String category, double meanPowerBaseline) {
    this.module = module;
    this.energyFormulas = new EnergyFormulas(meanPowerBaseline);
    this.timeElapsedFormulas = new TimeElapsedFormulas();
    this.basePath =
        String.format(
            "%s/%s/results/%s/%s/jdk-%s/%s",
            BASE_PATH, this.module, OS, ARCH, JDK_VERSION, category);
  }

  @Override
  public void printRawPerfStatsReport(String outputFilePath) throws IOException {
    if (perfStats.isEmpty()) {
      return;
    }

    try (PrintWriter writer = new PrintWriter(newBufferedWriter(Paths.get(outputFilePath)))) {
      writer.printf(
          "%18s;%16s;%27s;%23s;%15s\n",
          "Test Category",
          "Run Identifier",
          "Energy Package (Watt⋅sec)",
          "Energy RAM (Watt⋅sec)",
          "Elapsed (sec)");

      for (Map.Entry<String, List<PerfStats>> pair : getPerfStatsAsMap().entrySet()) {
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

  @Override
  public void createReportStats() {
    resetReportStats();

    for (Map.Entry<String, List<PerfStats>> pair : getPerfStatsAsMap().entrySet()) {
      double meanEnergy = energyFormulas.getMean(pair.getValue());
      double meanErrorEnergy = energyFormulas.getMeanError(pair.getValue());
      double meanTimeElapsed = timeElapsedFormulas.getMean(pair.getValue());
      double meanErrorTimeElapsed = timeElapsedFormulas.getMeanError(pair.getValue());
      reportStats.add(
          new ReportStats(
              this.module,
              pair.getKey(),
              pair.getValue().size(),
              meanEnergy,
              meanErrorEnergy,
              meanTimeElapsed,
              meanErrorTimeElapsed));
    }
  }

  @Override
  public void printReportStats(String outputFilePath) throws IOException {
    if (reportStats.isEmpty()) {
      return;
    }

    try (PrintWriter writer = new PrintWriter(newBufferedWriter(Paths.get(outputFilePath)))) {
      writer.printf(
          "%18s;%9s;%24s;%28s;%20s;%29s\n",
          "Test Category",
          "Samples",
          "Energy Mean (Watt⋅sec)",
          "Energy Score Error (90.0%)",
          "Elapsed Mean (sec)",
          "Elapsed Score Error (90.0%)");

      for (ReportStats reportStat : reportStats) {
        writer.printf(
            "%18s;%9d;%24.3f;%28.3f;%20.3f;%29.3f\n",
            reportStat.testCategory,
            reportStat.samples,
            reportStat.meanEnergy,
            reportStat.meanErrorEnergy,
            reportStat.meanTimeElapsed,
            reportStat.meanErrorTimeElapsed);
      }
      writer.printf(
          "\n# Note: The reference baseline has already been excluded from the energy scores");
    }

    System.out.printf("Report stats %s was successfully created\n", outputFilePath);
  }

  private Map<String, List<PerfStats>> getPerfStatsAsMap() {
    return perfStats.stream()
        .collect(
            groupingBy(
                perfStat -> perfStat.testCategory, TreeMap::new, mapping(identity(), toList())));
  }
}
