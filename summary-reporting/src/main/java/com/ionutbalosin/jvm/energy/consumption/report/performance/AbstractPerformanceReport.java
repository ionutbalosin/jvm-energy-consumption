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
import static com.ionutbalosin.jvm.energy.consumption.util.EnergyUtils.PERFORMANCE_STATS_OUTPUT_FILE;
import static com.ionutbalosin.jvm.energy.consumption.util.EnergyUtils.RAW_PERFORMANCE_STATS_OUTPUT_FILE;
import static java.util.stream.Collectors.toList;

import com.ionutbalosin.jvm.energy.consumption.report.Report;
import com.ionutbalosin.jvm.energy.consumption.stats.ExecutionType;
import com.ionutbalosin.jvm.energy.consumption.stats.performance.PerformanceStats;
import com.ionutbalosin.jvm.energy.consumption.stats.performance.ReportPerformanceStats;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractPerformanceReport implements Report {

  public String basePath;
  public List<PerformanceStats> rawStats;
  public List<ReportPerformanceStats> processedStats = new ArrayList<>();

  public void parseRawStats(ExecutionType executionType) throws IOException {
    this.rawStats = parseRawStats(basePath, executionType);
  }

  @Override
  public String getRawStatsOutputFile() {
    return RAW_PERFORMANCE_STATS_OUTPUT_FILE;
  }

  public void resetProcessedStats() {
    this.processedStats.clear();
  }

  @Override
  public void processRawStats() {}

  @Override
  public void reportProcessedStats(String outputFilePath) throws IOException {}

  @Override
  public String getProcessedStatsOutputFile() {
    return PERFORMANCE_STATS_OUTPUT_FILE;
  }

  private List<PerformanceStats> parseRawStats(String parentFolder, ExecutionType executionType)
      throws IOException {
    final PathMatcher buildAndRunMatcher = getPathMatcher(executionType.getType());
    final PathMatcher pgoInstrumentMatcher = getPathMatcher("pgo_instrument");

    return Files.walk(Paths.get(parentFolder))
        .filter(Files::isRegularFile)
        .filter(buildAndRunMatcher::matches)
        // Ignore all "pgo_instrument" files, since we do not collect performance stats from them
        .filter(path -> !pgoInstrumentMatcher.matches(path))
        .map(filePath -> parsePerformanceStats(filePath, executionType))
        .collect(toList());
  }

  private PathMatcher getPathMatcher(String pattern) {
    return FileSystems.getDefault().getPathMatcher("regex:.*-" + pattern + ".*\\.(txt|log)");
  }
}
