/**
 * JVM Energy Consumption
 *
 * Copyright (C) 2023-2025 Ionut Balosin
 * Website:      www.ionutbalosin.com
 * Social Media:
 *   LinkedIn:   ionutbalosin
 *   Bluesky:    @ionutbalosin.bsky.social
 *   X:          @ionutbalosin
 *   Mastodon:   ionutbalosin@mastodon.social
 *
 *  MIT License
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 *
 */
package com.ionutbalosin.jvm.energy.consumption.stats.performance;

import static com.ionutbalosin.jvm.energy.consumption.util.EnergyUtils.parseTestFileName;
import static com.ionutbalosin.jvm.energy.consumption.util.EnergyUtils.stringToDouble;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.function.Predicate.not;

import com.ionutbalosin.jvm.energy.consumption.stats.ExecutionType;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PerformanceStatsParser {

  static final Pattern PATTERN = Pattern.compile("\\d+(\\.\\d+)?");

  public static PerformanceStats parsePerformanceStats(Path filePath, ExecutionType executionType) {
    PerformanceStats performanceStats = new PerformanceStats();
    performanceStats.descriptor = parseTestFileName(filePath, executionType);
    parseReportPerformanceStats(filePath).map(value -> performanceStats.value = value);
    return performanceStats;
  }

  private static Optional<Double> parseReportPerformanceStats(Path filePath) {
    try (BufferedReader bufferedReader =
        new BufferedReader(new InputStreamReader(new FileInputStream(filePath.toFile()), UTF_8))) {
      return bufferedReader
          .lines()
          .map(String::trim)
          .filter(not(String::isBlank))
          .filter(line -> line.contains("Runs/sec") || line.contains("Requests/sec"))
          .map(
              line -> {
                Matcher matcher = PATTERN.matcher(line);
                if (matcher.find()) {
                  String valueString = matcher.group();
                  return stringToDouble(valueString);
                } else {
                  return null;
                }
              })
          .filter(Objects::nonNull)
          .findAny();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
