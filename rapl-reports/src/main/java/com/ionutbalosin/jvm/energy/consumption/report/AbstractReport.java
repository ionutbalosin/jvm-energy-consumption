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

import static com.ionutbalosin.jvm.energy.consumption.stats.PerfStatsParser.parseStats;
import static java.util.stream.Collectors.toList;

import com.ionutbalosin.jvm.energy.consumption.stats.ExecutionType;
import com.ionutbalosin.jvm.energy.consumption.stats.PerfStats;
import com.ionutbalosin.jvm.energy.consumption.stats.ReportStats;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractReport {

  public String module;
  public String basePath;
  public List<PerfStats> perfStats = new ArrayList<>();
  public List<ReportStats> reportStats = new ArrayList<>();

  public void parseRawPerfStats(ExecutionType perfType) throws IOException {
    this.perfStats = parseRawPerfStats(basePath + "/perf", perfType);
  }

  public abstract void printRawPerfStatsReport(String outputFilePath) throws IOException;

  public void resetReportStats() {
    this.reportStats.clear();
  }

  public abstract void createReportStats();

  public abstract void printReportStats(String outputFilePath) throws IOException;

  private List<PerfStats> parseRawPerfStats(String parentFolder, ExecutionType executionType)
      throws IOException {
    PathMatcher filenameMatcher = getPathMatcher(executionType);
    return Files.walk(Paths.get(parentFolder))
        .filter(Files::isRegularFile)
        .filter(filenameMatcher::matches)
        .map(filePath -> parseStats(filePath, executionType))
        .map(this::setModule)
        .collect(toList());
  }

  private PerfStats setModule(PerfStats parseStats) {
    parseStats.module = this.module;
    return parseStats;
  }

  private PathMatcher getPathMatcher(ExecutionType executionType) {
    return FileSystems.getDefault()
        .getPathMatcher("regex:.*-" + executionType.getType() + "-.*.stats");
  }
}
