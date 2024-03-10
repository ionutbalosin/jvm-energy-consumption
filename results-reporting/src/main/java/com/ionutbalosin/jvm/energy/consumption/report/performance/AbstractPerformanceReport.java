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

import static com.ionutbalosin.jvm.energy.consumption.stats.performance.PerformanceStatsParser.parsePerformanceStats;
import static java.nio.file.Files.newBufferedWriter;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

import com.ionutbalosin.jvm.energy.consumption.stats.ExecutionType;
import com.ionutbalosin.jvm.energy.consumption.stats.performance.PerformanceStats;
import com.ionutbalosin.jvm.energy.consumption.stats.performance.ReportPerformanceStats;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public abstract class AbstractPerformanceReport {

  public String basePath;
  public List<PerformanceStats> performanceStats;
  public List<ReportPerformanceStats> reportPerformanceStats = new ArrayList<>();

  public void parseRawPerformanceStats(ExecutionType executionType) throws IOException {
    this.performanceStats = parseRawPerformanceStats(basePath, executionType);
  }

  public void reportRawPerformanceStats(String outputFilePath) throws IOException {
    if (performanceStats.isEmpty()) {
      return;
    }

    try (PrintWriter writer = new PrintWriter(newBufferedWriter(Paths.get(outputFilePath)))) {
      writer.printf(
          "%18s;%26s;%16s;%22s\n", "Category", "Type", "Run Identifier", "Throughput (Ops/sec)");

      for (PerformanceStats performanceStat : performanceStats) {
        writer.printf(
            "%18s;%26s;%16s;%22.3f\n",
            performanceStat.descriptor.category,
            ofNullable(performanceStat.descriptor.type).orElse("N/A"),
            performanceStat.descriptor.runIdentifier,
            performanceStat.value);
      }
    }

    System.out.printf("Raw performance stats report %s was successfully created\n", outputFilePath);
  }

  public abstract void reportPerformanceStats(String outputFilePath) throws IOException;

  public void resetReportPerformanceStats() {
    this.reportPerformanceStats.clear();
  }

  public abstract void createReportStats();

  private List<PerformanceStats> parseRawPerformanceStats(
      String parentFolder, ExecutionType executionType) throws IOException {
    PathMatcher filenameMatcher = getPathMatcher(executionType);
    return Files.walk(Paths.get(parentFolder))
        .filter(Files::isRegularFile)
        .filter(filenameMatcher::matches)
        .map(filePath -> parsePerformanceStats(filePath, executionType))
        .filter(Predicate.not(Optional::isEmpty))
        .map(Optional::get)
        .collect(toList());
  }

  private PathMatcher getPathMatcher(ExecutionType executionType) {
    return FileSystems.getDefault()
        .getPathMatcher("regex:.*-" + executionType.getType() + "-.*\\.(txt|log)");
  }
}
