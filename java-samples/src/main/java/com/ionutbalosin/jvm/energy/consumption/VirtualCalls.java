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
package com.ionutbalosin.jvm.energy.consumption;

import static java.lang.Integer.valueOf;

import java.util.Date;

/*
 * References:
 * - https://shipilev.net/jvm/anatomy-quarks/16-megamorphic-virtual-calls
 */
public class VirtualCalls {

  // Total duration in sec (if not explicitly set by "-Dduration=<duration>", defaults to 20 min)
  private static final long DURATION_SEC = valueOf(System.getProperty("duration", "1200"));
  private static final long DURATION_NS = DURATION_SEC * 1_000_000_000L;

  // Warm-up duration in sec (if not explicitly set by "-Dwarmup=<warmup>", defaults to 5 min)
  private static final long WARMUP_SEC = valueOf(System.getProperty("warmup", "300"));
  private static final long WARMUP_NS = WARMUP_SEC * 1_000_000_000L;

  private final int ARRAY_SIZE = 9_600;

  private CMath[] array;
  private int targetTypes;
  private long iterations;
  private long runs;

  public static void main(String[] args) {
    validateArguments(args);

    VirtualCalls instance = new VirtualCalls();
    instance.initialize(args);

    System.out.printf(
        "%s %s %s %n",
        System.getProperty("java.vm.name"),
        System.getProperty("java.vendor"),
        System.getProperty("java.vm.version"));
    System.out.printf(
        "Starting %s at %tT, expected duration = %d sec, warmup = %d sec, number of virtual calls ="
            + " %d%n",
        args[0], new Date(), DURATION_SEC, WARMUP_SEC, instance.array.length);

    long startTime = System.nanoTime();
    instance.benchmark(startTime);
    long endTime = System.nanoTime();
    double elapsedTime = (double) (endTime - startTime) / 1_000_000_000L;

    System.out.printf("Successfully finished at %tT%n", new Date());
    System.out.printf("---------------------------------%n");
    System.out.printf("Summary statistics:%n");
    System.out.printf("  Elapsed = %.3f sec%n", elapsedTime);
    System.out.printf("  Iterations = %d%n", instance.iterations);
    System.out.printf("  Iterations/sec = %.9f%n", instance.iterations / elapsedTime);
    System.out.printf("  Runs = %d%n", instance.runs);
    System.out.printf("  Runs/sec = %.9f%n", instance.runs / (elapsedTime - WARMUP_SEC));
    System.out.printf(
        "%nNote: Iterations include all executions, while runs begin counting after the warm-up"
            + " phase.%n");
  }

  public static void validateArguments(String[] args) {
    if (args.length < 1) {
      System.out.println(
          """
          Usage: VirtualCalls <mode>

          Options:
            <mode> - must be one of {monomorphic, bimorphic, megamorphic_24}

          Examples:
            VirtualCalls monomorphic
            VirtualCalls bimorphic
            VirtualCalls megamorphic_8
          """);
      System.exit(1);
    }
  }

  public void initialize(String[] args) {
    String mode = args[0];
    array = new CMath[ARRAY_SIZE];
    switch (mode) {
      case "monomorphic":
        targetTypes = 1;
        for (int i = 0; i < ARRAY_SIZE; i += 1) {
          array[i] = new Alg1(1);
        }
        break;
      case "bimorphic":
        targetTypes = 2;
        for (int i = 0; i < ARRAY_SIZE; i += 2) {
          array[i] = new Alg1(1);
          array[i + 1] = new Alg2(2);
        }
        break;
      case "megamorphic_3":
        targetTypes = 3;
        for (int i = 0; i < ARRAY_SIZE; i += 3) {
          array[i] = new Alg1(1);
          array[i + 1] = new Alg2(2);
          array[i + 2] = new Alg3(3);
        }
        break;
      case "megamorphic_8":
        targetTypes = 8;
        for (int i = 0; i < ARRAY_SIZE; i += 8) {
          array[i] = new Alg1(1);
          array[i + 1] = new Alg2(2);
          array[i + 2] = new Alg3(3);
          array[i + 3] = new Alg4(4);
          array[i + 4] = new Alg5(5);
          array[i + 5] = new Alg6(6);
          array[i + 6] = new Alg7(7);
          array[i + 7] = new Alg8(8);
        }
        break;
      default:
        throw new UnsupportedOperationException("Unsupported mode: " + mode);
    }
  }

  public void virtualCalls() {
    for (CMath instance : array) {
      instance.compute();
    }
  }

  public void benchmark(long startTime) {
    // Benchmark loop: Attempts to run for a specific expected duration
    // Note: This loop may run beyond the expected duration, but it is acceptable for our goal
    while (System.nanoTime() < startTime + DURATION_NS) {
      virtualCalls();
      validateResults();
      iterations++;
      if (System.nanoTime() >= startTime + WARMUP_NS) {
        // If the warm-up phase has completed, start counting the runs
        runs++;
      }
    }
  }

  // validate the results (Note: The assertion error branch(es) should never be taken)
  public void validateResults() {
    long totalExpected = (iterations + 1) * targetTypes;
    if (totalExpected != array[targetTypes - 1].total) {
      throw new AssertionError(
          String.format("Expected = %s, actual = %s", totalExpected, array[targetTypes - 1].total));
    }
  }

  public abstract class CMath {
    long total;

    public abstract void compute();
  }

  public class Alg1 extends CMath {
    int factor;

    public Alg1(int factor) {
      this.factor = factor;
    }

    public void compute() {
      total += factor;
    }
  }

  public class Alg2 extends CMath {
    int factor;

    public Alg2(int factor) {
      this.factor = factor;
    }

    public void compute() {
      total += factor;
    }
  }

  public class Alg3 extends CMath {
    int factor;

    public Alg3(int factor) {
      this.factor = factor;
    }

    public void compute() {
      total += factor;
    }
  }

  public class Alg4 extends CMath {
    int factor;

    public Alg4(int factor) {
      this.factor = factor;
    }

    public void compute() {
      total += factor;
    }
  }

  public class Alg5 extends CMath {
    int factor;

    public Alg5(int factor) {
      this.factor = factor;
    }

    public void compute() {
      total += factor;
    }
  }

  public class Alg6 extends CMath {
    int factor;

    public Alg6(int factor) {
      this.factor = factor;
    }

    public void compute() {
      total += factor;
    }
  }

  public class Alg7 extends CMath {
    int factor;

    public Alg7(int factor) {
      this.factor = factor;
    }

    public void compute() {
      total += factor;
    }
  }

  public class Alg8 extends CMath {
    int factor;

    public Alg8(int factor) {
      this.factor = factor;
    }

    public void compute() {
      total += factor;
    }
  }
}
