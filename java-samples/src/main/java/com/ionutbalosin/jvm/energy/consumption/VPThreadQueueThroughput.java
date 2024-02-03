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

  // Read the test duration (in seconds) if explicitly set by the "-Dduration=<duration>" property,
  // otherwise default it to 15 minutes
  private final long DURATION = valueOf(System.getProperty("duration", "9000")) * 1_000;

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
        "Starting %s at %tT, expected duration = %d sec, number of tasks = %d%n",
        args[0], new Date(), instance.DURATION / 1000, TASKS);

    long startTime = System.currentTimeMillis();
    instance.benchmark(startTime);
    long endTime = System.currentTimeMillis();
    double elapsedTime = (double) (endTime - startTime) / 1000;

    System.out.printf("Successfully finished at %tT%n", new Date());
    System.out.printf(
        "Summary: elapsed = %.3f sec, ops = %d, sec/ops = %.9f%n",
        elapsedTime, instance.iterations, elapsedTime / instance.iterations);

    instance.tearDown();
  }

  public static void validateArguments(String[] args) {
    if (args.length != 1) {
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
    // benchmark loop: attempts to run for a specific expected duration
    while (System.currentTimeMillis() < startTime + DURATION) {
      counter.reset();
      final CountDownLatch latch = new CountDownLatch(TASKS);
      IntStream.range(0, TASKS).forEach(task -> consumerService.submit(() -> consume(latch)));
      latch.await();
      validateResults(counter.intValue());
      iterations++;
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
