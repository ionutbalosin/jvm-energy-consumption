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

import java.util.Random;

/*
 * References:
 * - https://mechanical-sympathy.blogspot.com/2012/08/memory-access-patterns-are-important.html
 */
public class MemoryAccessPatterns {

    int ITERATIONS = 5;

    int LONG_SIZE = 8; // 8 bytes
    int PAGE_SIZE = 2 * 1024 * 1024; // 2 MB (each Page)
    int ONE_GIG = 1024 * 1024 * 1024; // 1 GB
    long FOUR_GIG = 4L * ONE_GIG; // 4GB
    int ARRAY_SIZE = (int) (FOUR_GIG / LONG_SIZE);
    int ARRAY_MASK = ARRAY_SIZE - 1;
    int WORDS_PER_PAGE = PAGE_SIZE / LONG_SIZE;
    int PAGE_MASK = WORDS_PER_PAGE - 1;
    int PRIME_INC = 514_229;
    WalkerStep walkerStep;
    long[] array;

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: MemoryAccessPatterns <access_type>");
            System.out.println("Options:");
            System.out.println("  access_type must be {linear, random_page, random_heap}");
            System.out.println("Examples:");
            System.out.println("  MemoryAccessPatterns linear");
            System.out.println("  MemoryAccessPatterns random_page");
            System.out.println("  MemoryAccessPatterns random_heap");
            return;
        }

        MemoryAccessPatterns instance = new MemoryAccessPatterns();
        instance.initialize(args[0]);

        // start the tests
        for (int counter = 0; counter < instance.ITERATIONS; counter++) {
            long result = instance.memory_access();
            System.out.printf("[%d] Memory access type = %s, sum of all the accessed elements = %d\n", counter, instance.walkerStep.getClass().getName(), result);
        }
    }

    public void initialize(String type) {
        switch (type) {
            case "linear":
                walkerStep = new LinearWalk();
                break;
            case "random_page":
                walkerStep = new RandomPageWalk();
                break;
            case "random_heap":
                walkerStep = new RandomHeapWalk();
                break;
            default:
                throw new UnsupportedOperationException("Unsupported walker step type: " + type);
        }

        Random random = new Random(16384);
        array = new long[ARRAY_SIZE];
        for (int i = 0; i < ARRAY_SIZE; i++) {
            array[i] = random.nextLong(2);
        }
    }

    public long memory_access() {
        long result = 0;
        int pos = -1;
        // Walk page by page (of how many pages there are)
        for (int pageOffset = 0; pageOffset < ARRAY_SIZE; pageOffset += WORDS_PER_PAGE) {
            // Walk inside each page (of how many longs we have inside each page)
            for (int wordOffset = pageOffset, limit = pageOffset + WORDS_PER_PAGE; wordOffset < limit; wordOffset++) {
                pos = walkerStep.next(pageOffset, wordOffset, pos);
                result += array[pos];
            }
        }
        return result;
    }

    public abstract class WalkerStep {
        public abstract int next(int pageOffset, int wordOffset, int pos);
    }

    public class LinearWalk extends WalkerStep {
        // Walk through array in a linear fashion being completely predictable
        public int next(final int pageOffset, final int wordOffset, final int pos) {
            return (pos + 1) & ARRAY_MASK;
        }
    }

    public class RandomPageWalk extends WalkerStep {
        // Pseudo randomly walk round array within a restricted area then move on.
        // This restricted area is what is commonly known as an operating system page of array
        public int next(final int pageOffset, final int wordOffset, final int pos) {
            return pageOffset + ((pos + PRIME_INC) & PAGE_MASK);
        }
    }

    public class RandomHeapWalk extends WalkerStep {
        // Pseudo randomly walk around a large area of the heap
        public int next(final int pageOffset, final int wordOffset, final int pos) {
            return (pos + PRIME_INC) & ARRAY_MASK;
        }
    }

}
