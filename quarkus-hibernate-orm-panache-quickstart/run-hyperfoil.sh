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
  export HYPERFOIL_HOME=/Users/wzhioba/Data/Workspaces/hyperfoil-0.24.2

  #  The Hyperfoil needs the 'java' command to be configured
  if [[ (-n "$JAVA_HOME") && (-x "$JAVA_HOME/bin/java") ]]
  then
    export PATH=$JAVA_HOME/bin:$PATH
  else
    echo ""
    echo "ERROR: Cannot properly execute '$JAVA_HOME/bin/java' command, unable to continue!"
    echo "'java' command is needed for Hyperfoil."
    exit 1
  fi
}

configure_environment() {
  export JDK_VERSION=17
  export OUTPUT_FOLDER="$(pwd)/results/jdk-$JDK_VERSION"

  echo ""
  echo "Hyperfoil home: $HYPERFOIL_HOME"
  echo "Test number: $TEST_RUN_NO"
  echo "Test output folder: $OUTPUT_FOLDER"

  echo ""
  read -r -p "If the above configuration is correct, press ENTER to continue or CRTL+C to abort ... "
}

start_hyperfoil() {
  ${HYPERFOIL_HOME}/bin/cli.sh
}

TEST_RUN_NO="$1"

echo ""
echo "+=======================+"
echo "| Environment variables |"
echo "+=======================+"
configure_hyperfoil
configure_environment

echo ""
echo "+-----------------+"
echo "| Start Hyperfoil |"
echo "+-----------------+"
echo "Note: After Hyperfoil starts, please execute below command in the CLI:"
echo "$ start-local && upload test-plan.hf.yaml && run test-plan-benchmark && report --destination=$OUTPUT_FOLDER/report-run$TEST_RUN_NO.html"
start_hyperfoil

echo ""
echo "*** Test $TEST_RUN_NO successfully finished! ***"