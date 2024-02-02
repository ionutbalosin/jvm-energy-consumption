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

check_and_configure_power_consumption_options() {
  # Simulate a harmless command that requires sudo
  # Note: sudo is needed to further run this script
  if ! sudo true; then
    echo "ERROR: Invalid sudo password. Unable to continue."
    exit 1
  fi

  if [[ $# -lt 1 || $# -gt 3 ]]; then
    echo "Usage: ./power-consumption-os-mac.sh --output-file=<output-file> [--duration=<duration>] [--background]"
    echo ""
    echo "Options:"
    echo "  --output-file=<output-file>  A mandatory parameter to specify the output file name."
    echo "  --duration=<duration>        An optional parameter to specify the duration in seconds."
    echo "  --background                 An optional parameter to specify if the command runs in the background (i.e., asynchronous) or not."
    echo ""
    echo "Note: If the duration is not specified, the command will run for 86400 seconds (i.e., 24 hours) or until interrupted."
    echo "      If --background is specified, the command will run in the background, and the shell prompt is immediately returned; otherwise, it will run in the foreground."
    echo ""
    echo "Examples:"
    echo "  ./power-consumption-os-mac.sh --output-file=power-consumption.txt"
    echo "  ./power-consumption-os-mac.sh --output-file=power-consumption.txt --duration=900"
    echo "  ./power-consumption-os-mac.sh --output-file=power-consumption.txt --duration=900 --background"
    echo ""
    return 1
  fi

  export POWER_CONSUMPTION_OUTPUT_FILE=""
  export POWER_CONSUMPTION_RUNNING_TIME="86400"
  export POWER_CONSUMPTION_BACKGROUND_MODE=""

  while [ $# -gt 0 ]; do
    case "$1" in
      --output-file=*)
        POWER_CONSUMPTION_OUTPUT_FILE="${1#*=}"
        ;;
      --duration=*)
        POWER_CONSUMPTION_RUNNING_TIME="${1#*=}"
        ;;
      --background)
        POWER_CONSUMPTION_BACKGROUND_MODE="&"
        ;;
      *)
        echo "ERROR: Unknown parameter: $1"
        return 1
        ;;
    esac
    shift
  done

  if [ -z "$POWER_CONSUMPTION_OUTPUT_FILE" ]; then
    echo "ERROR: Missing mandatory parameter output file."
    return 1
  fi

  if [ "$POWER_CONSUMPTION_RUNNING_TIME" -lt 60 ]; then
    echo "ERROR: Duration must be greater than 60 seconds."
    return 1
  fi
}

start_power_consumption_measurements() {
  power_command="sudo powermetrics -i 1000 \
      -n ${POWER_CONSUMPTION_RUNNING_TIME} -a ${POWER_CONSUMPTION_RUNNING_TIME} \
      --samplers cpu_power,gpu_power,thermal,battery,network,disk | \
      grep -E \"Sampled system activity|CPU Power|GPU Power|ANE Power|Combined Power|package power|Current pressure level|in:|out:|read:|write:|Battery:\" \
      > $POWER_CONSUMPTION_OUTPUT_FILE 2>&1 $POWER_CONSUMPTION_BACKGROUND_MODE"
  echo "$power_command"
  eval "$power_command"

  # This only returns with an error if the command itself failed to execute (e.g., it does not exist)
  # Note: Any errors encountered by the command while running in background mode will not be captured by this exit status
  if [ $? -ne 0 ]; then
    echo "ERROR: Power consumption measurements failed to be started. Check $POWER_CONSUMPTION_OUTPUT_FILE for details."
    return 1
  fi

  # In case of background (i.e., asynchronous) mode, export the PID of the running command (to be used later by subsequent programs)
  if [[ "$POWER_CONSUMPTION_BACKGROUND_MODE" == "&" ]]; then
    export POWER_CONSUMPTION_PID=$!
  fi
}

check_power_consumption_measurements() {
  # 1. In case of background (i.e., asynchronous) mode, check if the power consumption measurements successfully started
  if [[ "$POWER_CONSUMPTION_BACKGROUND_MODE" == "&" ]]; then
    # Sleep for a short duration to allow the asynchronous process to start
    sleep 3

    # Check if the asynchronous process is still running
    if ps -p "$POWER_CONSUMPTION_PID" > /dev/null; then
      echo "Power consumption measurements with PID $POWER_CONSUMPTION_PID started successfully and will run in background."
    else
      echo "ERROR: Power consumption measurements failed to be started. Check $POWER_CONSUMPTION_OUTPUT_FILE for details."
      return 1
    fi

  # 2. Otherwise (i.e., in blocking mode), the command has already been executed synchronously, and now display the termination message
  else
    echo "Power consumption measurements successfully finished at $(date)."
  fi
}

start_power_consumption() {
  # Power consumption measurements utilize the 'powermetrics' command to record the machine's overall energy consumption every second
  # throughout the entire test duration (e.g., $POWER_CONSUMPTION_RUNNING_TIME seconds), unless explicitly terminated.
  echo "Starting power consumption measurements at: $(date) ..."

  check_and_configure_power_consumption_options "$@" || return 1
  start_power_consumption_measurements || return 1
  check_power_consumption_measurements || return 1
}

stop_power_consumption() {
  if ps -p "$POWER_CONSUMPTION_PID" > /dev/null; then
    echo "Stopping the power consumption measurements with PID $POWER_CONSUMPTION_PID."
    sudo kill -s INT "$POWER_CONSUMPTION_PID"
    echo "Power consumption measurements with PID $POWER_CONSUMPTION_PID successfully stopped at $(date)."
  fi
}