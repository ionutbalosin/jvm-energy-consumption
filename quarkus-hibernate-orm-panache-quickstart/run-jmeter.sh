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

configure_openjdk() {
  export JVM_NAME="openjdk"
}

configure_graalvm_ee() {
  export JVM_NAME="graalvm-ee"
}

configure_graalvm_ce() {
  export JVM_NAME="graalvm-ce"
}

configure_native_image() {
  export JVM_NAME="native-image"
}

configure_zing() {
  export JVM_NAME="zing"
}

configure_openj9() {
  export JVM_NAME="openj9"
}

select_jvm() {
  echo "Select the JVM:"
  echo "    1) - OpenJDK"
  echo "    2) - GraalVM CE"
  echo "    3) - GraalVM EE"
  echo "    4) - Native Image"
  echo "    5) - Azul Zing/Prime"
  echo "    6) - OpenJ9"
  echo ""

  while :; do
    read -r INPUT_KEY
    case $INPUT_KEY in
    1)
      configure_openjdk
      break
      ;;
    2)
      configure_graalvm_ce
      break
      ;;
    3)
      configure_graalvm_ee
      break
      ;;
    4)
      configure_native_image
      break
      ;;
    5)
      configure_zing
      break
      ;;
    6)
      configure_openj9
      break
      ;;
    *)
      echo "Sorry, I don't understand. Try again!"
      ;;
    esac
  done
}

configure_jmeter() {
  export JMETER_HOME=/Users/wzhioba/Data/Workspaces/jmeter/apache-jmeter-5.5

  #  The JMeter plugins need the 'java' command to be configured
  if [[ (-n "$JAVA_HOME") && (-x "$JAVA_HOME/bin/java") ]]
  then
    export PATH=$JAVA_HOME/bin:$PATH
  else
    echo ""
    echo "ERROR: Cannot properly execute '$JAVA_HOME/bin/java' command, unable to continue!"
    echo "'java' command is needed for JMeter plugins."
    exit 1
  fi
}

configure_environment() {
  export JDK_VERSION=17
  export JVM_IDENTIFIER=$JVM_NAME-jdk$JDK_VERSION
  export OUTPUT_FOLDER=results/jdk-$JDK_VERSION

  echo ""
  echo "JVM identifier: $JVM_IDENTIFIER"
  echo "JMeter home: $JMETER_HOME"
  echo "Test number: $TEST_RUN_NO"
  echo "Test output folder: $OUTPUT_FOLDER"

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
echo "+=======================+"
echo "| Environment variables |"
echo "+=======================+"
select_jvm
configure_jmeter
configure_environment

# make sure the output folders exist
create_output_folders

echo ""
echo "+--------------+"
echo "| Start JMeter |"
echo "+--------------+"
start_jmeter

echo ""
echo "+-------------------------+"
echo "| Generate JMeter reports |"
echo "+-------------------------+"
generate_jmeter_reports

echo ""
echo "*** Test $TEST_RUN_NO successfully finished! ***"