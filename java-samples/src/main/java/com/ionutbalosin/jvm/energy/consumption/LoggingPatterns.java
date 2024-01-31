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
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

public class LoggingPatterns {

  // Read the test duration (in seconds) if explicitly set by the "-Dduration=<duration>" property,
  // otherwise default it to 15 minutes
  long DURATION = valueOf(System.getProperty("duration", "9000")) * 1_000;

  Logger LOGGER = Logger.getLogger(LoggingPatterns.class.getName());
  Level LOG_LEVEL = Level.INFO;

  String aString;
  int anInt;
  float aFloat;
  char aChar;
  long aLong;
  double aDouble;
  boolean aBoolean;
  Object anObject;
  JulLogger julLogger;
  long operations;

  public static void main(String[] args) {
    validateArguments(args);

    LoggingPatterns instance = new LoggingPatterns();
    instance.initialize(args);

    System.out.printf(
        "Starting %s at %tT, expected duration = %d sec, log level = %s%n",
        instance.julLogger.getClass().getName(),
        new Date(),
        instance.DURATION / 1000,
        instance.LOG_LEVEL.getName());

    // benchmark loop: attempts to run for a specific expected duration
    long startTime = System.currentTimeMillis();
    while (System.currentTimeMillis() < startTime + instance.DURATION) {
      instance.julLogger.log();
      instance.operations++;
    }
    long endTime = System.currentTimeMillis();
    double elapsedTime = (double) (endTime - startTime) / 1000;

    System.out.printf("Successfully finished at %tT%n", new Date());
    System.out.printf(
        "Summary: elapsed = %.3f sec, ops = %d, sec/ops = %.9f%n",
        elapsedTime, instance.operations, elapsedTime / instance.operations);
  }

  public static void validateArguments(String[] args) {
    if (args.length != 1) {
      System.out.println(
          """
          Usage: LoggingPatterns <log_type>

          Options:
            <log_type> - must be one of {lambda_heap, lambda_local,
                                         guarded_parametrized, guarded_unparametrized,
                                         unguarded_parametrized, unguarded_unparametrized}

          Examples:
            LoggingPatterns lambda_heap
            LoggingPatterns lambda_local
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

    Random random = new Random(16384);
    aString = System.getProperty("java.home");
    anInt = 1;
    aFloat = 1F;
    aChar = (char) (random.nextInt(26) + 'a');
    aLong = 1L;
    aDouble = 1D;
    aBoolean = random.nextBoolean();
    anObject = new Object();
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
                  + aLong++ + "]" + "], [" + aDouble++ + "], [" + aBoolean + "], [" + anObject
                  + "]"));
    }
  }

  public class LambdaLocalLogger extends JulLogger {
    @Override
    public void log() {
      String localString = aString;
      int localInt = anInt++;
      float localFloat = aFloat++;
      char localChar = aChar++;
      long localLong = aLong++;
      double localDouble = aDouble++;
      boolean localBoolean = aBoolean;
      Object localObject = anObject;
      LOGGER.log(
          LOG_LEVEL,
          () ->
              ("["
                  + localString
                  + "], ["
                  + localInt
                  + "], ["
                  + localFloat
                  + "], ["
                  + localChar
                  + "], ["
                  + localLong
                  + "], ["
                  + localDouble
                  + "], ["
                  + localBoolean
                  + "], ["
                  + localObject
                  + "]"));
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
              aString, anInt++, aFloat++, aChar++, aLong++, aDouble++, aBoolean, anObject
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
                + aLong++ + "]" + "], [" + aDouble++ + "], [" + aBoolean + "], [" + anObject + "]");
      }
    }
  }

  public class UnguardedParametrizedLogger extends JulLogger {
    @Override
    public void log() {
      LOGGER.log(
          LOG_LEVEL,
          "[{0}], [{1}], [{2}], [{3}], [{4}], [{5}], [{6}], [{7}]",
          new Object[] {
            aString, anInt++, aFloat++, aChar++, aLong++, aDouble++, aBoolean, anObject
          });
    }
  }

  public class UnguardedUnparametrizedLogger extends JulLogger {
    @Override
    public void log() {
      LOGGER.log(
          LOG_LEVEL,
          "[" + aString + "], [" + anInt++ + "], [" + aFloat++ + "], [" + aChar++ + "], [" + aLong++
              + "]" + "], [" + aDouble++ + "], [" + aBoolean + "], [" + anObject + "]");
    }
  }
}
