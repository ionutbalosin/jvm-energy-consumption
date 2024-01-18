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
  if [[ $EUID != 0 || $# -ne 1 ]]; then
    echo "Usage: sudo ./run-baseline.sh <test-run-identifier>"
    echo ""
    echo "Options:"
    echo "  test-run-identifier   A mandatory parameter to identify the current execution test."
    echo ""
    echo "Examples:"
    echo "   $ sudo ./run-baseline.sh 1"
    echo ""
    return 1
  fi

  export TEST_RUN_IDENTIFIER="$1"
}

configure_baseline() {
  export APP_RUNNING_TIME=900
  export OUTPUT_FOLDER=results/$ARCH/$OS
  export OUTPUT_FILE="$OUTPUT_FOLDER/perf/baseline-idle-os-run-$TEST_RUN_IDENTIFIER.stats"

  echo ""
  echo "Test run identifier: $TEST_RUN_IDENTIFIER"
  echo "Idle OS baseline running time: $APP_RUNNING_TIME sec"
  echo "Output folder: $OUTPUT_FOLDER"
  echo "Test output file identifier: $OUTPUT_FILE"

  echo ""
  read -r -p "If the above configuration is correct, press ENTER to continue or CRTL+C to abort ... "
}

create_output_resources() {
  mkdir -p $OUTPUT_FOLDER/perf
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

echo ""
echo "+======================================+"
echo "| [3/4] Idle OS baseline configuration |"
echo "+======================================+"
configure_baseline

# make sure the output resources (e.g., folders and files) exist
create_output_resources

echo ""
echo "+==================================+"
echo "| [4/4] Start the idle OS baseline |"
echo "+==================================+"
echo "Starting the idle OS baseline at: $(date) ... "
. ../scripts/shell/power-consumption-os-$OS.sh --duration="$APP_RUNNING_TIME" --output-file="$OUTPUT_FILE"
echo "Idle OS baseline successfully finished at: $(date)"

echo ""
echo "*** Test $TEST_RUN_IDENTIFIER successfully finished! ***"
