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
import java.util.List;
import org.apache.commons.math3.distribution.TDistribution;

public abstract class AbstractFormulas {

  double CONFIDENCE = 0.90;

  // this could return one of below consumption formulas (depending on the implementation/caller):
  //  - the power (in Watt) consumption
  //  - the energy (in Watt⋅sec) consumption
  //  - the time elapsed (in sec) consumption
  public abstract double getConsumption(PerfStats perfStat);

  // returns the carbon dioxide (in grams) based on consumed energy
  public abstract double getCarbonDioxide(List<PerfStats> perfStats);

  public double getGeometricMean(List<PerfStats> perfStats) {
    int size = perfStats.size();
    if (size > 0) {
      double prod = 1;
      for (PerfStats perfStat : perfStats) {
        prod *= getConsumption(perfStat);
      }
      return Math.pow(prod, 1.0 / size);
    } else {
      return Double.NaN;
    }
  }

  public double getMean(List<PerfStats> perfStats) {
    int size = perfStats.size();
    if (size > 0) {
      return getSum(perfStats) / size;
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

  public double getSum(List<PerfStats> perfStats) {
    int size = perfStats.size();
    if (size > 0) {
      double sum = 0;
      for (PerfStats perfStat : perfStats) {
        sum += getConsumption(perfStat);
      }
      return sum;
    } else {
      return Double.NaN;
    }
  }

  private double getStandardDeviation(List<PerfStats> perfStats) {
    return Math.sqrt(getVariance(perfStats));
  }

  private double getVariance(List<PerfStats> perfStats) {
    int size = perfStats.size();
    if (size > 1) {
      double variance = 0;
      double mean = getMean(perfStats);
      for (PerfStats perfStat : perfStats) {
        variance += Math.pow(getConsumption(perfStat) - mean, 2);
      }
      return variance / (size - 1);
    } else {
      return Double.NaN;
    }
  }
}
