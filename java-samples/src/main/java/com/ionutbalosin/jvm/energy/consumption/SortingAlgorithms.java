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

/*
 * Selection sort is a simple, in-place comparison-based sorting algorithm with a quadratic time complexity of O(n^2).
 * It repeatedly selects the smallest element from the unsorted portion of the array and swaps it with the first unsorted element,
 * gradually building a sorted sequence. Selection sort's efficiency is limited, making it less suitable for large datasets.
 *
 * Bubble sort is a basic comparison-based sorting algorithm with a time complexity of O(n^2).
 * It repeatedly steps through the list, compares adjacent elements, and swaps them if they are in the wrong order.
 * Bubble sort is inefficient for large datasets and is mainly used for educational purposes or small datasets.
 *
 * Merge sort is a divide-and-conquer algorithm with a time complexity of O(n log n). It divides the input into
 * smaller sub-problems, sorts them, and then merges the sorted sub-arrays. Merge sort generally
 * performs well and is often used as a benchmark for other sorting algorithms.
 *
 * Quick sort is another divide-and-conquer algorithm with an average time complexity of O(n log n).
 * It works by selecting a pivot element and partitioning the array.
 * Quick sort is known for its efficiency and is widely used in practice.
 *
 * Radix sort is a non-comparative sorting algorithm with a time complexity of O(n k), where n is the number of elements and k is the average length of the keys.
 * It sorts the elements by their individual digits or characters, from least significant to most significant.
 * Radix sort can be efficient for sorting large integers or strings.
 *
 * | Sorting Algorithm | Worst Case Time Complexity | Best Case Time Complexity | Average Case Time Complexity |
 * |-------------------|----------------------------|---------------------------|------------------------------|
 * | Quick Sort        | O(n^2)                     | O(n log n)                | O(n log n)                   |
 * | Merge Sort        | O(n log n)                 | O(n log n)                | O(n log n)                   |
 * | Radix Sort        | O(nk)                      | O(nk)                     | O(nk)                        |
 */

import static java.lang.Integer.valueOf;

import java.util.Arrays;
import java.util.Date;

/*
 * References:
 * - https://lemire.me/blog/2021/04/09/how-fast-can-you-sort-arrays-of-integers-in-java
 * - https://www.geeksforgeeks.org/merge-sort
 */
public class SortingAlgorithms {

  // Total duration in sec (if not explicitly set by "-Dduration=<duration>", defaults to 20 min)
  private static final long DURATION_SEC = valueOf(System.getProperty("duration", "1200"));
  private static final long DURATION_NS = DURATION_SEC * 1_000_000_000L;

  // Warm-up duration in sec (if not explicitly set by "-Dwarmup=<warmup>", defaults to 5 min)
  private static final long WARMUP_SEC = valueOf(System.getProperty("warmup", "300"));
  private static final long WARMUP_NS = WARMUP_SEC * 1_000_000_000L;

  private final int INT_SIZE = 4; // 4 bytes
  private final long _1_GB = 1 * 1024 * 1024 * 1024; // 1 GB
  private final int ARRAY_SIZE = (int) (_1_GB / INT_SIZE);

  private Sorter sorter;
  private int[] array;
  private long iterations;
  private long runs;

