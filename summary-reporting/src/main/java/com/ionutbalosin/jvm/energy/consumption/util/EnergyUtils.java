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
package com.ionutbalosin.jvm.energy.consumption.util;

import com.ionutbalosin.jvm.energy.consumption.stats.ExecutionType;
import com.ionutbalosin.jvm.energy.consumption.stats.TestDescriptor;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class EnergyUtils {

  public static String BASE_PATH = Paths.get(".").toAbsolutePath().normalize().toString();

  public static String OS = "linux";
  public static String ARCH = "x86_64";
  public static String JDK_VERSION = "21";

  public static String OUTPUT_FOLDER = "summary-reports";

  public static String RAW_PERFORMANCE_STATS_OUTPUT_FILE = "performance-report-%s.csv";
  public static String PERFORMANCE_STATS_OUTPUT_FILE = "performance-report-%s.csv";

  public static String ENERGY_REPORT_OUTPUT_FILE = "energy-report-%s.csv";
  public static String RAW_POWER_STATS_OUTPUT_FILE = "raw-power-%s.csv";

  // Extract the test category, test type and test run identifier from the output file name
  // Note: this entire logic relies on a very specific file name convention:
  // "<test_category>-<execution_type>-<test_type>-<test_run_identifier>.stats"
  // Example:
  //  - filename: "openjdk-hotspot-vm-run-guarded_parametrized-default.txt"
  //  - category: "openjdk-hotspot-vm"
  //  - execution type: "run"
  //  - type: "guarded_parametrized"
  //  - run identifier: "default"
  public static TestDescriptor parseTestFileName(Path filePath, ExecutionType executionType) {
    String fileName = filePath.getFileName().toString();
    int executionIndex = fileName.indexOf("-" + executionType.getType() + "-");
    int extensionIndex = fileName.lastIndexOf(".");
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

  // Converts a string representing a number with decimals into a double value
  public static double stringToDouble(String value) {
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
