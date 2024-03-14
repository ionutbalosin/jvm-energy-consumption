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
package com.ionutbalosin.jvm.energy.consumption;

import static java.lang.Integer.valueOf;

import java.util.Date;
import java.util.Random;

/*
 * References:
 * - https://mechanical-sympathy.blogspot.com/2012/08/memory-access-patterns-are-important.html
 */
public class MemoryAccessPatterns {

  // Total duration in sec (if not explicitly set by "-Dduration=<duration>", defaults to 20 min)
  private static final long DURATION_SEC = valueOf(System.getProperty("duration", "1200"));
  private static final long DURATION_NS = DURATION_SEC * 1_000_000_000L;

  // Warm-up duration in sec (if not explicitly set by "-Dwarmup=<warmup>", defaults to 5 min)
  private static final long WARMUP_SEC = valueOf(System.getProperty("warmup", "300"));
  private static final long WARMUP_NS = WARMUP_SEC * 1_000_000_000L;

  private final int LONG_SIZE = 8; // 8 bytes
  private final int PAGE_SIZE = 2 * 1024 * 1024; // 2 MB
  private final long FOUR_GIG = 4L * 1024 * 1024 * 1024; // 4 GB
  private final int ARRAY_SIZE = (int) (FOUR_GIG / LONG_SIZE);
  private final int ARRAY_MASK = ARRAY_SIZE - 1;
  private final int WORDS_PER_PAGE = PAGE_SIZE / LONG_SIZE;
  private final int PAGE_MASK = WORDS_PER_PAGE - 1;
  private final int PRIME_INC = 514_229;

  private WalkerStep walkerStep;
  private long[] array;
  private long iterations;
  private long runs;

  public static void main(String[] args) {
    validateArguments(args);

    MemoryAccessPatterns instance = new MemoryAccessPatterns();
    instance.initialize(args);

    System.out.printf(
        "%s %s %s %n",
        System.getProperty("java.vm.name"),
        System.getProperty("java.vendor"),
        System.getProperty("java.vm.version"));
    System.out.printf(
        "Starting %s at %tT, expected duration = %d sec, warmup = %d sec%n",
        instance.walkerStep.getClass().getName(), new Date(), DURATION_SEC, WARMUP_SEC);

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
          Usage: MemoryAccessPatterns <access_type>

          Options:
            <access_type> - must be one of {linear, random_page, random_heap}

          Examples:
            MemoryAccessPatterns linear
            MemoryAccessPatterns random_page
            MemoryAccessPatterns random_heap
          """);
      System.exit(1);
    }
  }

  public void initialize(String[] args) {
    String type = args[0];
    switch (type) {
      case "linear":
        walkerStep = new LinearWalk();
        break;
      case "random_page":
        walkerStep = new RandomPageWalk();
        break;
      case "random_heap":
        walkerStep = new RandomHeapWalk();
        break;
      default:
        throw new UnsupportedOperationException("Unsupported walker step type: " + type);
    }

    Random random = new Random(16384);
    array = new long[ARRAY_SIZE];
    for (int i = 0; i < ARRAY_SIZE; i++) {
      array[i] = random.nextLong(2);
    }
  }

  public void benchmark(long startTime) {
    // Benchmark loop: Attempts to run for a specific expected duration
    // Note: This loop may run beyond the expected duration, but it is acceptable for our goal
    while (System.nanoTime() < startTime + DURATION_NS) {
      long result = memoryAccess();
      validateResults(result);
      iterations++;
      if (System.nanoTime() >= startTime + WARMUP_NS) {
        // If the warm-up phase has completed, start counting the runs
        runs++;
      }
    }
  }

  public long memoryAccess() {
    long result = 0;
    int pos = -1;
    // Walk page by page (of how many pages there are)
    for (int pageOffset = 0; pageOffset < ARRAY_SIZE; pageOffset += WORDS_PER_PAGE) {
      // Walk inside each page (of how many longs we have inside each page)
      for (int wordOffset = pageOffset, limit = pageOffset + WORDS_PER_PAGE;
          wordOffset < limit;
          wordOffset++) {
        pos = walkerStep.next(pageOffset, wordOffset, pos);
        result += array[pos];
      }
    }
    return result;
  }

  // validate the results (Note: The assertion error branch(es) should never be taken)
  public void validateResults(long result) {
    if (268435456L != result) {
      throw new AssertionError(String.format("Expected = 268435456L, actual = %s", result));
    }
  }

  public abstract class WalkerStep {
    public abstract int next(int pageOffset, int wordOffset, int pos);
  }

  public class LinearWalk extends WalkerStep {
    // Walk through array in a linear fashion being completely predictable
    public int next(final int pageOffset, final int wordOffset, final int pos) {
      return (pos + 1) & ARRAY_MASK;
    }
  }

  public class RandomPageWalk extends WalkerStep {
    // Pseudo randomly walk round array within a restricted area then move on.
    // This restricted area is what is commonly known as an operating system page of array
    public int next(final int pageOffset, final int wordOffset, final int pos) {
      return pageOffset + ((pos + PRIME_INC) & PAGE_MASK);
    }
  }

  public class RandomHeapWalk extends WalkerStep {
    // Pseudo randomly walk around a large area of the heap
    public int next(final int pageOffset, final int wordOffset, final int pos) {
      return (pos + PRIME_INC) & ARRAY_MASK;
    }
  }
}
