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
 * Bubble sort is a simple sorting algorithm with a time complexity of O(n^2).
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
 */

import java.util.Arrays;

/*
 * References:
 * - https://lemire.me/blog/2021/04/09/how-fast-can-you-sort-arrays-of-integers-in-java
 * - https://www.geeksforgeeks.org/merge-sort
 * - https://stackoverflow.com/questions/16195092/optimized-bubble-sort
 */
public class SortingAlgorithms {

  int ARRAY_SIZE = 1_000_000_000;

  Sorter sorter;
  int[] array;

  public static void main(String[] args) {
    if (args.length != 1) {
      System.out.println("Usage: SortingAlgorithms <algorithm_type>");
      System.out.println("Options:");
      System.out.println(
          "  algorithm_type   must be {bubble_sort, merge_sort, quick_sort, radix_sort}");
      System.out.println("Examples:");
      System.out.println("  SortingAlgorithms bubble_sort");
      System.out.println("  SortingAlgorithms merge_sort");
      System.out.println("  SortingAlgorithms quick_sort");
      System.out.println("  SortingAlgorithms radix_sort");
      return;
    }

    SortingAlgorithms instance = new SortingAlgorithms();
    instance.initialize(args[0]);

    // start the tests
    instance.sorter.sort(instance.array);
    instance.validate_results();

    System.out.printf(
        "Sorting algorithm = %s, number of sorted elements = %d\n",
        instance.sorter.getClass().getName(), instance.array.length);
  }

  public void initialize(String type) {
    switch (type) {
      case "bubble_sort":
        sorter = new BubbleSorter();
        break;
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
        throw new UnsupportedOperationException("Unsupported algorithm type: " + type);
    }

    // the given array is sorted in descending order, this might lead (for some algorithms) to the
    // maximum number of comparisons
    array = new int[ARRAY_SIZE];
    for (int i = 0; i < ARRAY_SIZE; i++) {
      array[i] = ARRAY_SIZE - i - 1;
    }
  }

  public void validate_results() {
    // validate the results (note: the assertion error branch(es) should never be taken)
    for (int i = 0; i < array.length - 1; i++) {
      if (array[i] > array[i + 1])
        throw new AssertionError(
            String.format("Elements at index %s and %s are not sorted", i, i + 1));
    }
  }

  public abstract class Sorter {
    public abstract void sort(int array[]);
  }

  public class BubbleSorter extends Sorter {
    @Override
    public void sort(int[] array) {
      int last_swap = array.length - 1;
      for (int i = 1; i < array.length; i++) {
        boolean is_sorted = true;
        int current_swap = -1;

        for (int j = 0; j < last_swap; j++) {
          if (array[j] > array[j + 1]) {
            int temp = array[j];
            array[j] = array[j + 1];
            array[j + 1] = temp;
            is_sorted = false;
            current_swap = j;
          }
        }

        // if no two elements were swapped by inner loop, then break
        if (is_sorted) {
          return;
        }
        last_swap = current_swap;
      }
    }
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
