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
  APP_JVM_IDENTIFIERS=("openjdk-hotspot-vm" "graalvm-ce" "oracle-graalvm" "native-image" "azul-prime-vm" "eclipse-openj9-vm")
  APP_JDK_VERSION="21"
  APP_JVM_IDENTIFIER=""
  APP_RUN_IDENTIFIER="default"
  APP_BASE_URL="localhost:8080"
  WRK_RUNNING_TIME="7080"
  WRK_THREADS="$(nproc)"
  WRK_SESSIONS="900"

  if [[ $# -lt 1 || $# -gt 6 ]]; then
    echo "Usage: ./run-wrk.sh --jvm-identifier=<jvm-identifier> [--run-identifier=<run-identifier>] [--jdk-version=<jdk-version>] [--app-base-url=<app-base-url>] [--wrk-duration=<wrk-duration>] [--wrk-threads=<wrk-threads>]"
    echo ""
    echo "Options:"
    echo "  --jvm-identifier=<jvm-identifier>  A mandatory parameter that should match the target JVM where the application is running for test correlation."
    echo "                                     Accepted options: {${APP_JVM_IDENTIFIERS[*]}}."
    echo "  --run-identifier=<run-identifier>  An optional parameter to identify the current execution run. This should match the target JVM execution run for test correlation. If not specified, it defaults to the value 'default'."
    echo "  --jdk-version=<jdk-version>        An optional parameter to specify the target JDK version where the application is running for test correlation. If not specified, it defaults to $APP_JDK_VERSION."
    echo "  --app-base-url=<app-base-url>      An optional parameter to specify where the target JVM application runs. If not specified, it is set by default to $APP_BASE_URL"
    echo "  --wrk-duration=<wrk-duration>      An optional parameter to specify the wrk duration in seconds. If not specified, it is set by default to $WRK_RUNNING_TIME seconds."
    echo "  --wrk-threads=<wrk-threads>        An optional parameter to specify the number of threads to use for wrk. If not specified, it is set by default to $WRK_THREADS (i.e., half the number of available CPUs)."
    echo ""
    echo "Examples:"
    echo "  $ ./run-wrk.sh --run-identifier=1 --jvm-identifier=openjdk-hotspot-vm"
    echo "  $ ./run-wrk.sh --run-identifier=1 --jvm-identifier=openjdk-hotspot-vm --app-base-url=192.168.0.2:8080"
    echo "  $ ./run-wrk.sh --run-identifier=1 --jvm-identifier=openjdk-hotspot-vm --app-base-url=192.168.0.2:8080 --jdk-version=21 --wrk-duration=60 --wrk-threads=4"
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
      --jdk-version=*)
        APP_JDK_VERSION="${1#*=}"
        ;;
      --app-base-url=*)
        APP_BASE_URL="${1#*=}"
        ;;
      --wrk-duration=*)
        WRK_RUNNING_TIME="${1#*=}"
        ;;
      --wrk-threads=*)
        WRK_THREADS="${1#*=}"
        ;;
      *)
        echo "ERROR: Unknown parameter $1"
        return 1
        ;;
    esac
    shift
  done

  if [ -z "$APP_JVM_IDENTIFIER" ]; then
    echo "ERROR: Missing mandatory parameter jvm identifier."
    return 1
  fi

  if [[ ! " ${APP_JVM_IDENTIFIERS[@]} " =~ " $APP_JVM_IDENTIFIER " ]]; then
    echo "ERROR: Invalid parameter jvm identifier '$APP_JVM_IDENTIFIER'. Accepted options:  {${APP_JVM_IDENTIFIERS[*]}}"
    return 1
  fi
}

configure_wrk() {
  CURR_DIR=$(pwd)
  OUTPUT_FOLDER=results/jdk-$APP_JDK_VERSION/$ARCH/$OS

  echo "Application run identifier (on the target machine): $APP_RUN_IDENTIFIER"
  echo "Application base url (on the target machine): $APP_BASE_URL"
  echo "Application JDK version (on the target machine): $APP_JDK_VERSION"
  echo "Application JVM identifier (on the target machine): $APP_JVM_IDENTIFIER"
  echo "Output folder: $OUTPUT_FOLDER"
  echo "wrk running time: $WRK_RUNNING_TIME sec"
  echo "wrk threads: $WRK_THREADS"
  echo "wrk sessions: $WRK_SESSIONS"

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
  echo "Please enjoy a ☕ while the application runs. This may take approximately $WRK_RUNNING_TIME seconds ..."
  echo ""

  output_file="$CURR_DIR/$OUTPUT_FOLDER/wrk/$APP_JVM_IDENTIFIER-run-$APP_RUN_IDENTIFIER.txt"
  wrk_command="wrk -t${WRK_THREADS} -c${WRK_SESSIONS} -d${WRK_RUNNING_TIME}s -s test-plan.lua http://$APP_BASE_URL | tee $output_file"

  echo "$wrk_command"
  eval "$wrk_command"
}

check_command_line_options "$@"
if [ $? -ne 0 ]; then
  exit 1
fi

echo ""
echo "+=============================+"
echo "| [1/4] Hardware Architecture |"
echo "+=============================+"
. ../../scripts/shell/configure-arch.sh

echo ""
echo "+========================+"
echo "| [2/4] OS Configuration |"
echo "+========================+"
. ../../scripts/shell/configure-os.sh || exit 1

echo ""
echo "+=========================+"
echo "| [3/4] wrk configuration |"
echo "+=========================+"
configure_wrk || exit 1

# make sure the output resources (e.g., folders and files) exist
create_output_resources

echo ""
echo "+=================+"
echo "| [4/4] Start wrk |"
echo "+=================+"
start_wrk

echo ""
echo "Bye bye! 👋"