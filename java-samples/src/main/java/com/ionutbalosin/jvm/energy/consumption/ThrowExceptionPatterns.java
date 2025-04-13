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
import java.util.function.Supplier;

public class ThrowExceptionPatterns {

  // Total duration in sec (if not explicitly set by "-Dduration=<duration>", defaults to 20 min)
  private static final long DURATION_SEC = valueOf(System.getProperty("duration", "1200"));
  private static final long DURATION_NS = DURATION_SEC * 1_000_000_000L;

  // Warm-up duration in sec (if not explicitly set by "-Dwarmup=<warmup>", defaults to 5 min)
  private static final long WARMUP_SEC = valueOf(System.getProperty("warmup", "300"));
  private static final long WARMUP_NS = WARMUP_SEC * 1_000_000_000L;

  private static final int STACK_DEPTH = 1024;
  private final Supplier<RuntimeException> LAMBDA_PROVIDER_EXCEPTION = () -> new RuntimeException();
  private final RuntimeException CONSTANT_EXCEPTION = new RuntimeException();
  private final int CONSTANT_STACK_TRACES = CONSTANT_EXCEPTION.getStackTrace().length;

  private ExceptionThrower exceptionThrower;
  private String exceptionType;
  private long stackTraces;
  private long iterations;
  private long runs;

  public static void main(String[] args) {
    validateArguments(args);

    ThrowExceptionPatterns instance = new ThrowExceptionPatterns();
    instance.initialize(args);

    System.out.printf(
        "%s %s %s %n",
        System.getProperty("java.vm.name"),
        System.getProperty("java.vendor"),
        System.getProperty("java.vm.version"));
    System.out.printf(
        "Starting %s at %tT, expected duration = %d sec, warmup = %d sec, stack depth = %d%n",
        instance.exceptionThrower.getClass().getName(),
        new Date(),
        DURATION_SEC,
        WARMUP_SEC,
        STACK_DEPTH);

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
    System.out.printf("  Stack trace elements = %d%n", instance.stackTraces);
    System.out.printf(
        "%nNote: Iterations include all executions, while runs begin counting after the warm-up"
            + " phase.%n");
  }

  public static void validateArguments(String[] args) {
    if (args.length < 1) {
      System.out.println(
          """
          Usage: ThrowExceptionPatterns <exception_type>

          Options:
            <exception_type> - must be one of {const, override_fist, lambda, new}

          Examples:
            ThrowExceptionPatterns const
            ThrowExceptionPatterns override_fist
            ThrowExceptionPatterns lambda
            ThrowExceptionPatterns new
          """);
      System.exit(1);
    }
  }

  public void initialize(String[] args) {
    exceptionType = args[0];
    switch (exceptionType) {
      case "const":
        exceptionThrower = new ThrowConstantException();
        break;
      case "lambda":
        exceptionThrower = new ThrowLambdaException();
        break;
      case "new":
        exceptionThrower = new ThrowNewException();
        break;
      case "override_fist":
        exceptionThrower = new ThrowNewExceptionOverrideFillInStackTrace();
        break;
      default:
        throw new UnsupportedOperationException("Unsupported exception type: " + exceptionType);
    }
  }

  // validate the results (Note: The assertion error branch(es) should never be taken)
  public void validateResults(int stackTraces) {
    if ("const".equals(exceptionType) && CONSTANT_STACK_TRACES != stackTraces) {
      throw new AssertionError(
          String.format("Expected = %s, actual = %s", CONSTANT_STACK_TRACES, stackTraces));
    }
    if (("lambda".equals(exceptionType) || "new".equals(exceptionType))
        && STACK_DEPTH > stackTraces) {
      // Note: The number of generated frames depends on the JVM, but it should (always)
      // be greater than STACK_DEPTH (plus eventually 1 or 2 frames more)
      throw new AssertionError(
          String.format("Expected at least = %s, actual = %s", STACK_DEPTH, stackTraces));
    }
    if ("override_fist".equals(exceptionType) && 0 != stackTraces) {
      throw new AssertionError(String.format("Expected = 0, actual = %s", stackTraces));
    }
  }

  public void benchmark(long startTime) {
    // Benchmark loop: Attempts to run for a specific expected duration
    // Note: This loop may run beyond the expected duration, but it is acceptable for our goal
    while (System.nanoTime() < startTime + DURATION_NS) {
      try {
        exceptionThrower.throwException(STACK_DEPTH);
      } catch (Exception exc) {
        validateResults(exc.getStackTrace().length);
        stackTraces += exc.getStackTrace().length;
        iterations++;
        if (System.nanoTime() >= startTime + WARMUP_NS) {
          // If the warm-up phase has completed, start counting the runs
          runs++;
        }
      }
    }
  }

  public abstract class ExceptionThrower {
    public abstract void throwException(int depth);
  }

  public class ThrowConstantException extends ExceptionThrower {
    @Override
    public void throwException(int depth) {
      if (depth == 0) {
        throw CONSTANT_EXCEPTION;
      }
      throwException(depth - 1);
    }
  }

  public class ThrowLambdaException extends ExceptionThrower {
    @Override
    public void throwException(int depth) {
      if (depth == 0) {
        throw LAMBDA_PROVIDER_EXCEPTION.get();
      }
      throwException(depth - 1);
    }
  }

  public class ThrowNewException extends ExceptionThrower {
    @Override
    public void throwException(int depth) {
      if (depth == 0) {
        throw new RuntimeException();
      }
      throwException(depth - 1);
    }
  }

  public class ThrowNewExceptionOverrideFillInStackTrace extends ExceptionThrower {
    @Override
    public void throwException(int depth) {
      if (depth == 0) {
        throw new RuntimeException() {
          @Override
          public Throwable fillInStackTrace() {
            return this;
          }
        };
      }
      throwException(depth - 1);
    }
  }
}
