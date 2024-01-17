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
  if [[ $EUID -ne 0 || ($# -ne 1 && $# -ne 2) ]]; then
    echo "Usage: sudo ./power-consumption-os-linux.sh <output-file> [<duration>]"
    echo ""
    echo "Options:"
    echo "  output-file          A mandatory parameter to specify the output file name."
    echo "  duration             An optional parameter to specify the duration in minutes."
    echo ""
    echo "Note: If the duration is not specified, the command will run indefinitely or until interrupted."
    echo ""
    echo "Examples:"
    echo "  sudo ./power-consumption-os-linux.sh power-consumption.txt"
    echo "  sudo ./power-consumption-os-linux.sh power-consumption.txt 900"
    echo ""
    return 1
  fi

  export TEST_RUN_OUTPUT_FILE="$1"
  if [ "$2" ]; then
    export TEST_RUN_DURATION="$2"
  fi

  echo "Test output file: $TEST_RUN_OUTPUT_FILE"
  echo "Test duration (in min): $TEST_RUN_DURATION"
}

start_power_consumption() {
  echo "Starting power consumption measurements at: $(date) ... "
  if [ -n "$TEST_RUN_DURATION" ]; then
    nohup timeout "${TEST_RUN_DURATION}m" sudo powerstat -DfHtn > "$TEST_RUN_OUTPUT_FILE" 2>&1 &
  else
    nohup sudo powerstat -DfHtn > "$TEST_RUN_OUTPUT_FILE" 2>&1 &
  fi
  export POWER_CONSUMPTION_MONITOR_PID=$!
}

check_power_consumption_started() {
  if ps -p $POWER_CONSUMPTION_MONITOR_PID > /dev/null; then
    echo "Power consumption measurements with PID $POWER_CONSUMPTION_MONITOR_PID started successfully."
  else
    echo "ERROR: Failed to start power consumption measurements."
  fi
}

check_command_line_options "$@" || exit 1

start_power_consumption

# Sleep for a short duration to allow the power consumption measurements to start
sleep 1

check_power_consumption_started