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
package com.ionutbalosin.jvm.energy.consumption.stats.power;

import com.ionutbalosin.jvm.energy.consumption.stats.TestDescriptor;

public class ReportPowerStats {

  // Power stats identifier (running JVM, benchmark name, and run identifier)
  public TestDescriptor descriptor = new TestDescriptor();

  // // Number of test samples
  public int samples;

  // Total energy consumed
  public double energy;

  // Geometric mean of energy consumption
  public double geoMeanEnergy;

  // Carbon dioxide emitted to consume the energy
  public double carbonDioxide;

  public ReportPowerStats(String category, int samples, double geoMeanEnergy) {
    this.descriptor.category = category;
    this.samples = samples;
    this.geoMeanEnergy = geoMeanEnergy;
  }

  public ReportPowerStats(
      String category, String type, String runIdentifier, int samples, double energy) {
    this.descriptor.category = category;
    this.descriptor.type = type;
    this.descriptor.runIdentifier = runIdentifier;
    this.samples = samples;
    this.energy = energy;
  }

  public ReportPowerStats(String category, String runIdentifier, int samples, double energy) {
    this.descriptor.category = category;
    this.descriptor.runIdentifier = runIdentifier;
    this.samples = samples;
    this.energy = energy;
  }

  public ReportPowerStats(
      String category, String runIdentifier, int samples, double energy, double carbonDioxide) {
    this.descriptor.category = category;
    this.descriptor.runIdentifier = runIdentifier;
    this.samples = samples;
    this.energy = energy;
    this.carbonDioxide = carbonDioxide;
  }
}
