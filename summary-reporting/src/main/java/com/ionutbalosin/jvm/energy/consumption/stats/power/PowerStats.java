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
package com.ionutbalosin.jvm.energy.consumption.stats.power;

import com.ionutbalosin.jvm.energy.consumption.stats.TestDescriptor;
import java.util.ArrayList;
import java.util.List;

public class PowerStats {

  // Power stats identifier (running JVM, benchmark name, and run identifier)
  public TestDescriptor descriptor = new TestDescriptor();

  // Total consumed energy (Watt⋅sec)
  public double energy;

  // Total elapsed time (in seconds)
  public double elapsed;

  // Total power samples recorded during the measurement
  public List<PowerSample> samples = new ArrayList<>();

  public static class PowerSample {
    public String date;
    public double watts;
    public double tcpu;
  }
}
