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
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import com.ionutbalosin.jvm.energy.consumption.formulas.EnergyFormulas;
import com.ionutbalosin.jvm.energy.consumption.formulas.StatisticsFormulas;
import com.ionutbalosin.jvm.energy.consumption.perfstats.Stats;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class JavaSamplesReport extends AbstractReport {

  StatisticsFormulas energyFormulas;

  public JavaSamplesReport(String module, String category, double meanPowerBaseline) {
    this.energyFormulas = new EnergyFormulas(meanPowerBaseline);
    this.perfStatsPath =
        String.format(
            "%s/%s/results/%s/%s/jdk-%s/%s/perf",
            BASE_PATH, module, OS, ARCH, JDK_VERSION, category);
  }

  @Override
  public void setPerfStats(List<Stats> perfStats) {
    this.perfStats =
        perfStats.stream()
            .collect(
                groupingBy(
                    perfStat -> perfStat.testCategory + "-" + perfStat.testType,
                    TreeMap::new,
                    mapping(identity(), toList())));
  }

  @Override
  public void createMeanReport(String outputFilePath) throws IOException {
    try (PrintWriter writer = new PrintWriter(newBufferedWriter(Paths.get(outputFilePath)))) {
      writer.printf(
          "%18s;%26s;%9s;%24s;%28s;%34s\n",
          "Test Category",
          "Test Type",
          "Samples",
          "Energy Mean (Watt⋅sec)",
          "Energy Score Error (90.0%)",
          "Energy Geometric Mean (Watt⋅sec)");

      for (Map.Entry<String, List<Stats>> pair : perfStats.entrySet()) {
        double meanEnergy = energyFormulas.getMean(pair.getValue());
        double meanErrorEnergy = energyFormulas.getMeanError(pair.getValue());
        double geometricMeanEnergy = energyFormulas.getGeometricMean(pair.getValue());
        writer.printf(
            "%18s;%26s;%9d;%24.3f;%28.3f;%34.3f\n",
            pair.getValue().get(0).testCategory,
            pair.getValue().get(0).testType,
            pair.getValue().size(),
            meanEnergy,
            meanErrorEnergy,
            geometricMeanEnergy);
      }
      writer.printf("\n# Note: The reference baseline is already excluded from this report");
    }

    System.out.printf("Mean report %s was successfully created\n", outputFilePath);
  }

  @Override
  public void createRawPerfStatsReport(String outputFilePath) throws IOException {

    try (PrintWriter writer = new PrintWriter(newBufferedWriter(Paths.get(outputFilePath)))) {
      writer.printf(
          "%18s;%26s;%16s;%27s;%23s;%15s\n",
          "Test Category",
          "Test Type",
          "Run Identifier",
          "Energy Package (Watt⋅sec)",
          "Energy RAM (Watt⋅sec)",
          "Elapsed (sec)");

      for (Map.Entry<String, List<Stats>> pair : perfStats.entrySet()) {
        for (Stats perfStat : pair.getValue()) {
          writer.printf(
              "%18s;%26s;%16s;%27.3f;%23.3f;%15.3f\n",
              perfStat.testCategory,
              perfStat.testType,
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
