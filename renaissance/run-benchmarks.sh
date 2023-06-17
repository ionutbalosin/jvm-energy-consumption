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

list_available_rapl_events() {
  echo ""
  echo "The available power events for the RAPL (Running Average Power Limit) energy consumption counters are:"
  perf list | grep power | grep "Kernel PMU event"
}

configure_openjdk() {
  export JAVA_HOME=/usr/lib/jvm/adoptium-temurin-jdk-17.0.7+7
  export JVM_NAME="openjdk"
}

configure_graalvm_ee() {
  export JAVA_HOME=/usr/lib/jvm/graalvm-ee-java17-22.3.2
  export JVM_NAME="graalvm-ee"
}

configure_graalvm_ce() {
  export JAVA_HOME=/usr/lib/jvm/graalvm-ce-java17-22.3.2
  export JVM_NAME="graalvm-ce"
}

configure_openj9() {
  export JAVA_HOME=/usr/lib/jvm/ibm-semeru-openj9-jdk-17.0.6+10
  export JVM_NAME="openj9"
}

configure_azul_prime() {
  export JAVA_HOME=/usr/lib/jvm/zing23.04.0.0-2-jdk17.0.7-linux_x64
  export JVM_NAME="azul-prime"
}

configure_renaissance() {
  export APP_HOME=/home/ionutbalosin/Workspace/renaissance/renaissance-gpl-0.14.2.jar
}

configure_environment() {
  # JDK related properties automatically generated
  export JDK_VERSION=$($JAVA_HOME/bin/java -XshowSettings:properties 2>&1 >/dev/null | grep 'java.specification.version' | awk '{split($0, array, "="); print array[2]}' | xargs echo -n)
  export PATH=$JAVA_HOME/bin:$PATH
  export JAVA_OPS="-Xms4g -Xmx4g"
  export JVM_IDENTIFIER=${JVM_NAME}-jdk${JDK_VERSION}
  export OUTPUT_FOLDER=results/jdk-$JDK_VERSION
  export BENCHMARK_REPETITIONS=100

  echo ""
  echo "Java home: $JAVA_HOME"
  echo "JVM identifier: $JVM_IDENTIFIER"
  echo "JDK version: $JDK_VERSION"
  echo "Java opts: $JAVA_OPS"
  echo "Application home: $APP_HOME"
  echo "Benchmark repetitions: $BENCHMARK_REPETITIONS"
  echo "Benchmark output folder: $OUTPUT_FOLDER"

  echo ""
  ${JAVA_HOME}/bin/java --version
}

create_output_folders() {
  mkdir -p ${OUTPUT_FOLDER}/perf
  mkdir -p ${OUTPUT_FOLDER}/logs
  mkdir -p ${OUTPUT_FOLDER}/reports
}

chmod_output_folders() {
  sudo chmod 777 ${OUTPUT_FOLDER}/perf/*
  sudo chmod 777 ${OUTPUT_FOLDER}/logs/*
  sudo chmod 777 ${OUTPUT_FOLDER}/reports/*
}

run_benchmarks(){
  # declare renaissance benchmark categories
  declare -a benchmark_categories=("concurrency" "functional" "scala" "web" "dummy")

  for benchmark_category in "${benchmark_categories[@]}"; do
    echo "Start benchmark category ${benchmark_category}"

    sudo perf stat -a \
      -e "power/energy-cores/" \
      -e "power/energy-gpu/" \
      -e "power/energy-pkg/" \
      -e "power/energy-psys/" \
      -e "power/energy-ram/" \
      -o ${OUTPUT_FOLDER}/perf/${JVM_IDENTIFIER}-${benchmark_category}.stats \
      ${JAVA_HOME}/bin/java ${JAVA_OPS} -jar ${APP_HOME} \
      -r $BENCHMARK_REPETITIONS \
      --csv ${OUTPUT_FOLDER}/reports/${JVM_IDENTIFIER}-${benchmark_category}.csv \
      ${benchmark_category} \
      > ${OUTPUT_FOLDER}/logs/${JVM_IDENTIFIER}-${benchmark_category}.log 2>&1

    # give a bit of time to the process to gracefully shut down
    sleep 10

    if [ $? -ne 0 ]; then
      echo ""
      echo "ERROR: Error encountered while running benchmark category ${benchmark_category}, unable to continue!"
      exit 1
    else
      echo "Benchmark category ${benchmark_category} successfully finished!"
    fi

  done
}

if [[ $EUID != 0 ]]; then
  echo "ERROR: sudo admin rights are needed (e.g., $ sudo ./run-benchmarks.sh)"
  exit 1
fi

echo ""
echo "+========================+"
echo "| Available power events |"
echo "+========================+"
list_available_rapl_events

# make sure the output folders exist
create_output_folders

echo ""
echo "+=============================+"
echo "| Run benchmarks with OpenJDK |"
echo "+=============================+"
configure_openjdk
configure_renaissance
configure_environment
run_benchmarks

echo ""
echo "+================================+"
echo "| Run benchmarks with GraalVM EE |"
echo "+================================+"
configure_graalvm_ee
configure_renaissance
configure_environment
run_benchmarks

echo ""
echo "+================================+"
echo "| Run benchmarks with GraalVM CE |"
echo "+================================+"
configure_graalvm_ce
configure_renaissance
configure_environment
run_benchmarks

echo ""
echo "+================================+"
echo "| Run benchmarks with Azul Prime |"
echo "+================================+"
configure_azul_prime
configure_renaissance
configure_environment
run_benchmarks

echo ""
echo "+============================+"
echo "| Run benchmarks with OpenJ9 |"
echo "+============================+"
configure_openj9
configure_renaissance
configure_environment
run_benchmarks

# assign read/write permissions to the output files
chmod_output_folders