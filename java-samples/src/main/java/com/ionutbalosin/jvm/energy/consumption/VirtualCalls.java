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

/*
 * References:
 * - https://shipilev.net/jvm/anatomy-quarks/16-megamorphic-virtual-calls
 */
public class VirtualCalls {

  int ITERATIONS = 300_000;
  int ARRAY_SIZE = 9_600;
  CMath[] array;

  public static void main(String[] args) {
    if (args.length != 1) {
      System.out.println("Usage: VirtualCalls <mode>");
      System.out.println("Options:");
      System.out.println("  mode   must be {bimorphic, megamorphic_24}");
      System.out.println("Examples:");
      System.out.println("  VirtualCalls bimorphic");
      System.out.println("  VirtualCalls megamorphic_24");
      return;
    }

    VirtualCalls instance = new VirtualCalls();
    instance.initialize(args[0]);

    // start the tests
    for (int counter = 0; counter < instance.ITERATIONS; counter++) {
      instance.virtual_calls();
      instance.validate_results(args, counter);
    }

    System.out.printf(
        "Virtual call type = %s, number of virtual calls = %d\n",
        args[0], instance.array.length * (long) instance.ITERATIONS);
  }

  public void initialize(String mode) {
    array = new CMath[ARRAY_SIZE];
    switch (mode) {
      case "bimorphic":
        for (int i = 0; i < ARRAY_SIZE; i += 2) {
          array[i] = new Alg1(1);
          array[i + 1] = new Alg2(2);
        }
        break;
      case "megamorphic_24":
        for (int i = 0; i < ARRAY_SIZE; i += 24) {
          array[i] = new Alg1(1);
          array[i + 1] = new Alg2(2);
          array[i + 2] = new Alg3(3);
          array[i + 3] = new Alg4(4);
          array[i + 4] = new Alg5(5);
          array[i + 5] = new Alg6(6);
          array[i + 6] = new Alg7(7);
          array[i + 7] = new Alg8(8);
          array[i + 8] = new Alg9(9);
          array[i + 9] = new Alg10(10);
          array[i + 10] = new Alg11(11);
          array[i + 11] = new Alg12(12);
          array[i + 12] = new Alg13(13);
          array[i + 13] = new Alg14(14);
          array[i + 14] = new Alg15(15);
          array[i + 15] = new Alg16(16);
          array[i + 16] = new Alg17(17);
          array[i + 17] = new Alg18(18);
          array[i + 18] = new Alg19(19);
          array[i + 19] = new Alg20(20);
          array[i + 20] = new Alg21(21);
          array[i + 21] = new Alg22(22);
          array[i + 22] = new Alg23(23);
          array[i + 23] = new Alg24(24);
        }
        break;
      default:
        throw new UnsupportedOperationException("Unsupported mode: " + mode);
    }
  }

  public void virtual_calls() {
    for (CMath instance : array) {
      instance.compute();
    }
  }

  public void validate_results(String[] args, int counter) {
    // validate the results (note: the assertion error branch(es) should never be taken)
    if ("bimorphic".equals(args[0]) && (counter + 1) * 2 != array[1].total) {
      throw new AssertionError(
          String.format("Expected = %s, found = %s", (counter + 1) * 2, array[1].total));
    }
    if ("megamorphic_24".equals(args[0]) && (counter + 1) * 24 != array[23].total) {
      throw new AssertionError(
          String.format("Expected = %s, found = %s", (counter + 1) * 24, array[23].total));
    }
  }

  public abstract class CMath {
    long total;

    public abstract void compute();
  }

  public class Alg1 extends CMath {
    int factor;

    public Alg1(int factor) {
      this.factor = factor;
    }

    public void compute() {
      total += factor;
    }
  }

  public class Alg2 extends CMath {
    int factor;

    public Alg2(int factor) {
      this.factor = factor;
    }

    public void compute() {
      total += factor;
    }
  }

  public class Alg3 extends CMath {
    int factor;

    public Alg3(int factor) {
      this.factor = factor;
    }

    public void compute() {
      total += factor;
    }
  }

  public class Alg4 extends CMath {
    int factor;

    public Alg4(int factor) {
      this.factor = factor;
    }

    public void compute() {
      total += factor;
    }
  }

  public class Alg5 extends CMath {
    int factor;

    public Alg5(int factor) {
      this.factor = factor;
    }

    public void compute() {
      total += factor;
    }
  }

  public class Alg6 extends CMath {
    int factor;

    public Alg6(int factor) {
      this.factor = factor;
    }

    public void compute() {
      total += factor;
    }
  }

  public class Alg7 extends CMath {
    int factor;

    public Alg7(int factor) {
      this.factor = factor;
    }

    public void compute() {
      total += factor;
    }
  }

  public class Alg8 extends CMath {
    int factor;

    public Alg8(int factor) {
      this.factor = factor;
    }

    public void compute() {
      total += factor;
    }
  }

  public class Alg9 extends CMath {
    int factor;

    public Alg9(int factor) {
      this.factor = factor;
    }

    public void compute() {
      total += factor;
    }
  }

  public class Alg10 extends CMath {
    int factor;

    public Alg10(int factor) {
      this.factor = factor;
    }

    public void compute() {
      total += factor;
    }
  }

  public class Alg11 extends CMath {
    int factor;

    public Alg11(int factor) {
      this.factor = factor;
    }

    public void compute() {
      total += factor;
    }
  }

  public class Alg12 extends CMath {
    int factor;

    public Alg12(int factor) {
      this.factor = factor;
    }

    public void compute() {
      total += factor;
    }
  }

  public class Alg13 extends CMath {
    int factor;

    public Alg13(int factor) {
      this.factor = factor;
    }

    public void compute() {
      total += factor;
    }
  }

  public class Alg14 extends CMath {
    int factor;

    public Alg14(int factor) {
      this.factor = factor;
    }

    public void compute() {
      total += factor;
    }
  }

  public class Alg15 extends CMath {
    int factor;

    public Alg15(int factor) {
      this.factor = factor;
    }

    public void compute() {
      total += factor;
    }
  }

  public class Alg16 extends CMath {
    int factor;

    public Alg16(int factor) {
      this.factor = factor;
    }

    public void compute() {
      total += factor;
    }
  }

  public class Alg17 extends CMath {
    int factor;

    public Alg17(int factor) {
      this.factor = factor;
    }

    public void compute() {
      total += factor;
    }
  }

  public class Alg18 extends CMath {
    int factor;

    public Alg18(int factor) {
      this.factor = factor;
    }

    public void compute() {
      total += factor;
    }
  }

  public class Alg19 extends CMath {
    int factor;

    public Alg19(int factor) {
      this.factor = factor;
    }

    public void compute() {
      total += factor;
    }
  }

  public class Alg20 extends CMath {
    int factor;

    public Alg20(int factor) {
      this.factor = factor;
    }

    public void compute() {
      total += factor;
    }
  }

  public class Alg21 extends CMath {
    int factor;

    public Alg21(int factor) {
      this.factor = factor;
    }

    public void compute() {
      total += factor;
    }
  }

  public class Alg22 extends CMath {
    int factor;

    public Alg22(int factor) {
      this.factor = factor;
    }

    public void compute() {
      total += factor;
    }
  }

  public class Alg23 extends CMath {
    int factor;

    public Alg23(int factor) {
      this.factor = factor;
    }

    public void compute() {
      total += factor;
    }
  }

  public class Alg24 extends CMath {
    int factor;

    public Alg24(int factor) {
      this.factor = factor;
    }

    public void compute() {
      total += factor;
    }
  }
}
