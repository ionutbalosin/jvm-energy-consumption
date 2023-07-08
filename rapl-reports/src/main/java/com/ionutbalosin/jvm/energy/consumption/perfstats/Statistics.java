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
package com.ionutbalosin.jvm.energy.consumption.perfstats;

import java.util.List;
import org.apache.commons.math3.distribution.TDistribution;

public class Statistics {

  private static double CONFIDENCE = 0.90;

  public static double getEnergy(Stats perfStat) {
    // pkg includes the cores and gpu
    // Note: on laptop battery the psys counters does not display proper stats
    return perfStat.pkg + perfStat.ram;
  }

  public static double getGeometricMean(List<Stats> perfStats) {
    double prod = 1;
    for (Stats perfStat : perfStats) {
      double energy = getEnergy(perfStat);
      prod *= energy;
    }
    return Math.pow(prod, 1.0 / perfStats.size());
  }

  public static double getMean(List<Stats> perfStats) {
    int count = perfStats.size();
    if (count > 0) {
      return getSum(perfStats) / count;
    } else {
      return Double.NaN;
    }
  }

  public static double getSum(List<Stats> perfStats) {
    int size = perfStats.size();
    if (size > 0) {
      double sum = 0;
      for (Stats perfStat : perfStats) {
        sum += getEnergy(perfStat);
      }
      return sum;
    } else {
      return Double.NaN;
    }
  }

  public static double getMeanError(List<Stats> perfStats) {
    int size = perfStats.size();
    if (size <= 2) {
      return Double.NaN;
    }
    TDistribution distribution = new TDistribution(size - 1);
    double probability = distribution.inverseCumulativeProbability(1 - (1 - CONFIDENCE) / 2);
    return probability * getStandardDeviation(perfStats) / Math.sqrt(size);
  }

  public static double getStandardDeviation(List<Stats> perfStats) {
    return Math.sqrt(getVariance(perfStats));
  }

  public static double getVariance(List<Stats> perfStats) {
    int size = perfStats.size();
    if (size > 1) {
      double variance = 0;
      double mean = getMean(perfStats);
      for (Stats perfStat : perfStats) {
        variance += Math.pow(getEnergy(perfStat) - mean, 2);
      }
      return variance / (size - 1);
    } else {
      return Double.NaN;
    }
  }
}
