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
package com.ionutbalosin.jvm.energy.consumption;

import static com.ionutbalosin.jvm.energy.consumption.util.EnergyUtils.OUTPUT_FOLDER;

import com.ionutbalosin.jvm.energy.consumption.report.power.AbstractPowerReport;
import com.ionutbalosin.jvm.energy.consumption.report.power.BaselinePowerReport;
import com.ionutbalosin.jvm.energy.consumption.report.power.JavaSamplesPowerReport;
import com.ionutbalosin.jvm.energy.consumption.report.power.OffTheShelfApplicationsPowerReport;
import com.ionutbalosin.jvm.energy.consumption.report.power.SummaryPowerReport;
import com.ionutbalosin.jvm.energy.consumption.stats.ExecutionType;
import com.ionutbalosin.jvm.energy.consumption.stats.power.PowerStats;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class EnergyReportGenerator {

  static Function<Double, List<AbstractPowerReport>> REPORTS =
      (baselinePower) ->
          List.of(
              new OffTheShelfApplicationsPowerReport("spring-petclinic", baselinePower),
              new OffTheShelfApplicationsPowerReport(
                  "quarkus-hibernate-orm-panache-quickstart", baselinePower),
              new JavaSamplesPowerReport("java-samples", "LoggingPatterns", baselinePower),
              new JavaSamplesPowerReport("java-samples", "MemoryAccessPatterns", baselinePower),
              new JavaSamplesPowerReport("java-samples", "SortingAlgorithms", baselinePower),
              new JavaSamplesPowerReport(
                  "java-samples", "StringConcatenationPatterns", baselinePower),
              new JavaSamplesPowerReport("java-samples", "ThrowExceptionPatterns", baselinePower),
              new JavaSamplesPowerReport("java-samples", "VirtualCalls", baselinePower),
              new JavaSamplesPowerReport("java-samples", "VPThreadQueueThroughput", baselinePower));

  public static void main(String[] args) throws IOException {
    // 1. calculate the baseline mean power from the baseline measurements
    BaselinePowerReport baseline = new BaselinePowerReport("baseline-idle-os");
    calculateEnergy(baseline);

    // 2. for any other report pass the baseline mean power and collect raw power stats
    Map<ExecutionType, List<PowerStats>> allPerfStats = new HashMap();
    for (AbstractPowerReport report : REPORTS.apply(baseline.baselinePower)) {
      Map<ExecutionType, List<PowerStats>> result = calculateEnergy(report);

      // collect individual raw power stats for each execution type
      for (ExecutionType executionType : getExecutionTypes()) {
        List<PowerStats> powerStats = allPerfStats.getOrDefault(executionType, new ArrayList<>());
        powerStats.addAll(result.get(executionType));
        allPerfStats.put(executionType, powerStats);
      }
    }

    // 3. for the summary report pass the baseline mean power and the raw power stats
    SummaryPowerReport summary =
        new SummaryPowerReport("summary-reporting", baseline.baselinePower);
    calculateEnergy(summary, allPerfStats);
  }

  private static Map<ExecutionType, List<PowerStats>> calculateEnergy(AbstractPowerReport report)
      throws IOException {
    String outputPath = new File(getPath(report.basePath, OUTPUT_FOLDER)).getCanonicalPath();
    Files.createDirectories(Paths.get(outputPath));
    Map<ExecutionType, List<PowerStats>> result = new HashMap();

    for (ExecutionType executionType : getExecutionTypes()) {
      report.process(outputPath, executionType);
      result.put(executionType, report.rawStats);
    }

    return result;
  }

  private static void calculateEnergy(
      AbstractPowerReport report, Map<ExecutionType, List<PowerStats>> allPowerStats)
      throws IOException {
    String outputPath = new File(getPath(report.basePath, OUTPUT_FOLDER)).getCanonicalPath();
    Files.createDirectories(Paths.get(outputPath));

    for (ExecutionType executionType : getExecutionTypes()) {
      report.rawStats = allPowerStats.get(executionType);
      report.process(outputPath, executionType);
    }
  }

  private static String getPath(String outputPath, String outputFile) {
    return outputPath + "/" + outputFile;
  }

  private static List<ExecutionType> getExecutionTypes() {
    return List.of(ExecutionType.values());
  }
}
