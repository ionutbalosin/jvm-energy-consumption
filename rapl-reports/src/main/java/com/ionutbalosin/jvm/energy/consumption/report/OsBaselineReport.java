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
import static com.ionutbalosin.jvm.energy.consumption.stats.PerfStatsStatistics.getEnergy;
import static com.ionutbalosin.jvm.energy.consumption.stats.PerfStatsStatistics.getGeometricMean;
import static com.ionutbalosin.jvm.energy.consumption.stats.PerfStatsStatistics.getMean;
import static com.ionutbalosin.jvm.energy.consumption.stats.PerfStatsStatistics.getMeanError;
import static java.nio.file.Files.newBufferedWriter;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import com.ionutbalosin.jvm.energy.consumption.Application;
import com.ionutbalosin.jvm.energy.consumption.stats.PerfStats;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class OsBaselineReport extends AbstractReport {
  public OsBaselineReport(Application application) {
    super(application);
  }

  @Override
  public String getPerfStatsPath() {
    // Note: this is a specific path format for this application type.
    return String.format("%s/%s/results/%s/%s/perf", BASE_PATH, application.name, OS, ARCH);
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
  public void createMeanReport(String outputFilePath) throws IOException {
    double refGeometricMean = getGeometricMean(perfStats.get(application.refGeometricMean));

    try (PrintWriter writer = new PrintWriter(newBufferedWriter(Paths.get(outputFilePath)))) {
      writer.printf(
          "%18s;%9s;%17s;%21s;%27s;%27s\n",
          "Test Category",
          "Samples",
          "Mean (Watt⋅sec)",
          "Score Error (90.0%)",
          "Geometric Mean (Watt⋅sec)",
          "Normalized Geometric Mean");
      for (Map.Entry<String, List<PerfStats>> pair : perfStats.entrySet()) {
        double mean = getMean(pair.getValue());
        double meanError = getMeanError(pair.getValue());
        double geometricMean = getGeometricMean(pair.getValue());
        writer.printf(
            "%18s;%9d;%17.3f;%21s;%27.3f;%27.3f\n",
            pair.getKey(),
            pair.getValue().size(),
            mean,
            String.format("± %.3f", meanError),
            geometricMean,
            geometricMean / refGeometricMean);
      }
      writer.printf(
          "\n# Note: The reference value '%s' was considered for the normalized geometric mean",
          application.refGeometricMean);
    }

    System.out.printf("Geometric mean report %s was successfully created\n", outputFilePath);
  }

  @Override
  public void createPerfStatsReport(String outputFilePath) throws IOException {
    try (PrintWriter writer = new PrintWriter(newBufferedWriter(Paths.get(outputFilePath)))) {
      writer.printf(
          "%18s;%16s;%27s;%23s;%18s;%15s\n",
          "Test Category",
          "Run Identifier",
          "Energy Package (Watt⋅sec)",
          "Energy RAM (Watt⋅sec)",
          "Total (Watt⋅sec)",
          "Elapsed (sec)");

      for (Map.Entry<String, List<PerfStats>> pair : perfStats.entrySet()) {
        for (PerfStats perfStat : pair.getValue()) {
          writer.printf(
              "%18s;%16s;%27.3f;%23.3f;%18.3f;%15.3f\n",
              perfStat.testCategory,
              perfStat.testRunIdentifier,
              perfStat.pkg,
              perfStat.ram,
              getEnergy(perfStat),
              perfStat.elapsed);
        }
      }
    }

    System.out.printf("Perf stats report %s was successfully created\n", outputFilePath);
  }
}
