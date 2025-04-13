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
package com.ionutbalosin.jvm.energy.consumption.report.power;

import static com.ionutbalosin.jvm.energy.consumption.stats.ExecutionType.BUILD_TIME;
import static com.ionutbalosin.jvm.energy.consumption.stats.power.PowerStatsParser.parsePowerStats;
import static com.ionutbalosin.jvm.energy.consumption.util.EnergyUtils.ENERGY_REPORT_OUTPUT_FILE;
import static com.ionutbalosin.jvm.energy.consumption.util.EnergyUtils.RAW_POWER_STATS_OUTPUT_FILE;
import static java.nio.file.Files.exists;
import static java.util.stream.Collectors.toList;

import com.ionutbalosin.jvm.energy.consumption.report.Report;
import com.ionutbalosin.jvm.energy.consumption.stats.ExecutionType;
import com.ionutbalosin.jvm.energy.consumption.stats.power.PowerStats;
import com.ionutbalosin.jvm.energy.consumption.stats.power.ReportPowerStats;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractPowerReport implements Report {

  public String basePath;
  public List<PowerStats> rawStats;
  public List<ReportPowerStats> processedStats = new ArrayList<>();

  @Override
  public void parseRawStats(ExecutionType executionType) throws IOException {
    this.rawStats = parseRawStats(basePath + "/power", executionType);
  }

  @Override
  public String getRawStatsOutputFile() {
    return RAW_POWER_STATS_OUTPUT_FILE;
  }

  public void resetProcessedStats() {
    this.processedStats.clear();
  }

  @Override
  public String getProcessedStatsOutputFile() {
    return ENERGY_REPORT_OUTPUT_FILE;
  }

  private List<PowerStats> parseRawStats(String parentFolder, ExecutionType executionType)
      throws IOException {
    final PathMatcher buildAndRunMatcher = getPathMatcher(executionType.getType());
    final PathMatcher pgoMatcher = getPathMatcher("pgo");
    final PathMatcher pgoInstrumentMatcher = getPathMatcher("pgo_instrument");

    return Files.walk(Paths.get(parentFolder))
        .filter(Files::isRegularFile)
        .filter(path -> buildAndRunMatcher.matches(path))
        // Ignore all 'pgo_instrument' files, since they are handled separately,
        // together with 'pgo' (for type 'build')
        .filter(path -> !pgoInstrumentMatcher.matches(path))
        .map(
            filePath -> {
              PowerStats powerStats = parsePowerStats(filePath, executionType);

              // Only in the case of the 'build' type, the 'pgo' and 'pgo_instrument' files are
              // handled together because 'pgo_instrument' represents the initial phase of 'pgo'.
              // Therefore, they need to be aggregated in the final report to calculate the total
              // build energy consumption. Note: The 'run' type of the 'pgo_instrument' file should
              // be ignored since it was only used (as a temporary run) to create the final 'pgo'
              // file.
              if (executionType == BUILD_TIME && pgoMatcher.matches(filePath)) {
                String pgoFilename = filePath.getFileName().toString();
                Path directory = filePath.getParent();

                // 1. Create the analogous to "pgo" the "pgo_instrument" file
                String pgoInstrumentFilename = pgoFilename.replaceAll("pgo", "pgo_instrument");
                Path pgoInstrumentFilePath = directory.resolve(pgoInstrumentFilename);

                // 2. If the "pgo_instrument" file exists, parse it and add their stats to the "pgo"
                if (exists(pgoInstrumentFilePath)) {
                  PowerStats pgoInstrumentPowerStats =
                      parsePowerStats(pgoInstrumentFilePath, executionType);
                  powerStats.samples.addAll(pgoInstrumentPowerStats.samples);
                }

                return powerStats;
              }

              return powerStats;
            })
        .collect(toList());
  }

  private PathMatcher getPathMatcher(String pattern) {
    return FileSystems.getDefault().getPathMatcher("regex:.*-" + pattern + ".*\\.txt");
  }
}
