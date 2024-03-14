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

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

public class LoggingPatterns {

  // Total duration in sec (if not explicitly set by "-Dduration=<duration>", defaults to 20 min)
  private static final long DURATION_SEC = valueOf(System.getProperty("duration", "1200"));
  private static final long DURATION_NS = DURATION_SEC * 1_000_000_000L;

  // Warm-up duration in sec (if not explicitly set by "-Dwarmup=<warmup>", defaults to 5 min)
  private static final long WARMUP_SEC = valueOf(System.getProperty("warmup", "300"));
  private static final long WARMUP_NS = WARMUP_SEC * 1_000_000_000L;

  private final Logger LOGGER = Logger.getLogger(LoggingPatterns.class.getName());
  private final Level LOG_LEVEL = Level.INFO;

  private String aString;
  private int anInt;
  private float aFloat;
  private char aChar;
  private long aLong;
  private double aDouble;
  private boolean aBool;
  private Object anObject;
  private JulLogger julLogger;
  private long iterations;
  private long runs;

  public static void main(String[] args) {
    validateArguments(args);

    LoggingPatterns instance = new LoggingPatterns();
    instance.initialize(args);

    System.out.printf(
        "%s %s %s %n",
        System.getProperty("java.vm.name"),
        System.getProperty("java.vendor"),
        System.getProperty("java.vm.version"));
    System.out.printf(
        "Starting %s at %tT, expected duration = %d sec, warmup = %d sec, log level = %s%n",
        instance.julLogger.getClass().getName(),
        new Date(),
        DURATION_SEC,
        WARMUP_SEC,
        instance.LOG_LEVEL.getName());

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
          Usage: LoggingPatterns <log_type>

          Options:
            <log_type> - must be one of {lambda, guarded_parametrized, guarded_unparametrized,
                                         unguarded_parametrized, unguarded_unparametrized}

          Examples:
            LoggingPatterns lambda
            LoggingPatterns guarded_parametrized
            LoggingPatterns guarded_unparametrized
            LoggingPatterns unguarded_parametrized
            LoggingPatterns unguarded_unparametrized
          """);
      System.exit(1);
    }
  }

  public void initialize(String[] args) {
    String type = args[0];
    switch (type) {
      case "lambda":
        julLogger = new LambdaHeapLogger();
        break;
      case "guarded_parametrized":
        julLogger = new GuardedParametrizedLogger();
        break;
      case "guarded_unparametrized":
        julLogger = new GuardedUnparametrizedLogger();
        break;
      case "unguarded_parametrized":
        julLogger = new UnguardedParametrizedLogger();
        break;
      case "unguarded_unparametrized":
        julLogger = new UnguardedUnparametrizedLogger();
        break;
      default:
        throw new UnsupportedOperationException("Unsupported logger type: " + type);
    }

    OutputStream nullOutputStream = OutputStream.nullOutputStream();
    // OutputStream nullOutputStream = new FileOutputStream("output-" + type + ".log");
    LOGGER.addHandler(
        new StreamHandler(
            new PrintStream(
                new OutputStream() {
                  @Override
                  public void write(int b) throws IOException {
                    nullOutputStream.write(b);
                  }

                  @Override
                  public void write(byte[] b, int off, int len) throws IOException {
                    nullOutputStream.write(b, off, len);
                  }

                  @Override
                  public void write(byte[] b) throws IOException {
                    nullOutputStream.write(b);
                  }
                }),
            new SimpleFormatter()));
    LOGGER.setUseParentHandlers(false);
    LOGGER.setLevel(LOG_LEVEL);

    aString = System.getProperty("user.dir");
    anInt = 0;
    aFloat = 0F;
    aChar = '\u0000';
    aLong = 0L;
    aDouble = 0D;
    aBool = aString.length() > 64 ? true : false;
    anObject = new Object();
  }

  public void benchmark(long startTime) {
    // Benchmark loop: Attempts to run for a specific expected duration
    // Note: This loop may run beyond the expected duration, but it is acceptable for our goal
    while (System.nanoTime() < startTime + DURATION_NS) {
      julLogger.log();
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
    // verification check: if every line was logged, then 'aLong' has been incremented
    if (iterations + 1 != aLong) {
      throw new AssertionError(String.format("Expected = %s, actual = %s", iterations + 1, aLong));
    }
  }

  public abstract class JulLogger {
    public abstract void log();
  }

  public class LambdaHeapLogger extends JulLogger {
    @Override
    public void log() {
      LOGGER.log(
          LOG_LEVEL,
          () ->
              ("[" + aString + "], [" + anInt++ + "], [" + aFloat++ + "], [" + aChar++ + "], ["
                  + aLong++ + "]" + "], [" + aDouble++ + "], [" + aBool + "], [" + anObject + "]"));
    }
  }

  public class GuardedParametrizedLogger extends JulLogger {
    @Override
    public void log() {
      if (LOGGER.isLoggable(LOG_LEVEL)) {
        LOGGER.log(
            LOG_LEVEL,
            "[{0}], [{1}], [{2}], [{3}], [{4}], [{5}], [{6}], [{7}]",
            new Object[] {
              aString, anInt++, aFloat++, aChar++, aLong++, aDouble++, aBool, anObject
            });
      }
    }
  }

  public class GuardedUnparametrizedLogger extends JulLogger {
    @Override
    public void log() {
      if (LOGGER.isLoggable(LOG_LEVEL)) {
        LOGGER.log(
            LOG_LEVEL,
            "[" + aString + "], [" + anInt++ + "], [" + aFloat++ + "], [" + aChar++ + "], ["
                + aLong++ + "]" + "], [" + aDouble++ + "], [" + aBool + "], [" + anObject + "]");
      }
    }
  }

  public class UnguardedParametrizedLogger extends JulLogger {
    @Override
    public void log() {
      LOGGER.log(
          LOG_LEVEL,
          "[{0}], [{1}], [{2}], [{3}], [{4}], [{5}], [{6}], [{7}]",
          new Object[] {aString, anInt++, aFloat++, aChar++, aLong++, aDouble++, aBool, anObject});
    }
  }

  public class UnguardedUnparametrizedLogger extends JulLogger {
    @Override
    public void log() {
      LOGGER.log(
          LOG_LEVEL,
          "[" + aString + "], [" + anInt++ + "], [" + aFloat++ + "], [" + aChar++ + "], [" + aLong++
              + "]" + "], [" + aDouble++ + "], [" + aBool + "], [" + anObject + "]");
    }
  }
}
