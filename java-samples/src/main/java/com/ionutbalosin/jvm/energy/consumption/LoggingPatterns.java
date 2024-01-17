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

import static java.lang.String.format;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

public class LoggingPatterns {

  Logger LOGGER = Logger.getLogger(LoggingPatterns.class.getName());
  Level LOG_LEVEL = Level.INFO;
  int ITERATIONS = 1_000_000;

  String aString;
  int anInt;
  float aFloat;
  boolean aBoolean;
  char aChar;
  JulLogger julLogger;

  public static void main(String[] args) {
    if (args.length != 1) {
      System.out.println("Usage: LoggingPatterns <log_type>");
      System.out.println("Options:");
      System.out.println(
          "  log_type   must be {string_format, lambda_heap, lambda_local, guarded_parametrized,"
              + " guarded_unparametrized, unguarded_parametrized, unguarded_unparametrized}");
      System.out.println("Examples:");
      System.out.println("  LoggingPatterns string_format");
      System.out.println("  LoggingPatterns lambda_heap");
      System.out.println("  LoggingPatterns lambda_local");
      System.out.println("  LoggingPatterns guarded_parametrized");
      System.out.println("  LoggingPatterns guarded_unparametrized");
      System.out.println("  LoggingPatterns unguarded_parametrized");
      System.out.println("  LoggingPatterns unguarded_unparametrized");
      return;
    }

    LoggingPatterns instance = new LoggingPatterns();
    instance.initialize(args[0]);

    // start the tests
    for (int counter = 0; counter < instance.ITERATIONS; counter++) {
      instance.julLogger.log();
      instance.validate_results(counter);
    }

    System.out.printf(
        "Log level = %s, log type = %s, number of logged lines = %d\n",
        instance.LOG_LEVEL.getName(), instance.julLogger.getClass().getName(), instance.ITERATIONS);
  }

  public void initialize(String type) {
    switch (type) {
      case "string_format":
        julLogger = new StringFormatLogger();
        break;
      case "lambda_heap":
        julLogger = new LambdaHeapLogger();
        break;
      case "lambda_local":
        julLogger = new LambdaLocalLogger();
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

    Random random = new Random(16384);
    aString = System.getProperty("java.home");
    anInt = 0;
    aFloat = random.nextFloat(4);
    aBoolean = random.nextBoolean();
    aChar = (char) (random.nextInt(26) + 'a');
  }

  public void validate_results(int counter) {
    // validate the results (note: the assertion error branch(es) should never be taken)
    if (counter + 1 != anInt) {
      throw new AssertionError(String.format("Expected = %s, found = %s", counter + 1, anInt));
    }
  }

  public abstract class JulLogger {
    public abstract void log();
  }

  public class StringFormatLogger extends JulLogger {
    @Override
    public void log() {
      LOGGER.log(
          LOG_LEVEL,
          format("[%s], [%s], [%s], [%s], [%s]", aString, ++anInt, aBoolean, aFloat++, ++aChar));
    }
  }

  public class LambdaHeapLogger extends JulLogger {
    @Override
    public void log() {
      LOGGER.log(
          LOG_LEVEL,
          () ->
              ("[" + aString + "], [" + (++anInt) + "], [" + aBoolean + "], [" + aFloat++ + "], ["
                  + ++aChar + "]"));
    }
  }

  public class LambdaLocalLogger extends JulLogger {
    @Override
    public void log() {
      String localString = aString;
      int localInt = ++anInt;
      boolean localBoolean = aBoolean;
      float localFloat = aFloat++;
      char localChar = ++aChar;
      LOGGER.log(
          LOG_LEVEL,
          () ->
              ("["
                  + localString
                  + "], ["
                  + localInt
                  + "], ["
                  + localBoolean
                  + "], ["
                  + localFloat
                  + "], ["
                  + localChar
                  + "]"));
    }
  }

  public class GuardedParametrizedLogger extends JulLogger {
    @Override
    public void log() {
      if (LOGGER.isLoggable(LOG_LEVEL)) {
        LOGGER.log(
            LOG_LEVEL,
            "[{0}], [{1}], [{2}], [{3}], [{4}]",
            new Object[] {aString, ++anInt, aBoolean, aFloat++, ++aChar});
      }
    }
  }

  public class GuardedUnparametrizedLogger extends JulLogger {
    @Override
    public void log() {
      if (LOGGER.isLoggable(LOG_LEVEL)) {
        LOGGER.log(
            LOG_LEVEL,
            "[" + aString + "], [" + (++anInt) + "], [" + aBoolean + "], [" + aFloat++ + "], ["
                + ++aChar + "]");
      }
    }
  }

  public class UnguardedParametrizedLogger extends JulLogger {
    @Override
    public void log() {
      LOGGER.log(
          LOG_LEVEL,
          "[{0}], [{1}], [{2}], [{3}], [{4}]",
          new Object[] {aString, ++anInt, aBoolean, aFloat++, ++aChar});
    }
  }

  public class UnguardedUnparametrizedLogger extends JulLogger {
    @Override
    public void log() {
      LOGGER.log(
          LOG_LEVEL,
          "[" + aString + "], [" + (++anInt) + "], [" + aBoolean + "], [" + aFloat++ + "], ["
              + ++aChar + "]");
    }
  }
}
