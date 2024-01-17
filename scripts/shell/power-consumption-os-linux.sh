#!/bin/bash
#
# JVM Energy Consumption
#
# MIT License
#
# Copyright (c) 2023-2024 Ionut Balosin, Ko Turk
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.
#

check_command_line_options() {
  if [[ $EUID -ne 0 || $# -ne 3 ]]; then
    echo "Usage: sudo ./power-consumption-os-linux.sh <test-run-identifier> <duration> <output-file>"
    echo ""
    echo "Options:"
    echo "  test-run-identifier  A mandatory parameter to identify the current execution test."
    echo "  duration             A mandatory parameter to specify the duration in minutes."
    echo "  output-file          A mandatory parameter to specify the output file name."
    echo ""
    echo "Examples:"
    echo "  sudo ./power-consumption-os-linux.sh 1 900 power-consumption.txt"
    echo ""
    return 1
  fi

  export TEST_RUN_IDENTIFIER="$1"
  export TEST_RUN_DURATION="$2"
  export TEST_RUN_OUTPUT_FILE="$3"

  echo "Test run identifier: $TEST_RUN_IDENTIFIER"
  echo "Test duration (in min): $TEST_RUN_DURATION"
  echo "Test output file: $TEST_RUN_OUTPUT_FILE"
}

check_command_line_options "$@"

echo "Running power consumption measurements at: $(date) ... "
nohup timeout "${TEST_RUN_DURATION}m" sudo powerstat12 -DfHtn > "$TEST_RUN_OUTPUT_FILE" 2>&1 &

# Sleep for a short duration to allow the command to start
sleep 1

if ps -p $! > /dev/null; then
  echo "The power consumption measurements started successfully."
else
  echo "ERROR: Failed to start the power consumption measurements."
fi