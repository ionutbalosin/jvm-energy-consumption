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

import com.ionutbalosin.jvm.energy.consumption.stats.ExecutionType;
import java.io.IOException;

public interface Report {

  void parseRawStats(ExecutionType executionType) throws IOException;

  void reportRawStats(String outputFilePath) throws IOException;

  void processRawStats();

  String getRawStatsOutputFile();

  void resetProcessedStats();

  void reportProcessedStats(String outputFilePath) throws IOException;

  String getProcessedStatsOutputFile();

  default void processReport(String outputPath, ExecutionType executionType) throws IOException {
    // 1. Extract from initial source, aggregate, and report raw stats
    String rawStatsOutputFile = getPath(outputPath, executionType, getRawStatsOutputFile());
    parseRawStats(executionType);
    reportRawStats(rawStatsOutputFile);

    // 2. Process raw stats and report them
    String processedStatsOutputFile =
        getPath(outputPath, executionType, getProcessedStatsOutputFile());
    processRawStats();
    reportProcessedStats(processedStatsOutputFile);
  }

  private static String getPath(String outputPath, ExecutionType executionType, String outputFile) {
    return outputPath + "/" + String.format(outputFile, executionType.getType());
  }
}
