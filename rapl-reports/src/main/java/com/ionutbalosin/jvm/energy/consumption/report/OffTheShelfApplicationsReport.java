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

import static com.ionutbalosin.jvm.energy.consumption.rapl.report.EnergyReportCalculator.ARCH;
import static com.ionutbalosin.jvm.energy.consumption.rapl.report.EnergyReportCalculator.BASE_PATH;
import static com.ionutbalosin.jvm.energy.consumption.rapl.report.EnergyReportCalculator.JDK_VERSION;
import static com.ionutbalosin.jvm.energy.consumption.rapl.report.EnergyReportCalculator.OS;

import com.ionutbalosin.jvm.energy.consumption.Application;

public class OffTheShelfApplicationsReport extends BaselineReport {
  public OffTheShelfApplicationsReport(Application application) {
    super(application);
  }

  @Override
  public String getPerfStatsPath() {
    // Note: this is a specific path format for this application type.
    return String.format(
        "%s/%s/results/%s/%s/jdk-%s/perf", BASE_PATH, application.name, OS, ARCH, JDK_VERSION);
  }
}
