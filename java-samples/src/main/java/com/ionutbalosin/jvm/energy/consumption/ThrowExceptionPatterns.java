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
import java.util.function.Supplier;

public class ThrowExceptionPatterns {

  // Read the test duration (in seconds) if explicitly set by the "-Dduration=<duration>" property,
  // otherwise default it to 15 minutes
  private final long DURATION_SEC = valueOf(System.getProperty("duration", "9000"));
  private final long DURATION_NS = DURATION_SEC * 1_000_000_000L;

  private final int STACK_DEPTH = 1024;
  private final Supplier<RuntimeException> LAMBDA_PROVIDER_EXCEPTION = () -> new RuntimeException();
  private final RuntimeException CONSTANT_EXCEPTION = new RuntimeException();
  private final int CONSTANT_STACK_TRACES = CONSTANT_EXCEPTION.getStackTrace().length;

  private ExceptionThrower exceptionThrower;
  private String exceptionType;
  private long stackTraces;
  private long iterations;

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
        "Starting %s at %tT, expected duration = %d sec, stack depth = %d%n",
        instance.exceptionThrower.getClass().getName(),
        new Date(),
        instance.DURATION_SEC,
        instance.STACK_DEPTH);

    long startTime = System.nanoTime();
    instance.benchmark(startTime);
    long endTime = System.nanoTime();
    double elapsedTime = (double) (endTime - startTime) / 1_000_000_000L;

    System.out.printf("Successfully finished at %tT%n", new Date());
    System.out.printf("---------------------------------%n");
    System.out.printf("Summary statistics:%n");
    System.out.printf("  Elapsed = %.3f sec%n", elapsedTime);
    System.out.printf("  Ops = %d%n", instance.iterations);
    System.out.printf("  Ops/sec = %.9f%n", instance.iterations / elapsedTime);
    System.out.printf("  Stack trace elements = %d%n", instance.stackTraces);
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
