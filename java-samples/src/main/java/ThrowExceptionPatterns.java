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

import java.util.function.Supplier;

public class ThrowExceptionPatterns {

    int ITERATIONS = 50_000;
    Supplier<RuntimeException> LAMBDA_PROVIDER_EXCEPTION = () -> new RuntimeException();
    RuntimeException CONSTANT_EXCEPTION = new RuntimeException("Something wrong happened.");
    int stackDepth;
    ExceptionThrower exceptionThrower;

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: ThrowExceptionPatterns <exception_type> <stack_depth>");
            System.out.println("Options:");
            System.out.println("  exception_type  must be {const, lambda, new, override_fist}");
            System.out.println("  stack_depth     defines the recursive method depth");
            System.out.println("Examples:");
            System.out.println("  ThrowExceptionPatterns const 1024");
            System.out.println("  ThrowExceptionPatterns lambda 1024");
            System.out.println("  ThrowExceptionPatterns new 1024");
            System.out.println("  ThrowExceptionPatterns override_fist 1024");
            return;
        }

        ThrowExceptionPatterns instance = new ThrowExceptionPatterns();
        instance.initialize(args[0], args[1]);

        // start the tests
        for (int counter = 0; counter < instance.ITERATIONS; counter++) {
            try {
                instance.exceptionThrower.throw_exception(instance.stackDepth);
            } catch (Exception exc) {
                System.out.printf("[%d] Exception type = %s, stack depth = %d, stack trace elements length = %d\n", counter, instance.exceptionThrower.getClass().getName(), instance.stackDepth, exc.getStackTrace().length);
            }
        }
    }

    public void initialize(String type, String depth) {
        stackDepth = Integer.parseInt(depth);
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
