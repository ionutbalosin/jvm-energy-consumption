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

import static com.ionutbalosin.jvm.energy.consumption.util.EnergyUtils.OUTPUT_FOLDER;

import com.ionutbalosin.jvm.energy.consumption.report.performance.AbstractPerformanceReport;
import com.ionutbalosin.jvm.energy.consumption.report.performance.JavaSamplesPerformanceReport;
import com.ionutbalosin.jvm.energy.consumption.report.performance.OffTheShelfApplicationsPerformanceReport;
import com.ionutbalosin.jvm.energy.consumption.report.performance.SummaryPerformanceReport;
import com.ionutbalosin.jvm.energy.consumption.stats.ExecutionType;
import com.ionutbalosin.jvm.energy.consumption.stats.performance.PerformanceStats;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PerformanceReportGenerator {

  static List<AbstractPerformanceReport> REPORTS =
      List.of(
          new OffTheShelfApplicationsPerformanceReport("spring-petclinic"),
          new OffTheShelfApplicationsPerformanceReport("quarkus-hibernate-orm-panache-quickstart"),
          new JavaSamplesPerformanceReport("java-samples", "LoggingPatterns"),
          new JavaSamplesPerformanceReport("java-samples", "MemoryAccessPatterns"),
          new JavaSamplesPerformanceReport("java-samples", "SortingAlgorithms"),
          new JavaSamplesPerformanceReport("java-samples", "StringConcatenationPatterns"),
          new JavaSamplesPerformanceReport("java-samples", "ThrowExceptionPatterns"),
          new JavaSamplesPerformanceReport("java-samples", "VirtualCalls"),
          new JavaSamplesPerformanceReport("java-samples", "VPThreadQueueThroughput"));

  public static void main(String[] args) throws IOException {
    Map<ExecutionType, List<PerformanceStats>> allPerfStats = new HashMap();
    // 1. collect raw performance stats
    for (AbstractPerformanceReport report : REPORTS) {
      Map<ExecutionType, List<PerformanceStats>> result = calculatePerformance(report);

      // collect individual raw performance stats for each execution type
      for (ExecutionType executionType : getExecutionTypes()) {
        List<PerformanceStats> performanceStats =
            allPerfStats.getOrDefault(executionType, new ArrayList<>());
        performanceStats.addAll(result.get(executionType));
        allPerfStats.put(executionType, performanceStats);
      }
    }

    // 2. the summary report
    SummaryPerformanceReport summary = new SummaryPerformanceReport("summary-reporting");
    calculatePerformance(summary, allPerfStats);
  }

  private static Map<ExecutionType, List<PerformanceStats>> calculatePerformance(
      AbstractPerformanceReport report) throws IOException {
    String outputPath = new File(getPath1(report.basePath, OUTPUT_FOLDER)).getCanonicalPath();
    Files.createDirectories(Paths.get(outputPath));
    Map<ExecutionType, List<PerformanceStats>> result = new HashMap();

    for (ExecutionType executionType : getExecutionTypes()) {
      report.process(outputPath, executionType);
      result.put(executionType, report.rawStats);
    }

    return result;
  }

  private static void calculatePerformance(
      AbstractPerformanceReport report,
      Map<ExecutionType, List<PerformanceStats>> allPerformanceStats)
      throws IOException {
    String outputPath = new File(getPath2(report.basePath, OUTPUT_FOLDER)).getCanonicalPath();
    Files.createDirectories(Paths.get(outputPath));

    for (ExecutionType executionType : getExecutionTypes()) {
      report.rawStats = allPerformanceStats.get(executionType);
      report.process(outputPath, executionType);
    }
  }

  private static String getPath1(String outputPath, String outputFile) {
    return outputPath + "/../" + outputFile;
  }

  private static String getPath2(String outputPath, String outputFile) {
    return outputPath + "/" + outputFile;
  }

  private static List<ExecutionType> getExecutionTypes() {
    return List.of(ExecutionType.RUNTIME);
  }
}
