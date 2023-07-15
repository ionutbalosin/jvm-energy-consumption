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
package com.ionutbalosin.jvm.energy.consumption.formulas;

import com.ionutbalosin.jvm.energy.consumption.stats.PerfStats;
import com.ionutbalosin.jvm.energy.consumption.stats.ReportStats;
import java.util.List;
import org.apache.commons.math3.distribution.TDistribution;

public abstract class AbstractFormulas {

  private static double CONFIDENCE = 0.90;

  // this could return one of below formulae:
  //  - the power formula (in Watt) typically used for the baseline measurements
  //  - the energy formula (in Watt⋅sec) typically used for any java-samples and off-the-shelf
  // applications
  //  - the time elapsed formula (in sec) typically used for any java-samples and off-the-shelf
  // applications
  public abstract double getFormula(PerfStats perfStat);

  public abstract double getFormula(ReportStats reportStat);

  public double getGeometricMean(List<ReportStats> reportStats) {
    double prod = 1;
    for (ReportStats reportStat : reportStats) {
      prod *= getFormula(reportStat);
    }
    return Math.pow(prod, 1.0 / reportStats.size());
  }

  public double getMean(List<PerfStats> perfStats) {
    int count = perfStats.size();
    if (count > 0) {
      return getSum(perfStats) / count;
    } else {
      return Double.NaN;
    }
  }

  public double getSum(List<PerfStats> perfStats) {
    int size = perfStats.size();
    if (size > 0) {
      double sum = 0;
      for (PerfStats perfStat : perfStats) {
        sum += getFormula(perfStat);
      }
      return sum;
    } else {
      return Double.NaN;
    }
  }

  public double getMeanError(List<PerfStats> perfStats) {
    int size = perfStats.size();
    if (size <= 2) {
      return Double.NaN;
    }
    TDistribution distribution = new TDistribution(size - 1);
    double probability = distribution.inverseCumulativeProbability(1 - (1 - CONFIDENCE) / 2);
    return probability * getStandardDeviation(perfStats) / Math.sqrt(size);
  }

  public double getStandardDeviation(List<PerfStats> perfStats) {
    return Math.sqrt(getVariance(perfStats));
  }

  public double getVariance(List<PerfStats> perfStats) {
    int size = perfStats.size();
    if (size > 1) {
      double variance = 0;
      double mean = getMean(perfStats);
      for (PerfStats perfStat : perfStats) {
        variance += Math.pow(getFormula(perfStat) - mean, 2);
      }
      return variance / (size - 1);
    } else {
      return Double.NaN;
    }
  }
}
