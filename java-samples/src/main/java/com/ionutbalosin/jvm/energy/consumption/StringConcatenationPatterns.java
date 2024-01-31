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

  // Read the test duration (in seconds) if explicitly set by the "-Dduration=<duration>" property,
  // otherwise default it to 15 minutes
  private final long DURATION = valueOf(System.getProperty("duration", "9000")) * 1_000;

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

  public static void main(String[] args) {
    validateArguments(args);

    StringConcatenationPatterns instance = new StringConcatenationPatterns();
    instance.initialize(args);

    System.out.printf(
        "Starting %s at %tT, expected duration = %d sec%n",
        instance.concatPattern.getClass().getName(), new Date(), instance.DURATION / 1000);

    long startTime = System.currentTimeMillis();
    instance.benchmark(startTime);
    long endTime = System.currentTimeMillis();
    double elapsedTime = (double) (endTime - startTime) / 1000;

    System.out.printf("Successfully finished at %tT%n", new Date());
    System.out.printf(
        "Summary: elapsed = %.3f sec, ops = %d, sec/ops = %.9f%n",
        elapsedTime, instance.iterations, elapsedTime / instance.iterations);
  }

  public static void validateArguments(String[] args) {
    if (args.length != 1) {
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

    aString = System.getProperty("java.home");
    anInt = 0;
    aFloat = 0F;
    aChar = '\u0000';
    aLong = 0L;
    aDouble = 0D;
    aBool = aString.length() > 64 ? true : false;
    anObject = new Object();
  }

  public void benchmark(long startTime) {
    // benchmark loop: attempts to run for a specific expected duration
    while (System.currentTimeMillis() < startTime + DURATION) {
      String result = concatPattern.concat();
      validateResults(result);
      iterations++;
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
