#!/bin/bash
#
# JVM Energy Consumption
#
# MIT License
#
# Copyright (c) 2023 Ionut Balosin
# Copyright (c) 2023 Ko Turk
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
  if [ $# -ne 1 ] || [[ $EUID != 0 ]]; then
    echo "Usage: sudo ./run-baseline.sh <test-run-identifier>"
    echo ""
    echo "Options:"
    echo "  test-run-identifier  is a mandatory parameter to identify the current execution test."
    echo ""
    echo "Examples:"
    echo "   $ ./run-baseline.sh 1"
    echo ""
    return 1
  fi

  if [ "$1" ]; then
    export TEST_RUN_IDENTIFIER="$1"
  fi
}

configure_baseline() {
  export RUNNING_TIME=900
  export OUTPUT_FOLDER=results/$ARCH

  echo ""
  echo "Output folder: $OUTPUT_FOLDER"
  echo "Idle OS baseline running time: $RUNNING_TIME sec"
  echo "Test run identifier: $TEST_RUN_IDENTIFIER"

  echo ""
  read -r -p "If the above configuration is correct, press ENTER to continue or CRTL+C to abort ... "
}

create_output_resources() {
  mkdir -p $OUTPUT_FOLDER/perf
}

chmod_output_resources() {
  sudo chmod 777 $OUTPUT_FOLDER/*
  sudo chmod -R a+rwx $OUTPUT_FOLDER

  sudo chmod 777 $OUTPUT_FOLDER/perf/*
  sudo chmod -R a+rwx $OUTPUT_FOLDER/perf
}

start_baseline() {
  export RUN_CMD="sleep $RUNNING_TIME"

  echo ""
  echo "Command line: $RUN_CMD"

  echo ""
  read -r -p "If the above configuration is correct, press ENTER to continue or CRTL+C to abort ... "

  echo "Starting the idle OS baseline at: $(date) ... "
  sudo perf stat -a \
    -e "power/energy-cores/" \
    -e "power/energy-gpu/" \
    -e "power/energy-pkg/" \
    -e "power/energy-psys/" \
    -e "power/energy-ram/" \
    -o $OUTPUT_FOLDER/perf/idle-os-baseline-run-$TEST_RUN_IDENTIFIER.stats \
    $RUN_CMD
}

check_command_line_options "$@"
if [ $? -ne 0 ]; then
  exit 1
fi

echo ""
echo "+========================+"
echo "| [1/3] OS configuration |"
echo "+========================+"
. ../configure-os.sh

echo ""
echo "+======================================+"
echo "| [2/3] Idle OS baseline configuration |"
echo "+======================================+"
configure_baseline

# make sure the output resources (e.g., folders and files) exist
create_output_resources

echo ""
echo "+==================================+"
echo "| [3/3] Start the idle OS baseline |"
echo "+==================================+"
start_baseline

echo "Idle OS baseline successfully stopped at: $(date)"

# assign read/write permissions to the output files
chmod_output_resources

echo ""
echo "*** Test $TEST_RUN_IDENTIFIER successfully finished! ***"