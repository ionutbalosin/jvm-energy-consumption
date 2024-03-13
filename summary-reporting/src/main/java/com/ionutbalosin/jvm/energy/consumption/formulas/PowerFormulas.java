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
package com.ionutbalosin.jvm.energy.consumption.formulas;

import com.ionutbalosin.jvm.energy.consumption.stats.power.PowerStats;
import java.util.List;

public class PowerFormulas {

  // Source: https://app.electricitymaps.com/zone/AT
  // TODO: this value changes frequently, it should be checked before every run
  public static double CARBON_DIOXIDE_EMISSION_FACTOR = 195; // Carbon Intensity (gCO₂eq/kWh)

  // Calculates the total energy consumption (in watt-seconds) after subtracting a baseline from
  // every result. Since each power measurement (in watts) corresponds to a duration of 1 second,
  // summing up all the power measurements results in the total consumed energy.
  public double getEnergy(PowerStats powerStats, double baseline) {
    double watts = 0;
    for (PowerStats.PowerSample powerSample : powerStats.samples) {
      watts += powerSample.watts - baseline;
    }
    return watts;
  }

  // returns the total energy consumption (in Watt⋅sec)
  public double getEnergy(List<PowerStats> powerStats) {
    int size = powerStats.size();
    if (size > 0) {
      double sum = 0;
      for (PowerStats powerStat : powerStats) {
        sum += powerStat.energy;
      }
      return sum;
    } else {
      return Double.NaN;
    }
  }

  // returns the carbon dioxide (in grams) based on total energy
  public double getCarbonDioxide(double totalEnergy) {
    double energyInKWh = totalEnergy / 3_600_000;
    return energyInKWh * CARBON_DIOXIDE_EMISSION_FACTOR;
  }

  public double getGeometricMean(PowerStats powerStats) {
    int size = powerStats.samples.size();
    if (size > 0) {
      double prod = getEnergy(powerStats, 0);
      return Math.pow(prod, 1.0 / size);
    } else {
      return Double.NaN;
    }
  }

  public double getElapsed(List<PowerStats> powerStats) {
    int size = powerStats.size();
    if (size > 0) {
      double sum = 0;
      for (PowerStats powerStat : powerStats) {
        sum += powerStat.elapsed;
      }
      return sum;
    } else {
      return Double.NaN;
    }
  }
}
