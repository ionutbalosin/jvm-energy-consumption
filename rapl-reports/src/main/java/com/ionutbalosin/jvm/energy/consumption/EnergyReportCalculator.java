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

import static java.util.stream.Collectors.toList;

import com.ionutbalosin.jvm.energy.consumption.formula.WattEnergyFormulas;
import com.ionutbalosin.jvm.energy.consumption.formula.WattSecEnergyFormulas;
import com.ionutbalosin.jvm.energy.consumption.perfstats.Parser;
import com.ionutbalosin.jvm.energy.consumption.perfstats.Stats;
import com.ionutbalosin.jvm.energy.consumption.report.AbstractReport;
import com.ionutbalosin.jvm.energy.consumption.report.BaselineReport;
import com.ionutbalosin.jvm.energy.consumption.report.JavaSamplesReport;
import com.ionutbalosin.jvm.energy.consumption.report.OffTheShelfApplicationsReport;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class EnergyReportCalculator {

  public static final String BASE_PATH = Paths.get(".").toAbsolutePath().normalize().toString();
  public static final String OUTPUT_FOLDER = "power-consumption";
  public static final String MEAN_OUTPUT_FILE = "power-reports.csv";
  public static final String RAW_PERF_STATS_OUTPUT_FILE = "raw-perf-stats.csv";
  public static final String OS = "linux";
  public static final String ARCH = "x86_64";
  public static final String JDK_VERSION = "17";

  private static final List<AbstractReport> REPORTS =
      List.of(
          new BaselineReport("baseline-idle-os", new WattEnergyFormulas()),
          new OffTheShelfApplicationsReport(
              "spring-petclinic", "openjdk-hotspot-vm", new WattSecEnergyFormulas()),
          new OffTheShelfApplicationsReport(
              "quarkus-hibernate-orm-panache-quickstart",
              "openjdk-hotspot-vm",
              new WattSecEnergyFormulas()),
          new OffTheShelfApplicationsReport(
              "renaissance", "openjdk-hotspot-vm", new WattSecEnergyFormulas()),
          new JavaSamplesReport(
              "ThrowExceptionPatterns",
              "openjdk-hotspot-vm-override_fist",
              new WattSecEnergyFormulas()),
          new JavaSamplesReport(
              "MemoryAccessPatterns", "openjdk-hotspot-vm-linear", new WattSecEnergyFormulas()),
          new JavaSamplesReport(
              "LoggingPatterns", "openjdk-hotspot-vm-lambda_heap", new WattSecEnergyFormulas()));

  public static void main(String[] args) throws IOException {
    for (AbstractReport report : REPORTS) {
      calculateEnergy(report);
    }
  }

  private static void calculateEnergy(AbstractReport energyReport) throws IOException {
    System.out.printf("Calculate energy for '%s'\n", energyReport.category);

    String perfStatsPath = energyReport.getPerfStatsPath();
    List<Stats> perfStats = readFiles(perfStatsPath);
    energyReport.setPerfStats(perfStats);

    String outputPath = new File(perfStatsPath + "/../" + OUTPUT_FOLDER).getCanonicalPath();
    Files.createDirectories(Paths.get(outputPath));

    String perfStatsOutputFile = outputPath + "/" + RAW_PERF_STATS_OUTPUT_FILE;
    energyReport.createRawPerfStatsReport(perfStatsOutputFile);

    String geometricMeanOutputFile = outputPath + "/" + MEAN_OUTPUT_FILE;
    energyReport.createMeanReport(geometricMeanOutputFile);

    System.out.println();
  }

  private static List<Stats> readFiles(String parentFolder) throws IOException {
    return Files.walk(Paths.get(parentFolder))
        .filter(Files::isRegularFile)
        .map(Parser::parseStats)
        .collect(toList());
  }
}
