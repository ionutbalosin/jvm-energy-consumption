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

configure_jmeter() {
  export JMETER_HOME=/Users/wzhioba/Data/Workspaces/jmeter/apache-jmeter-5.5

  echo ""
  echo "JMeter home: $JMETER_HOME"
  echo "Test number: $TEST_RUN_NO"

  echo ""
  read -r -p "If the above configuration is correct, press ENTER to continue or CRTL+C to abort ... "
}

create_output_folders() {
  mkdir -p ${OUTPUT_FOLDER}/jtl
  mkdir -p ${OUTPUT_FOLDER}/jreports
  mkdir -p ${OUTPUT_FOLDER}/jplot
}

start_jmeter() {
  ${JMETER_HOME}/bin/jmeter -n -t test-plan.jmx -l ${OUTPUT_FOLDER}/jtl/${JVM_IDENTIFIER}-run${TEST_RUN_NO}.jtl
}

generate_jmeter_reports() {
  echo "Generate the aggregated report"
  ${JMETER_HOME}/bin/JMeterPluginsCMD.sh \
    --generate-csv ${OUTPUT_FOLDER}/jreports/${JVM_IDENTIFIER}-aggregate-report-run${TEST_RUN_NO}.csv \
    --input-jtl ${OUTPUT_FOLDER}/jtl/${JVM_IDENTIFIER}-run${TEST_RUN_NO}.jtl \
    --plugin-type AggregateReport

  echo ""
  echo "Generate the response times over time report"
  ${JMETER_HOME}/bin/JMeterPluginsCMD.sh \
    --generate-png ${OUTPUT_FOLDER}/jplot/${JVM_IDENTIFIER}-response-times-over-time-run${TEST_RUN_NO}.png \
    --input-jtl ${OUTPUT_FOLDER}/jtl/${JVM_IDENTIFIER}-run${TEST_RUN_NO}.jtl \
    --plugin-type ResponseTimesOverTime \
    --width 1280 --height 720

  echo ""
  echo "Generate the response times percentiles report"
  ${JMETER_HOME}/bin/JMeterPluginsCMD.sh \
    --generate-png ${OUTPUT_FOLDER}/jplot/${JVM_IDENTIFIER}-response-times-percentiles-run${TEST_RUN_NO}.png \
    --input-jtl ${OUTPUT_FOLDER}/jtl/${JVM_IDENTIFIER}-run${TEST_RUN_NO}.jtl \
    --plugin-type ResponseTimesPercentiles \
    --width 1280 --height 720

  echo ""
  echo "Generate the times vs threads report"
  ${JMETER_HOME}/bin/JMeterPluginsCMD.sh \
    --generate-png ${OUTPUT_FOLDER}/jplot/${JVM_IDENTIFIER}-times-vs-threads-run${TEST_RUN_NO}.png \
    --input-jtl ${OUTPUT_FOLDER}/jtl/${JVM_IDENTIFIER}-run${TEST_RUN_NO}.jtl \
    --plugin-type TimesVsThreads \
    --width 1280 --height 720
}

TEST_RUN_NO="$1"

echo ""
echo "+=========================+"
echo "| [1/4] JVM configuration |"
echo "+=========================+"
. ../configure-jvm.sh

echo ""
echo ""
echo "+============================+"
echo "| [2/4] JMeter configuration |"
echo "+============================+"
configure_jmeter

# make sure the output folders exist
create_output_folders

echo ""
echo "+====================+"
echo "| [3/4] Start JMeter |"
echo "+====================+"
start_jmeter

echo ""
echo "+===============================+"
echo "| [4/4] Generate JMeter reports |"
echo "+===============================+"
generate_jmeter_reports

echo ""
echo "*** Test $TEST_RUN_NO successfully finished! ***"