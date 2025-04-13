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
import static java.lang.Thread.ofPlatform;
import static java.lang.Thread.ofVirtual;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor;

import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.IntStream;

public class VPThreadQueueThroughput {

  // Total duration in sec (if not explicitly set by "-Dduration=<duration>", defaults to 20 min)
  private static final long DURATION_SEC = valueOf(System.getProperty("duration", "1200"));
  private static final long DURATION_NS = DURATION_SEC * 1_000_000_000L;

  // Warm-up duration in sec (if not explicitly set by "-Dwarmup=<warmup>", defaults to 5 min)
  private static final long WARMUP_SEC = valueOf(System.getProperty("warmup", "300"));
  private static final long WARMUP_NS = WARMUP_SEC * 1_000_000_000L;

  private static final int PARALLELISM_COUNT =
      ofNullable(System.getProperty("jdk.virtualThreadScheduler.parallelism"))
          .map(Integer::parseInt)
          .orElse(Runtime.getRuntime().availableProcessors());
  private static final int TASKS = PARALLELISM_COUNT * 100_000;

  private final Integer ELEMENT = 42;
  private final BlockingQueue<Integer> queue = new SynchronousQueue<>();
  private final LongAdder counter = new LongAdder();

  private Thread producerThread;
  private ExecutorService consumerService;
  private long iterations;
  private long runs;

  public static void main(String[] args) throws InterruptedException {
    validateArguments(args);

    VPThreadQueueThroughput instance = new VPThreadQueueThroughput();
    instance.initialize(args);

    System.out.printf(
        "%s %s %s %n",
        System.getProperty("java.vm.name"),
        System.getProperty("java.vendor"),
        System.getProperty("java.vm.version"));
    System.out.printf(
        "Starting %s at %tT, expected duration = %d sec, warmup = %d sec, number of tasks = %d%n",
        args[0], new Date(), DURATION_SEC, WARMUP_SEC, TASKS);

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

    instance.tearDown();
  }

  public static void validateArguments(String[] args) {
    if (args.length < 1) {
      System.out.println(
          """
            Usage: VPThreadQueueThroughput <thread_type>

            Options:
              <thread_type> - must be one of {virtual, platform}

            Examples:
              VPThreadQueueThroughput virtual
              VPThreadQueueThroughput platform
            """);
      System.exit(1);
    }
  }

  public void initialize(String[] args) {
    String threadType = args[0];
    switch (threadType) {
      case "virtual":
        producerThread = ofVirtual().unstarted(() -> produce());
        consumerService = newVirtualThreadPerTaskExecutor();
        break;
      case "platform":
        producerThread = ofPlatform().unstarted(() -> produce());
        consumerService = newFixedThreadPool(PARALLELISM_COUNT, ofPlatform().factory());
        break;
      default:
        throw new UnsupportedOperationException("Unsupported thread type: " + threadType);
    }

    producerThread.start();
  }

  public void tearDown() {
    producerThread.interrupt();
    consumerService.shutdownNow();
  }

  public void benchmark(long startTime) throws InterruptedException {
    // Benchmark loop: Attempts to run for a specific expected duration
    // Note: This loop may run beyond the expected duration, but it is acceptable for our goal
    while (System.nanoTime() < startTime + DURATION_NS) {
      counter.reset();
      final CountDownLatch latch = new CountDownLatch(TASKS);
      IntStream.range(0, TASKS).forEach(task -> consumerService.submit(() -> consume(latch)));
      latch.await();
      validateResults(counter.intValue());
      iterations++;
      if (System.nanoTime() >= startTime + WARMUP_NS) {
        // If the warm-up phase has completed, start counting the runs
        runs++;
      }
    }
  }

  public void consume(CountDownLatch latch) {
    try {
      if (queue.take() == ELEMENT) {
        counter.increment();
        latch.countDown();
      }
    } catch (InterruptedException ex) {
      throw new RuntimeException(ex);
    }
  }

  public void produce() {
    boolean interrupted = false;
    while (!interrupted) {
      try {
        // If necessary, a backoff strategy can be employed to lower the producer rate
        queue.put(ELEMENT);
      } catch (InterruptedException ignore) {
        interrupted = true;
      }
    }
  }

  // validate the results (Note: The assertion error branch(es) should never be taken)
  public void validateResults(int value) {
    if (TASKS != value) {
      throw new AssertionError(
          String.format("Expected = %s, actual = %s", TASKS, counter.intValue()));
    }
  }
}
