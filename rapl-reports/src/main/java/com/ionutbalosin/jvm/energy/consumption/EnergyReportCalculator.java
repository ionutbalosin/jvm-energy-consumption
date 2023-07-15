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
package com.ionutbalosin.jvm.energy.consumption.rapl.report;

import com.ionutbalosin.jvm.energy.consumption.report.AbstractReport;
import com.ionutbalosin.jvm.energy.consumption.report.BaselineReport;
import com.ionutbalosin.jvm.energy.consumption.report.JavaSamplesReport;
import com.ionutbalosin.jvm.energy.consumption.report.OffTheShelfApplicationsReport;
import com.ionutbalosin.jvm.energy.consumption.report.SummaryReport;
import com.ionutbalosin.jvm.energy.consumption.stats.ReportStats;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class EnergyReportCalculator {

  public static final String BASE_PATH = Paths.get(".").toAbsolutePath().normalize().toString();
  public static final String OUTPUT_FOLDER = "energy-consumption";
  public static final String REPORT_STATS_OUTPUT_FILE = "energy-reports.csv";
  public static final String RAW_PERF_STATS_OUTPUT_FILE = "raw-perf-stats.csv";
  public static final String SUMMARY_REPORT_OUTPUT_FILE = "summary-report.csv";
  public static final String OS = "linux";
  public static final String ARCH = "x86_64";
  public static final String JDK_VERSION = "17";

  public static void main(String[] args) throws IOException {
    // 1. calculate the baseline mean power from the baseline measurements
    BaselineReport baseline = new BaselineReport("baseline-idle-os");
    calculateEnergy(baseline);

    // 2. for any other report pass the baseline mean power and save the generated report stats
    final List<AbstractReport> REPORTS =
        List.of(
            new OffTheShelfApplicationsReport("spring-petclinic", baseline.meanPower),
            new OffTheShelfApplicationsReport(
                "quarkus-hibernate-orm-panache-quickstart", baseline.meanPower),
            new OffTheShelfApplicationsReport("renaissance", "concurrency", baseline.meanPower),
            new OffTheShelfApplicationsReport("renaissance", "functional", baseline.meanPower),
            new OffTheShelfApplicationsReport("renaissance", "scala", baseline.meanPower),
            new OffTheShelfApplicationsReport("renaissance", "web", baseline.meanPower),
            new JavaSamplesReport("java-samples", "ThrowExceptionPatterns", baseline.meanPower),
            new JavaSamplesReport("java-samples", "MemoryAccessPatterns", baseline.meanPower),
            new JavaSamplesReport("java-samples", "LoggingPatterns", baseline.meanPower),
            new JavaSamplesReport("java-samples", "SortingAlgorithms", baseline.meanPower),
            new JavaSamplesReport("java-samples", "VirtualCalls", baseline.meanPower));
    List<ReportStats> reportStats = new ArrayList<>();
    for (AbstractReport report : REPORTS) {
      calculateEnergy(report);
      reportStats.addAll(report.reportStats);
    }

    // 3. the summary report takes into account all previously generated report stats
    SummaryReport summary = new SummaryReport("rapl-reports", reportStats);
    calculateEnergySummary(summary);
  }

  private static void calculateEnergy(AbstractReport energyReport) throws IOException {
    String outputPath = new File(energyReport.basePath + "/" + OUTPUT_FOLDER).getCanonicalPath();
    Files.createDirectories(Paths.get(outputPath));

    String rawPerfStatsOutputFile = outputPath + "/" + RAW_PERF_STATS_OUTPUT_FILE;
    energyReport.parseRawPerfStats();
    energyReport.printRawPerfStatsReport(rawPerfStatsOutputFile);

    String reportStatsOutputFile = outputPath + "/" + REPORT_STATS_OUTPUT_FILE;
    energyReport.createReportStats();
    energyReport.printReportStats(reportStatsOutputFile);
  }

  private static void calculateEnergySummary(AbstractReport energyReport) throws IOException {
    String outputPath = new File(energyReport.basePath + "/" + OUTPUT_FOLDER).getCanonicalPath();
    Files.createDirectories(Paths.get(outputPath));

    String summaryReportOutputFile = outputPath + "/" + SUMMARY_REPORT_OUTPUT_FILE;
    energyReport.createReportStats();
    energyReport.printReportStats(summaryReportOutputFile);
  }
}
