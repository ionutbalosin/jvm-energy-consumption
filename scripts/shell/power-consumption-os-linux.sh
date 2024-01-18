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
  if [ $EUID -ne 0 ] || [ $# -lt 1 ] || [ $# -gt 3 ]; then
    echo "Usage: sudo ./power-consumption-os-linux.sh [--background] [--duration=<duration>] --output-file=<output-file>"
    echo ""
    echo "Options:"
    echo "  --background         An optional parameter to specify if the command runs in the background or not."
    echo "  duration             An optional parameter to specify the duration in seconds. If specified, it needs to be greater than 60 seconds. This is a restriction of the 'powerstat' command."
    echo "  output-file          A mandatory parameter to specify the output file name."
    echo ""
    echo "Note: If the duration is not specified, the command will run for 86400 seconds (i.e., 24 hours) or until interrupted."
    echo "      If --background is specified, the command will run in the background, and the shell prompt is immediately returned; otherwise, it will run in the foreground."
    echo ""
    echo "Examples:"
    echo "  sudo ./power-consumption-os-linux.sh --background --duration=900 --output-file=power-consumption.txt"
    echo "  sudo ./power-consumption-os-linux.sh --output-file=power-consumption.txt"
    echo "  sudo ./power-consumption-os-linux.sh --background --output-file=power-consumption.txt"
    echo ""
    return 1
  fi

  export STATS_OUTPUT_FILE=""
  export APP_RUNNING_TIME="86400"
  export BACKGROUND_MODE=""

  while [ $# -gt 0 ]; do
    case "$1" in
      --background)
        BACKGROUND_MODE="&"
        ;;
      --duration=*)
        APP_RUNNING_TIME="${1#*=}"
        ;;
      --output-file=*)
        STATS_OUTPUT_FILE="${1#*=}"
        ;;
      *)
        echo "ERROR: Unknown command line parameter: $1"
        return 1
        ;;
    esac
    shift
  done

  if [ "$APP_RUNNING_TIME" -lt 60 ]; then
    echo "ERROR: Duration must be greater than 60 seconds."
    return 1
  fi

  if [ -z "$STATS_OUTPUT_FILE" ]; then
    echo "ERROR: Missing mandatory parameter output file."
    return 1
  fi

  echo "Output file: $STATS_OUTPUT_FILE"
  echo "Duration (in sec): $APP_RUNNING_TIME"
  echo "Background mode: $BACKGROUND_MODE"
  echo ""
}

start_power_consumption() {
  echo "Starting power consumption measurements at: $(date) ... "
  eval "sudo powerstat -DfHtn 1 ${APP_RUNNING_TIME}" > "$STATS_OUTPUT_FILE" 2>&1 "$BACKGROUND_MODE"
  export POWER_CONSUMPTION_MONITOR_PID=$!
}

check_power_consumption() {
  # In case of background (i.e., asynchronous) mode, check if the power consumption measurements successfully started
  if [ -n "$BACKGROUND_MODE" ]; then
    if ps -p $POWER_CONSUMPTION_MONITOR_PID > /dev/null; then
      echo "Power consumption measurements with PID $POWER_CONSUMPTION_MONITOR_PID started successfully."
    else
      echo "ERROR: Power consumption measurements failed to start."
    fi
  # Otherwise (i.e., the command was already executed synchronously) just show the termination message
  else
    echo "Power consumption measurements successfully finished at $(date)."
  fi
}

check_command_line_options "$@" || exit 1

start_power_consumption

# Sleep for a short duration to allow the power consumption measurements to start
sleep 1

check_power_consumption