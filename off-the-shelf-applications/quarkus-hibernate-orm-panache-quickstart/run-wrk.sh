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
  APP_RUN_IDENTIFIER=""
  APP_JVM_IDENTIFIER=""
  APP_RUNNING_TIME="5280"
  APP_BASE_URL="localhost:8080"
  APP_THREADS="$(( $(nproc) / 2 ))"
  APP_SESSIONS="256"

  if [[ $# -lt 1 || $# -gt 5 ]]; then
    echo "Usage: ./run-wrk.sh --run-identifier=<run-identifier> [--jvm-identifier=<jvm-identifier>] [--duration=<duration>] [--threads=<threads>] [--app-base-url=<app-base-url>]"
    echo ""
    echo "Options:"
    echo "  --run-identifier=<run-identifier>  A mandatory parameter to identify the current execution run."
    echo "  --jvm-identifier=<jvm-identifier>  An optional parameter to specify the target JVM where the application is running (for a match). Java is not needed to launch the test client. If not specified, the user will be prompted to select it at the beginning of the run."
    echo "                                     Accepted options: {openjdk-hotspot-vm, graalvm-ce, oracle-graalvm, native-image, azul-prime-vm, eclipse-openj9-vm}."
    echo "  --duration=<duration>              An optional parameter to specify the duration in seconds. If not specified, it is set by default to $APP_RUNNING_TIME seconds."
    echo "  --threads=<threads>                An optional parameter to specify the number of threads to use for wrk. If not specified, it is set by default to $APP_THREADS (i.e., half the number of available CPUs)."
    echo "  --app-base-url=<app-base-url>      An optional parameter to specify where the target JVM application runs. If not specified, it is set by default to $APP_BASE_URL"
    echo ""
    echo "Examples:"
    echo "  $ ./run-wrk.sh --run-identifier=1"
    echo "  $ ./run-wrk.sh --run-identifier=1 --jvm-identifier=openjdk-hotspot-vm"
    echo "  $ ./run-wrk.sh --run-identifier=1 --jvm-identifier=openjdk-hotspot-vm --duration=60"
    echo "  $ ./run-wrk.sh --run-identifier=1 --jvm-identifier=openjdk-hotspot-vm --duration=60 --threads=8 --app-base-url=192.168.0.2:8080"
    echo ""
    return 1
  fi

  while [ $# -gt 0 ]; do
    case "$1" in
      --run-identifier=*)
        APP_RUN_IDENTIFIER="${1#*=}"
        ;;
      --jvm-identifier=*)
        APP_JVM_IDENTIFIER="${1#*=}"
        ;;
      --duration=*)
        APP_RUNNING_TIME="${1#*=}"
        ;;
      --threads=*)
        APP_THREADS="${1#*=}"
        ;;
      --app-base-url=*)
        APP_BASE_URL="${1#*=}"
        ;;
      *)
        echo "ERROR: Unknown parameter $1"
        return 1
        ;;
    esac
    shift
  done

  if [ -z "$APP_RUN_IDENTIFIER" ]; then
    echo "ERROR: Missing mandatory parameter run identifier."
    return 1
  fi
}

configure_wrk() {
  CURR_DIR=$(pwd)

  echo "Application run identifier: $APP_RUN_IDENTIFIER"
  echo "Application running time: $APP_RUNNING_TIME sec"
  echo "Application sessions: $APP_SESSIONS"
  echo "Application threads: $APP_THREADS"
  echo "JVM identifier: $APP_JVM_IDENTIFIER"
  echo "Application base url: $APP_BASE_URL"

  if ! command -v wrk &> /dev/null; then
    echo ""
    echo "ERROR: Unable to execute the 'wrk' command. Cannot proceed!"
    return 1
  fi
}

create_output_resources() {
  mkdir -p $OUTPUT_FOLDER/wrk
}

start_wrk() {
  echo "Starting wrk at: $(date) ..."
  echo "Please enjoy a coffee ☕ while the application runs. This may take approximately $APP_RUNNING_TIME seconds ..."
  echo ""

  output_file="$CURR_DIR/$OUTPUT_FOLDER/wrk/$JVM_IDENTIFIER-run-$APP_RUN_IDENTIFIER.txt"
  run_command="wrk -t${APP_THREADS} -c${APP_SESSIONS} -d${APP_RUNNING_TIME}s -s test-plan.lua --latency http://$APP_BASE_URL | tee $output_file"

  echo "$run_command"
  eval "$run_command"
}

check_command_line_options "$@"
if [ $? -ne 0 ]; then
  exit 1
fi

echo ""
echo "+================================+"
echo "| [1/6] Configuration Properties |"
echo "+================================+"
. ../../scripts/shell/configure-properties.sh || exit 1

echo ""
echo "+=============================+"
echo "| [2/6] Hardware Architecture |"
echo "+=============================+"
. ../../scripts/shell/configure-arch.sh

echo ""
echo "+========================+"
echo "| [3/6] OS Configuration |"
echo "+========================+"
. ../../scripts/shell/configure-os.sh || exit 1

echo ""
echo "+=========================+"
echo "| [4/6] JVM Configuration |"
echo "+=========================+"
. ../../scripts/shell/configure-jvm.sh "$APP_JVM_IDENTIFIER" || exit 1

echo ""
echo "+=========================+"
echo "| [5/6] wrk configuration |"
echo "+=========================+"
configure_wrk || exit 1

# make sure the output resources (e.g., folders and files) exist
create_output_resources

echo ""
echo "+=================+"
echo "| [6/6] Start wrk |"
echo "+=================+"
start_wrk

echo ""
echo "Bye bye! 👋"