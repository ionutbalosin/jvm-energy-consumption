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
import static com.ionutbalosin.jvm.energy.consumption.util.EnergyUtils.RAW_PERFORMANCE_STATS_OUTPUT_FILE;
import static java.util.stream.Collectors.toList;

import com.ionutbalosin.jvm.energy.consumption.report.Report;
import com.ionutbalosin.jvm.energy.consumption.stats.ExecutionType;
import com.ionutbalosin.jvm.energy.consumption.stats.performance.PerformanceStats;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public abstract class AbstractPerformanceReport implements Report {

  public String basePath;
  public List<PerformanceStats> rawStats;

  public void parseRawStats(ExecutionType executionType) throws IOException {
    this.rawStats = parseRawStats(basePath, executionType);
  }

  @Override
  public String getRawStatsOutputFile() {
    return RAW_PERFORMANCE_STATS_OUTPUT_FILE;
  }

  @Override
  public void resetProcessedStats() {}

  @Override
  public void processRawStats() {}

  @Override
  public void reportProcessedStats(String outputFilePath) {}

  @Override
  public String getProcessedStatsOutputFile() {
    return "n/a";
  }

  private List<PerformanceStats> parseRawStats(String parentFolder, ExecutionType executionType)
      throws IOException {
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