  public static void main(String[] args) {
    validateArguments(args);

    SortingAlgorithms instance = new SortingAlgorithms();
    instance.initialize(args);

    System.out.printf(
        "%s %s %s %n",
        System.getProperty("java.vm.name"),
        System.getProperty("java.vendor"),
        System.getProperty("java.vm.version"));
    System.out.printf(
        "Starting %s at %tT, expected duration = %d sec, warmup = %d sec, number of elements to"
            + " sort = %d%n",
        instance.sorter.getClass().getName(),
        new Date(),
        DURATION_SEC,
        WARMUP_SEC,
        instance.array.length);

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
          Usage: SortingAlgorithms <algorithm_type>

          Options:
            <algorithm_type> - must be one of {quick_sort, merge_sort, radix_sort}

          Examples:
            SortingAlgorithms quick_sort
            SortingAlgorithms merge_sort
            SortingAlgorithms radix_sort
          """);
      System.exit(1);
    }
  }

  public void initialize(String[] args) {
    String type = args[0];
    switch (type) {
      case "merge_sort":
        sorter = new MergeSorter();
        break;
      case "quick_sort":
        sorter = new QuickSorter();
        break;
      case "radix_sort":
        sorter = new RadixSorter();
        break;
      default:
        throw new UnsupportedOperationException("Unsupported sorting algorithm type: " + type);
    }

    array = new int[ARRAY_SIZE];
  }

  public void benchmark(long startTime) {
    // Benchmark loop: Attempts to run for a specific expected duration
    // Note: This loop may run beyond the expected duration, but it is acceptable for our goal
    while (System.nanoTime() < startTime + DURATION_NS) {
      initializeInteration();
      sorter.sort(array);
      validateResults(array);
      iterations++;
      if (System.nanoTime() >= startTime + WARMUP_NS) {
        // If the warm-up phase has completed, start counting the runs
        runs++;
      }
    }
  }

  private void initializeInteration() {
    // the given array is sorted in descending order, this might lead (for some algorithms) to the
    // maximum number of comparisons
    for (int i = 0; i < ARRAY_SIZE; i++) {
      array[i] = ARRAY_SIZE - i - 1;
    }
  }

  // validate the results (Note: The assertion error branch(es) should never be taken)
  public void validateResults(int array[]) {
    for (int i = 0; i < array.length - 1; i++) {
      if (array[i] > array[i + 1])
        throw new AssertionError(
            String.format("Elements at index %s and %s are not sorted", i, i + 1));
    }
  }

  public abstract class Sorter {
    public abstract void sort(int array[]);
  }

  public class MergeSorter extends Sorter {

    @Override
    public void sort(int[] array) {
      sort(array, 0, array.length - 1);
    }

    // Merges two sub-arrays: array[left...middle] and array[middle+1...right]
    public void merge(int array[], int left, int middle, int right) {
      int n1 = middle - left + 1;
      int n2 = right - middle;

      int left_array[] = new int[n1];
      int right_array[] = new int[n2];

      for (int i = 0; i < n1; ++i) left_array[i] = array[left + i];
      for (int j = 0; j < n2; ++j) right_array[j] = array[middle + 1 + j];

      int i = 0, j = 0;
      int k = left;
      while (i < n1 && j < n2) {
        if (left_array[i] <= right_array[j]) {
          array[k] = left_array[i];
          i++;
        } else {
          array[k] = right_array[j];
          j++;
        }
        k++;
      }

      // Copy remaining elements of left_array[] if any
      while (i < n1) {
        array[k] = left_array[i];
        i++;
        k++;
      }

      // Copy remaining elements of right_array[] if any
      while (j < n2) {
        array[k] = right_array[j];
        j++;
        k++;
      }
    }

    public void sort(int array[], int left, int right) {
      if (left < right) {
        int m = left + (right - left) / 2;

        sort(array, left, m);
        sort(array, m + 1, right);

        merge(array, left, m, right);
      }
    }
  }

  public class QuickSorter extends Sorter {

    @Override
    public void sort(int[] array) {
      Arrays.sort(array);
    }
  }

  public class RadixSorter extends Sorter {

    @Override
    public void sort(int[] array) {
      int[] array_copy = new int[array.length];
      int[] level_0 = new int[257];
      int[] level_1 = new int[257];
      int[] level_2 = new int[257];
      int[] level_3 = new int[257];

      for (int value : array) {
        value -= Integer.MIN_VALUE;
        level_0[(value & 0xFF) + 1]++;
        level_1[((value >>> 8) & 0xFF) + 1]++;
        level_2[((value >>> 16) & 0xFF) + 1]++;
        level_3[((value >>> 24) & 0xFF) + 1]++;
      }

      for (int i = 1; i < level_0.length; ++i) {
        level_0[i] += level_0[i - 1];
        level_1[i] += level_1[i - 1];
        level_2[i] += level_2[i - 1];
        level_3[i] += level_3[i - 1];
      }

      for (int value : array) {
        array_copy[level_0[(value - Integer.MIN_VALUE) & 0xFF]++] = value;
      }

      for (int value : array_copy) {
        array[level_1[((value - Integer.MIN_VALUE) >>> 8) & 0xFF]++] = value;
      }

      for (int value : array) {
        array_copy[level_2[((value - Integer.MIN_VALUE) >>> 16) & 0xFF]++] = value;
      }

      for (int value : array_copy) {
        array[level_3[((value - Integer.MIN_VALUE) >>> 24) & 0xFF]++] = value;
      }
    }
  }
}
