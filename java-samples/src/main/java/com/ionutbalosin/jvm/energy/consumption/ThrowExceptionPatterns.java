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
package com.ionutbalosin.jvm.energy.consumption;

import java.util.function.Supplier;

public class ThrowExceptionPatterns {

  int ITERATIONS = 100_000;
  int STACK_DEPTH = 1024;
  Supplier<RuntimeException> LAMBDA_PROVIDER_EXCEPTION = () -> new RuntimeException();
  RuntimeException CONSTANT_EXCEPTION = new RuntimeException("Something wrong happened.");
  int CONSTANT_STACK_TRACES = CONSTANT_EXCEPTION.getStackTrace().length;

  ExceptionThrower exceptionThrower;

  public static void main(String[] args) {
    if (args.length != 1) {
      System.out.println("Usage: ThrowExceptionPatterns <exception_type>");
      System.out.println("Options:");
      System.out.println("  exception_type  must be {const, lambda, new, override_fist}");
      System.out.println("Examples:");
      System.out.println("  ThrowExceptionPatterns const");
      System.out.println("  ThrowExceptionPatterns lambda");
      System.out.println("  ThrowExceptionPatterns new");
      System.out.println("  ThrowExceptionPatterns override_fist");
      return;
    }

    ThrowExceptionPatterns instance = new ThrowExceptionPatterns();
    instance.initialize(args[0]);

    // start the tests
    long result = 0;
    for (int counter = 0; counter < instance.ITERATIONS; counter++) {
      try {
        instance.exceptionThrower.throw_exception(instance.STACK_DEPTH);
      } catch (Exception exc) {
        instance.validate_results(args, exc);
        result += exc.getStackTrace().length;
      }
    }

    System.out.printf(
        "Exception type = %s, stack depth = %d, number of generated stack trace elements = %d\n",
        instance.exceptionThrower.getClass().getName(), instance.STACK_DEPTH, result);
  }

  public void initialize(String type) {
    switch (type) {
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
        throw new UnsupportedOperationException("Unsupported exception type: " + type);
    }
  }

  public void validate_results(String[] args, Exception exc) {
    // validate the results (note: the assertion error branch(es) should never be taken)
    if ("const".equals(args[0]) && CONSTANT_STACK_TRACES != exc.getStackTrace().length) {
      throw new AssertionError(
          String.format(
              "Expected = %s, found = %s", CONSTANT_STACK_TRACES, exc.getStackTrace().length));
    }
    if (("lambda".equals(args[0]) || "new".equals(args[0]))
        && STACK_DEPTH > exc.getStackTrace().length) {
      // Note: it depends on the JVM, but the number of generated frames should be (always)
      // greater than STACK_DEPTH (+ eventually 1/2 frames more)
      throw new AssertionError(
          String.format(
              "Expected at least = %s, found = %s", STACK_DEPTH, exc.getStackTrace().length));
    }
    if ("override_fist".equals(args[0]) && 0 != exc.getStackTrace().length) {
      throw new AssertionError(
          String.format("Expected = 0, found = %s", exc.getStackTrace().length));
    }
  }

  public abstract class ExceptionThrower {
    public abstract void throw_exception(int depth);
  }

  public class ThrowConstantException extends ExceptionThrower {
    @Override
    public void throw_exception(int depth) {
      if (depth == 0) {
        throw CONSTANT_EXCEPTION;
      }
      throw_exception(depth - 1);
    }
  }

  public class ThrowLambdaException extends ExceptionThrower {
    @Override
    public void throw_exception(int depth) {
      if (depth == 0) {
        throw LAMBDA_PROVIDER_EXCEPTION.get();
      }
      throw_exception(depth - 1);
    }
  }

  public class ThrowNewException extends ExceptionThrower {
    @Override
    public void throw_exception(int depth) {
      if (depth == 0) {
        throw new RuntimeException();
      }
      throw_exception(depth - 1);
    }
  }

  public class ThrowNewExceptionOverrideFillInStackTrace extends ExceptionThrower {
    @Override
    public void throw_exception(int depth) {
      if (depth == 0) {
        throw new RuntimeException() {
          @Override
          public Throwable fillInStackTrace() {
            return this;
          }
        };
      }
      throw_exception(depth - 1);
    }
  }
}
