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
  if [[ $# -lt 1 || $# -gt 2 ]]; then
    echo "Usage: ./run-baseline.sh --run-identifier=<run-identifier> [--duration=<duration>]"
    echo ""
    echo "Options:"
    echo "  --run-identifier=<run-identifier>  A mandatory parameter to identify the current execution run(s). It can be a single value or a comma-separated list for multiple runs."
    echo "  --duration=<duration>              An optional parameter to specify the duration in seconds. If not specified, it is set by default to 1800 seconds."
    echo ""
    echo "Examples:"
    echo "   $ ./run-baseline.sh --run-identifier=1"
    echo "   $ ./run-baseline.sh --run-identifier=1,2"
    echo "   $ ./run-baseline.sh --run-identifier=1,2,3 --duration=3600"
    echo ""
    return 1
  fi

  APP_RUNNING_TIME="1800"
  APP_RUN_IDENTIFIER=""

  while [ $# -gt 0 ]; do
    case "$1" in
      --duration=*)
        APP_RUNNING_TIME="${1#*=}"
        ;;
      --run-identifier=*)
        APP_RUN_IDENTIFIER="${1#*=}"
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

configure_baseline() {
  OUTPUT_FOLDER=results/$ARCH/$OS

  read -ra APP_RUN_IDENTIFIERS <<< "$(tr ',' ' ' <<< "$APP_RUN_IDENTIFIER")"
  echo "Run identifier(s): ${APP_RUN_IDENTIFIERS[@]}"
  echo "Idle OS baseline running time: $APP_RUNNING_TIME sec"
  echo "Output folder: $OUTPUT_FOLDER"
}

create_output_resources() {
  mkdir -p "$OUTPUT_FOLDER/power"
}

check_command_line_options "$@"
if [ $? -ne 0 ]; then
  exit 1
fi

echo ""
echo "+=============================+"
echo "| [1/4] Hardware Architecture |"
echo "+=============================+"
. ../scripts/shell/configure-arch.sh

echo ""
echo "+========================+"
echo "| [2/4] OS Configuration |"
echo "+========================+"
. ../scripts/shell/configure-os.sh || exit 1
. ../scripts/shell/system-power-consumption-os-"$OS".sh

echo ""
echo "+======================================+"
echo "| [3/4] Idle OS baseline configuration |"
echo "+======================================+"
configure_baseline

# make sure the output resources (e.g., folders and files) exist
create_output_resources

base_run_counter=1
base_run_limit="${#APP_RUN_IDENTIFIERS[@]}"
for base_run_identifier in "${APP_RUN_IDENTIFIERS[@]}"; do
  RUN_IDENTIFIER="$base_run_identifier"

  echo ""
  echo "+============================================================+"
  echo "| [4/4][$base_run_counter/$base_run_limit] Start the system power consumption measurements |"
  echo "+============================================================+"
  power_output_file="$OUTPUT_FOLDER/power/baseline-idle-os-run-$RUN_IDENTIFIER.txt"
  start_system_power_consumption --duration="$APP_RUNNING_TIME" --output-file="$power_output_file" || exit 1

  ((base_run_counter++))
done

echo "Everything went well, bye bye! 👋"