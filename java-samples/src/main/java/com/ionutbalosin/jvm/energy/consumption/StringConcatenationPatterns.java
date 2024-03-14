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
import static java.lang.StringTemplate.STR;

import java.util.Date;

public class StringConcatenationPatterns {

  // Total duration in sec (if not explicitly set by "-Dduration=<duration>", defaults to 20 min)
  private static final long DURATION_SEC = valueOf(System.getProperty("duration", "1200"));
  private static final long DURATION_NS = DURATION_SEC * 1_000_000_000L;

  // Warm-up duration in sec (if not explicitly set by "-Dwarmup=<warmup>", defaults to 5 min)
  private static final long WARMUP_SEC = valueOf(System.getProperty("warmup", "300"));
  private static final long WARMUP_NS = WARMUP_SEC * 1_000_000_000L;

  private String aString;
  private int anInt;
  private float aFloat;
  private char aChar;
  private long aLong;
  private double aDouble;
  private boolean aBool;
  private Object anObject;
  private StringConcat concatPattern;
  private long iterations;
  private long runs;

  public static void main(String[] args) {
    validateArguments(args);

    StringConcatenationPatterns instance = new StringConcatenationPatterns();
    instance.initialize(args);

    System.out.printf(
        "%s %s %s %n",
        System.getProperty("java.vm.name"),
        System.getProperty("java.vendor"),
        System.getProperty("java.vm.version"));
    System.out.printf(
        "Starting %s at %tT, expected duration = %d sec, warmup = %d sec%n",
        instance.concatPattern.getClass().getName(), new Date(), DURATION_SEC, WARMUP_SEC);

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
          Usage: StringConcatenationPatterns <concat_pattern>

          Options:
            <concat_pattern> - must be one of {plus_operator, string_builder, string_template}

          Examples:
            StringConcatenationPatterns plus_operator
            StringConcatenationPatterns string_builder
            StringConcatenationPatterns string_template
          """);
      System.exit(1);
    }
  }

  public void initialize(String[] args) {
    String pattern = args[0];
    switch (pattern) {
      case "string_builder":
        concatPattern = new StringBuilderConcat();
        break;
      case "plus_operator":
        concatPattern = new PlusOperatorConcat();
        break;
      case "string_template":
        concatPattern = new StringTemplateConcat();
        break;
      default:
        throw new UnsupportedOperationException("Unsupported pattern: " + pattern);
    }

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
      String result = concatPattern.concat();
      validateResults(result);
      iterations++;
      if (System.nanoTime() >= startTime + WARMUP_NS) {
        // If the warm-up phase has completed, start counting the runs
        runs++;
      }
    }
  }

  // validate the results (Note: The assertion error branch(es) should never be taken)
  public void validateResults(String result) {
    // verification check: If every String was created, then its length should always be
    // greater than 'aString', and 'aLong' should have been incremented.
    if (result.length() > aString.length() && iterations + 1 != aLong) {
      throw new AssertionError(String.format("Expected = %s, actual = %s", iterations + 1, aLong));
    }
  }

  public abstract class StringConcat {
    public abstract String concat();
  }

  public class StringBuilderConcat extends StringConcat {
    @Override
    public String concat() {
      // Do not explicitly set a capacity
      return new StringBuilder()
          .append(aString)
          .append(anInt++)
          .append(aFloat++)
          .append(aChar++)
          .append(aLong++)
          .append(aDouble++)
          .append(aBool)
          .append(anObject)
          .toString();
    }
  }

  public class PlusOperatorConcat extends StringConcat {
    @Override
    public String concat() {
      return aString + anInt++ + aFloat++ + aChar++ + aLong++ + aDouble++ + aBool + anObject;
    }
  }

  public class StringTemplateConcat extends StringConcat {
    @Override
    public String concat() {
      return STR."\{
          aString}\{
          anInt++}\{
          aFloat++}\{
          aChar++}\{
          aLong++}\{
          aDouble++}\{
          aBool}\{
          anObject}";
    }
  }
}
