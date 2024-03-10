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
package com.ionutbalosin.jvm.energy.consumption;

import com.ionutbalosin.jvm.energy.consumption.report.AbstractPowerReport;
import com.ionutbalosin.jvm.energy.consumption.report.BaselinePowerReport;
import com.ionutbalosin.jvm.energy.consumption.report.JavaSamplesPowerReport;
import com.ionutbalosin.jvm.energy.consumption.report.OffTheShelfApplicationsPowerReport;
import com.ionutbalosin.jvm.energy.consumption.report.SummaryPowerReport;
import com.ionutbalosin.jvm.energy.consumption.stats.ExecutionType;
import com.ionutbalosin.jvm.energy.consumption.stats.PowerStats;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class PowerReportCalculator {

  static Function<Double, List<AbstractPowerReport>> REPORTS =
      (baselinePower) ->
          List.of(
              new OffTheShelfApplicationsPowerReport("spring-petclinic", baselinePower),
              new OffTheShelfApplicationsPowerReport(
                  "quarkus-hibernate-orm-panache-quickstart", baselinePower),
              new JavaSamplesPowerReport("java-samples", "LoggingPatterns", baselinePower),
              new JavaSamplesPowerReport("java-samples", "MemoryAccessPatterns", baselinePower),
              new JavaSamplesPowerReport("java-samples", "SortingAlgorithms", baselinePower),
              new JavaSamplesPowerReport(
                  "java-samples", "StringConcatenationPatterns", baselinePower),
              new JavaSamplesPowerReport("java-samples", "ThrowExceptionPatterns", baselinePower),
              new JavaSamplesPowerReport("java-samples", "VirtualCalls", baselinePower),
              new JavaSamplesPowerReport("java-samples", "VPThreadQueueThroughput", baselinePower));

  public static String BASE_PATH = Paths.get(".").toAbsolutePath().normalize().toString();
  public static String OUTPUT_FOLDER = "energy";
  public static String ENERGY_REPORT_OUTPUT_FILE = "%s-report.csv";
  public static String RAW_POWER_STATS_OUTPUT_FILE = "%s-raw-power.csv";
  public static String OS = "linux";
  public static String ARCH = "x86_64";
  public static String JDK_VERSION = "21";

  public static void main(String[] args) throws IOException {
    // 1. calculate the baseline mean power from the baseline measurements
    BaselinePowerReport baseline = new BaselinePowerReport("baseline-idle-os");
    calculateEnergy(baseline);

    // 2. for any other report pass the baseline mean power and collect raw power stats
    Map<ExecutionType, List<PowerStats>> allPerfStats = new HashMap();
    for (AbstractPowerReport report : REPORTS.apply(baseline.baselinePower)) {
      Map<ExecutionType, List<PowerStats>> result = calculateEnergy(report);

      // collect individual raw power stats for each execution type (e.g., RUN, BUILD)
      for (ExecutionType executionType : ExecutionType.values()) {
        List<PowerStats> powerStats = allPerfStats.getOrDefault(executionType, new ArrayList<>());
        powerStats.addAll(result.get(executionType));
        allPerfStats.put(executionType, powerStats);
      }
    }

    // 3. for the summary report pass the baseline mean power and the raw power stats
    SummaryPowerReport summary =
        new SummaryPowerReport("results-reporting", baseline.baselinePower);
    calculateEnergy(summary, allPerfStats);
  }

  private static Map<ExecutionType, List<PowerStats>> calculateEnergy(
      AbstractPowerReport energyReport) throws IOException {
    String outputPath = new File(getPath(energyReport.basePath, OUTPUT_FOLDER)).getCanonicalPath();
    Files.createDirectories(Paths.get(outputPath));
    Map<ExecutionType, List<PowerStats>> result = new HashMap();

    for (ExecutionType executionType : ExecutionType.values()) {
      calculateEnergy(energyReport, outputPath, executionType);
      result.put(executionType, energyReport.powerStats);
    }

    return result;
  }

  private static void calculateEnergy(
      AbstractPowerReport energyReport, Map<ExecutionType, List<PowerStats>> allPerfStats)
      throws IOException {
    String outputPath = new File(getPath(energyReport.basePath, OUTPUT_FOLDER)).getCanonicalPath();
    Files.createDirectories(Paths.get(outputPath));

    for (ExecutionType executionType : ExecutionType.values()) {
      energyReport.powerStats = allPerfStats.get(executionType);
      calculateEnergy(energyReport, outputPath, executionType);
    }
  }

  private static void calculateEnergy(
      AbstractPowerReport energyReport, String outputPath, ExecutionType executionType)
      throws IOException {
    String rawPerfStatsOutputFile = getPath(outputPath, executionType, RAW_POWER_STATS_OUTPUT_FILE);
    energyReport.parseRawPowerStats(executionType);
    energyReport.reportRawPowerStats(rawPerfStatsOutputFile);

    String reportStatsOutputFile = getPath(outputPath, executionType, ENERGY_REPORT_OUTPUT_FILE);
    energyReport.createReportStats();
    energyReport.printReportStats(reportStatsOutputFile);
  }

  private static String getPath(String outputPath, String outputFile) {
    return outputPath + "/" + outputFile;
  }

  private static String getPath(String outputPath, ExecutionType executionType, String outputFile) {
    return outputPath + "/" + String.format(outputFile, executionType.getType());
  }
}
