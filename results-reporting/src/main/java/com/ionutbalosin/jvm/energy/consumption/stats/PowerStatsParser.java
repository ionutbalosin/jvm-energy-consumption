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
package com.ionutbalosin.jvm.energy.consumption.stats;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.function.Predicate.not;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class PowerStatsParser {

  public static PowerStats parsePowerStats(Path filePath, ExecutionType executionType) {
    TestDescriptor descriptor = parsePowerStatsFileName(filePath, executionType);
    List<PowerStats.PowerSample> samples = parseReportPowerStats(filePath);
    PowerStats powerStats = new PowerStats();
    powerStats.descriptor = descriptor;
    powerStats.samples = samples;
    // assume every stat is printed at every 1 second
    powerStats.elapsed = samples.size();
    return powerStats;
  }

  private static List<PowerStats.PowerSample> parseReportPowerStats(Path filePath) {
    final AtomicBoolean isInsideTable = new AtomicBoolean(false);
    final AtomicInteger timeColIndex = new AtomicInteger(-1);
    final AtomicInteger wattsColIndex = new AtomicInteger(-1);
    final AtomicInteger tcpuColIndex = new AtomicInteger(-1);

    try (BufferedReader bufferedReader =
        new BufferedReader(new InputStreamReader(new FileInputStream(filePath.toFile()), UTF_8))) {
      return bufferedReader
          .lines()
          .map(String::trim)
          .filter(not(String::isBlank))
          .map(
              line -> {
                // if table header is encountered, set Time, Watts and TCPU column indexes
                if (line.contains("Time") && line.contains("Watts") && line.contains("TCPU")) {
                  String[] headers = line.replaceAll("\\s+", " ").split(" ");
                  for (int i = 0; i < headers.length; i++) {
                    switch (headers[i]) {
                      case "Time":
                        timeColIndex.set(i);
                        break;
                      case "Watts":
                        wattsColIndex.set(i);
                        break;
                      case "TCPU":
                        tcpuColIndex.set(i);
                        break;
                    }
                  }
                  isInsideTable.set(true);
                }
                // mark the table end
                if (line.contains("-----")) {
                  isInsideTable.set(false);
                }
                return line;
              })
          .filter(lines -> isInsideTable.get())
          .filter(line -> !line.contains("Skipped samples"))
          .skip(1) // skip the table header
          .map(line -> line.replaceAll("\\s+", " ").split(" "))
          .map(
              columns -> {
                PowerStats.PowerSample powerSample = new PowerStats.PowerSample();
                powerSample.date = columns[timeColIndex.get()];
                powerSample.watts = stringToDouble(columns[wattsColIndex.get()]);
                powerSample.tcpu = stringToDouble(columns[tcpuColIndex.get()]);
                return powerSample;
              })
          .collect(Collectors.toList());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  // extract the test category, test type and test run identifier from the output file name
  // Note: this entire logic relies on a very specific file name convention:
  // "<test_category>-<execution_type>-<test_type>-<test_run_identifier>.stats"
  // Example:
  //  - filename: "openjdk-hotspot-vm-run-guarded_parametrized-default.txt"
  //  - category: "openjdk-hotspot-vm"
  //  - execution type: "run"
  //  - type: "guarded_parametrized"
  //  - run identifier: "default"
  private static TestDescriptor parsePowerStatsFileName(
      Path filePath, ExecutionType executionType) {
    String fileName = filePath.getFileName().toString();
    int executionIndex = fileName.indexOf("-" + executionType.getType() + "-");
    int extensionIndex = fileName.indexOf(".txt");
    int lastDashIndex = fileName.lastIndexOf("-");
    int beforeLastDashIndex = fileName.lastIndexOf("-", lastDashIndex - 1);

    TestDescriptor descriptor = new TestDescriptor();
    descriptor.category = fileName.substring(0, executionIndex);
    // add test type only if they exist in the file name format
    if (executionIndex != beforeLastDashIndex) {
      descriptor.type = fileName.substring(beforeLastDashIndex + 1, lastDashIndex);
    }
    descriptor.runIdentifier = fileName.substring(lastDashIndex + 1, extensionIndex);

    return descriptor;
  }

  private static double stringToDouble(String value) {
    try {
      DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
      symbols.setDecimalSeparator('.');
      symbols.setGroupingSeparator(',');
      DecimalFormat format = new DecimalFormat("#,###.##", symbols);
      return format.parse(value).doubleValue();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
