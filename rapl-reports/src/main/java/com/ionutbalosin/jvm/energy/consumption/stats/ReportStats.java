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
package com.ionutbalosin.jvm.energy.consumption.stats;

public class ReportStats {
  public String module;
  public String testCategory;
  public String testType;
  public int samples;
  public double totalEnergy;
  public double meanEnergy;
  public double meanErrorEnergy;
  public double meanPower;
  public double meanErrorPower;
  public double meanTimeElapsed;
  public double meanErrorTimeElapsed;
  public double geoMeanEnergy;
  public double carbonDioxide;

  public ReportStats(
      String module, String testCategory, int samples, double meanPower, double meanErrorPower) {
    this.module = module;
    this.testCategory = testCategory;
    this.samples = samples;
    this.meanPower = meanPower;
    this.meanErrorPower = meanErrorPower;
  }

  public ReportStats(
      String module,
      String testCategory,
      String testType,
      int samples,
      double meanEnergy,
      double meanErrorEnergy,
      double meanTimeElapsed,
      double meanErrorTimeElapsed) {
    this.module = module;
    this.testCategory = testCategory;
    this.testType = testType;
    this.samples = samples;
    this.meanEnergy = meanEnergy;
    this.meanErrorEnergy = meanErrorEnergy;
    this.meanTimeElapsed = meanTimeElapsed;
    this.meanErrorTimeElapsed = meanErrorTimeElapsed;
  }

  public ReportStats(
      String module,
      String testCategory,
      int samples,
      double meanEnergy,
      double meanErrorEnergy,
      double meanTimeElapsed,
      double meanErrorTimeElapsed) {
    this.module = module;
    this.testCategory = testCategory;
    this.samples = samples;
    this.meanEnergy = meanEnergy;
    this.meanErrorEnergy = meanErrorEnergy;
    this.meanTimeElapsed = meanTimeElapsed;
    this.meanErrorTimeElapsed = meanErrorTimeElapsed;
  }

  public ReportStats(
      String testCategory,
      int samples,
      double totalEnergy,
      double geoMeanEnergy,
      double carbonDioxide) {
    this.testCategory = testCategory;
    this.samples = samples;
    this.totalEnergy = totalEnergy;
    this.geoMeanEnergy = geoMeanEnergy;
    this.carbonDioxide = carbonDioxide;
  }
}
