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

public class EnergyFormulas extends AbstractFormulas {

  // Source: https://app.electricitymaps.com/zone/AT
  // TODO: this value changes frequently, it should be checked before every run
  public static double CARBON_DIOXIDE_EMISSION_FACTOR = 137;

  // the baseline represents the measurement of the machine power consumption while it is idle or
  // running minimal background processes.
  // Since it is "Watt", it must be converted to "Watt⋅sec" and subtracted from every measurement
  double meanPowerBaseline;

  public EnergyFormulas(double meanPowerBaseline) {
    this.meanPowerBaseline = meanPowerBaseline;
  }

  // returns the energy consumption (in Watt⋅sec) after subtracting the baseline
  @Override
  public double getConsumption(PerfStats perfStat) {
    // pkg includes the cores and gpu
    // Note: on laptop battery the psys counters does not display proper stats
    return (perfStat.pkg + perfStat.ram) - (meanPowerBaseline * perfStat.elapsed);
  }

  // returns the carbon dioxide (in grams) based on consumed energy
  public double getCarbonDioxide(List<PerfStats> perfStats) {
    double energyInWattSec = getSum(perfStats);
    double energyInKWh = energyInWattSec / 3_600_000;
    return energyInKWh * CARBON_DIOXIDE_EMISSION_FACTOR;
  }
}
