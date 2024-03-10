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
package com.ionutbalosin.jvm.energy.consumption.stats;

public class ReportStats {

  public String category;
  public String type;
  public String runIdentifier;

  public int samples;
  public double energy;
  public double geoMeanEnergy;
  public double carbonDioxide;

  public ReportStats(String category, int samples, double geoMeanEnergy) {
    this.category = category;
    this.samples = samples;
    this.geoMeanEnergy = geoMeanEnergy;
  }

  public ReportStats(
      String category, String type, String runIdentifier, int samples, double energy) {
    this.category = category;
    this.type = type;
    this.runIdentifier = runIdentifier;
    this.samples = samples;
    this.energy = energy;
  }

  public ReportStats(String category, String runIdentifier, int samples, double energy) {
    this.category = category;
    this.runIdentifier = runIdentifier;
    this.samples = samples;
    this.energy = energy;
  }

  public ReportStats(
      String category, String runIdentifier, int samples, double energy, double carbonDioxide) {
    this.category = category;
    this.runIdentifier = runIdentifier;
    this.samples = samples;
    this.energy = energy;
    this.carbonDioxide = carbonDioxide;
  }
}
