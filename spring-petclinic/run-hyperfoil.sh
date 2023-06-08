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

configure_hyperfoil() {
  export HYPERFOIL_HOME=/home/ionutbalosin/Kit/hyperfoil-0.24.2

  echo ""
  echo "Hyperfoil home: $HYPERFOIL_HOME"
  echo "Test number: $TEST_RUN_NO"

  echo ""
  read -r -p "If the above configuration is correct, press ENTER to continue or CRTL+C to abort ... "
}

create_output_folders() {
  mkdir -p ${OUTPUT_FOLDER}/reports
}

start_hyperfoil() {
  ${HYPERFOIL_HOME}/bin/cli.sh
}

TEST_RUN_NO="$1"

echo ""
echo "+=========================+"
echo "| [1/3] JVM configuration |"
echo "+=========================+"
. ../configure-jvm.sh

echo ""
echo "+===============================+"
echo "| [2/3] Hyperfoil configuration |"
echo "+===============================+"
configure_hyperfoil

# make sure the output folders exist
create_output_folders

echo ""
echo "+=======================+"
echo "| [3/3] Start Hyperfoil |"
echo "+=======================+"
echo "IMPORTANT: execute the below commands in the Hyperfoil CLI to trigger the load test, save the report, and exit the CLI at the end:"
echo "$ start-local && upload test-plan.hf.yaml && run test-plan-benchmark && report --destination=$(pwd)/${OUTPUT_FOLDER}/reports/${IDENTIFIER}-run${TEST_RUN_NO}.html"
echo "$ exit"
echo ""
start_hyperfoil

echo ""
echo "*** Test $TEST_RUN_NO successfully finished! ***"