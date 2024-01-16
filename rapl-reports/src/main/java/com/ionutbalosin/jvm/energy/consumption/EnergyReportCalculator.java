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
package com.ionutbalosin.jvm.energy.consumption.rapl.report;

import com.ionutbalosin.jvm.energy.consumption.report.AbstractReport;
import com.ionutbalosin.jvm.energy.consumption.report.BaselineReport;
import com.ionutbalosin.jvm.energy.consumption.report.JavaSamplesReport;
import com.ionutbalosin.jvm.energy.consumption.report.OffTheShelfApplicationsReport;
import com.ionutbalosin.jvm.energy.consumption.report.SummaryReport;
import com.ionutbalosin.jvm.energy.consumption.stats.ExecutionType;
import com.ionutbalosin.jvm.energy.consumption.stats.PerfStats;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class EnergyReportCalculator {

  static Function<Double, List<AbstractReport>> REPORTS =
      (meanPowerBaseline) ->
          List.of(
              new OffTheShelfApplicationsReport("spring-petclinic", meanPowerBaseline),
              new OffTheShelfApplicationsReport(
                  "quarkus-hibernate-orm-panache-quickstart", meanPowerBaseline),
              new JavaSamplesReport("java-samples", "ThrowExceptionPatterns", meanPowerBaseline),
              new JavaSamplesReport("java-samples", "MemoryAccessPatterns", meanPowerBaseline),
              new JavaSamplesReport("java-samples", "LoggingPatterns", meanPowerBaseline),
              new JavaSamplesReport("java-samples", "SortingAlgorithms", meanPowerBaseline),
              new JavaSamplesReport("java-samples", "VirtualCalls", meanPowerBaseline));

  public static String BASE_PATH = Paths.get(".").toAbsolutePath().normalize().toString();
  public static String OUTPUT_FOLDER = "energy-consumption";
  public static String REPORT_STATS_OUTPUT_FILE = "energy-reports.csv";
  public static String RAW_PERF_STATS_OUTPUT_FILE = "raw-perf-stats.csv";
  public static String OS = "linux";
  public static String ARCH = "x86_64";
  public static String JDK_VERSION = "17";

  public static void main(String[] args) throws IOException {
    // 1. calculate the baseline mean power from the baseline measurements
    BaselineReport baseline = new BaselineReport("baseline-idle-os");
    calculateEnergy(baseline);

    // 2. for any other report pass the baseline mean power and collect raw perf stats
    Map<ExecutionType, List<PerfStats>> allPerfStats = new HashMap();
    for (AbstractReport report : REPORTS.apply(baseline.meanPowerBaseline)) {
      Map<ExecutionType, List<PerfStats>> result = calculateEnergy(report);

      // collect individual raw perf stats for each execution type (e.g., RUN, BUILD)
      for (ExecutionType executionType : ExecutionType.values()) {
        List<PerfStats> perfStats = allPerfStats.getOrDefault(executionType, new ArrayList<>());
        perfStats.addAll(result.get(executionType));
        allPerfStats.put(executionType, perfStats);
      }
    }

    // 3. for the summary report pass the baseline mean power and the raw perf stats
    SummaryReport summary = new SummaryReport("rapl-reports", baseline.meanPowerBaseline);
    calculateEnergy(summary, allPerfStats);
  }

  private static Map<ExecutionType, List<PerfStats>> calculateEnergy(AbstractReport energyReport)
      throws IOException {
    String outputPath = new File(getPath(energyReport.basePath, OUTPUT_FOLDER)).getCanonicalPath();
    Files.createDirectories(Paths.get(outputPath));
    Map<ExecutionType, List<PerfStats>> result = new HashMap();

    for (ExecutionType executionType : ExecutionType.values()) {
      calculateEnergy(energyReport, outputPath, executionType);
      result.put(executionType, energyReport.perfStats);
    }

    return result;
  }

  private static void calculateEnergy(
      AbstractReport energyReport, Map<ExecutionType, List<PerfStats>> allPerfStats)
      throws IOException {
    String outputPath = new File(getPath(energyReport.basePath, OUTPUT_FOLDER)).getCanonicalPath();
    Files.createDirectories(Paths.get(outputPath));

    for (ExecutionType executionType : ExecutionType.values()) {
      energyReport.perfStats = allPerfStats.get(executionType);
      calculateEnergy(energyReport, outputPath, executionType);
    }
  }

  private static void calculateEnergy(
      AbstractReport energyReport, String outputPath, ExecutionType executionType)
      throws IOException {
    String rawPerfStatsOutputFile = getPath(outputPath, executionType, RAW_PERF_STATS_OUTPUT_FILE);
    energyReport.parseRawPerfStats(executionType);
    energyReport.printRawPerfStatsReport(rawPerfStatsOutputFile);

    String reportStatsOutputFile = getPath(outputPath, executionType, REPORT_STATS_OUTPUT_FILE);
    energyReport.createReportStats();
    energyReport.printReportStats(reportStatsOutputFile);
  }

  private static String getPath(String outputPath, String outputFile) {
    return outputPath + "/" + outputFile;
  }

  private static String getPath(String outputPath, ExecutionType executionType, String outputFile) {
    return outputPath + "/" + executionType.getType() + "-" + outputFile;
  }
}
