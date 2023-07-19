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
package com.ionutbalosin.jvm.energy.consumption.stats;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.function.Predicate.not;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class PerfStatsParser {

  public static PerfStats parseStats(Path filePath, ExecutionType executionType) {
    try (BufferedReader bufferedReader =
        new BufferedReader(new InputStreamReader(new FileInputStream(filePath.toFile()), UTF_8))) {

      PerfStats perfStats = new PerfStats();
      bufferedReader
          .lines()
          .map(String::trim)
          .filter(not(String::isBlank))
          .map(line -> line.split(" "))
          .filter(lines -> lines.length > 2)
          .forEach(
              words -> {
                switch (words[2]) {
                  case "power/energy-cores/":
                    perfStats.cores = stringToDouble(words[0]);
                    break;
                  case "power/energy-gpu/":
                    perfStats.gpu = stringToDouble(words[0]);
                    break;
                  case "power/energy-pkg/":
                    perfStats.pkg = stringToDouble(words[0]);
                    break;
                  case "power/energy-psys/":
                    perfStats.psys = stringToDouble(words[0]);
                    break;
                  case "power/energy-ram/":
                    perfStats.ram = stringToDouble(words[0]);
                    break;
                  case "time":
                    perfStats.elapsed = stringToDouble(words[0]);
                    break;
                }
              });

      // extract the test category, test type and test run identifier from the output file name
      // Note: this entire logic relies on a very specific file name convention:
      // "<test_category>-<execution_type>-<test_type>-<test_run_identifier>.stats"
      // Example:
      //  -filename: "openjdk-hotspot-vm-run-guarded_parametrized-1.stats"
      //  - test category: "openjdk-hotspot-vm"
      //  - execution type: "run"
      //  - test type: "guarded_parametrized"
      //  - test run identifier: "1"
      String fileName = filePath.getFileName().toString();
      int runIndex = fileName.indexOf("-" + executionType.toString().toLowerCase() + "-");
      int statsIndex = fileName.indexOf(".stats");
      int lastDashIndex = fileName.lastIndexOf("-");
      int beforeLastDashIndex = fileName.lastIndexOf("-", lastDashIndex - 1);
      perfStats.testCategory = fileName.substring(0, runIndex);
      // add test type only if they exist in the file name format
      if (runIndex != beforeLastDashIndex) {
        perfStats.testType = fileName.substring(beforeLastDashIndex + 1, lastDashIndex);
      }
      perfStats.testRunIdentifier = fileName.substring(lastDashIndex + 1, statsIndex);

      return perfStats;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static double stringToDouble(String statValue) {
    try {
      DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
      symbols.setDecimalSeparator(',');
      symbols.setGroupingSeparator('.');
      DecimalFormat format = new DecimalFormat("#,###.##", symbols);
      return format.parse(statValue).doubleValue();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
