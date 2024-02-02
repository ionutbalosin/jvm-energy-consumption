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

check_and_configure_process_performance_monitoring_options() {
  if [[ ($# -lt 1 || $# -gt 3) ]]; then
    echo "Usage: ./process-performance-monitoring-os-linux.sh --pid=<pid> --output-file=<output-file> [--duration=<duration>]"
    echo ""
    echo "Options:"
    echo "  --pid=<pid>            A mandatory parameter to specify the process ID to be monitored. This needs to be an existing running process."
    echo "  --output-file=<file>   A mandatory parameter to specify the output file name."
    echo "  --duration=<duration>  An optional parameter to specify the duration in seconds."
    echo ""
    echo "Note: If the duration is not specified, the command will run for 86400 seconds (i.e., 24 hours) or until interrupted."
    echo ""
    echo "Examples:"
    echo "  ./process-performance-monitoring-os-linux.sh --pid=123 --output-file=process-status.txt"
    echo "  ./process-performance-monitoring-os-linux.sh --pid=123 --output-file=process-status.txt --duration=900 "
    echo ""
    return 1
  fi

  export PERFORMANCE_MONITOR_TARGET_PID=""
  export PERFORMANCE_MONITOR_OUTPUT_FILE=""
  export PERFORMANCE_MONITOR_RUNNING_TIME="86400"

  while [ $# -gt 0 ]; do
    case "$1" in
      --pid=*)
        PERFORMANCE_MONITOR_TARGET_PID="${1#*=}"
        ;;
      --output-file=*)
        PERFORMANCE_MONITOR_OUTPUT_FILE="${1#*=}"
        ;;
      --duration=*)
        PERFORMANCE_MONITOR_RUNNING_TIME="${1#*=}"
        ;;
      *)
        echo "ERROR: Unknown parameter: $1"
        return 1
        ;;
    esac
    shift
  done

  if [ -z "$PERFORMANCE_MONITOR_TARGET_PID" ]; then
    echo "ERROR: Missing mandatory parameter pid."
    return 1
  fi

  if [ -z "$PERFORMANCE_MONITOR_OUTPUT_FILE" ]; then
    echo "ERROR: Missing mandatory parameter output file."
    return 1
  fi
}

start_process_performance_monitoring_measurements() {
  # Clear the output file to make sure it does not contain any previous stats
  echo -n > "$PERFORMANCE_MONITOR_OUTPUT_FILE"

  # Run the process performance monitoring in the background using a subshell
  (
    for ((i = 1; i <= $PERFORMANCE_MONITOR_RUNNING_TIME; i++)); do
      if ! ps -p "$PERFORMANCE_MONITOR_TARGET_PID" > /dev/null; then
        echo "WARNING: Process with PID $PERFORMANCE_MONITOR_TARGET_PID no longer exists. Stopping the process performance monitoring." >> "$PERFORMANCE_MONITOR_OUTPUT_FILE"
        exit 1
      fi

      if ((i == 1)); then
        SHOW_HEADERS=""
      else
        SHOW_HEADERS="--no-headers"
      fi

      ps -p "$PERFORMANCE_MONITOR_TARGET_PID" -o pid,etime,cputime,pcpu,pmem,rss,vsz,pss,user,comm,s "$SHOW_HEADERS" >> "$PERFORMANCE_MONITOR_OUTPUT_FILE"
      sleep 1
    done
    
    echo "Process performance monitoring successfully finished." >> "$PERFORMANCE_MONITOR_OUTPUT_FILE"
  ) &

  export PERFORMANCE_MONITOR_PID=$!
}

check_process_performance_monitoring_measurements() {
  # Sleep for a short duration to allow the asynchronous process to start
  sleep 3

  # Check if the asynchronous process is still running
  if ps -p "$PERFORMANCE_MONITOR_PID" > /dev/null; then
    echo "Process performance monitoring with PID $PERFORMANCE_MONITOR_PID started successfully and will run in background."
  else
    echo "ERROR: Process performance monitoring failed to be started. Check $PERFORMANCE_MONITOR_OUTPUT_FILE for details."
    return 1
  fi
}

start_process_performance_monitoring() {
  # Process performance monitoring utilizes the 'ps' command to record the process CPU and memory stats every second
  # throughout the entire test duration (e.g., $PERFORMANCE_MONITOR_RUNNING_TIME seconds), unless explicitly terminated.
  echo "Starting process performance monitoring at: $(date) ..."

  check_and_configure_process_performance_monitoring_options "$@" || exit 1
  start_process_performance_monitoring_measurements || exit 1
  check_process_performance_monitoring_measurements || exit 1
}

stop_process_performance_monitoring() {
  if ps -p "$PERFORMANCE_MONITOR_PID" > /dev/null; then
    echo "Stopping the process performance monitoring with PID $PERFORMANCE_MONITOR_PID."
    kill -s INT "$PERFORMANCE_MONITOR_PID"
    echo "Process performance monitoring with PID $PERFORMANCE_MONITOR_PID successfully stopped at $(date)."
  else
    echo "Process performance monitoring with PID $PERFORMANCE_MONITOR_PID no longer exists (it has likely already finished)."
  fi
}
