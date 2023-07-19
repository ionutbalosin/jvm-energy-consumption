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
  if [[ $EUID != 0 || $# -ne 1 ]]; then
    echo "Usage: sudo ./run-benchmarks.sh <test-run-identifier>"
    echo ""
    echo "Options:"
    echo "  test-run-identifier  is a mandatory parameter to identify the current execution test."
    echo ""
    echo "Examples:"
    echo "   $ sudo ./run-benchmarks.sh 1"
    echo ""
    return 1
  fi

  if [ "$1" ]; then
    export TEST_RUN_IDENTIFIER="$1"
  fi
}

configure_benchmark() {
  export BENCHMARK_HOME=/home/ionutbalosin/Workspace/renaissance/renaissance-gpl-0.14.2.jar
  export BENCHMARK_CATEGORIES=("concurrency" "functional" "scala" "web")
  export BENCHMARK_REPETITIONS=100
  export JAVA_OPS="-Xms1m -Xmx2g"

  echo ""
  echo "Benchmark home: $BENCHMARK_HOME"
  echo "Benchmark categories:"
  for benchmark_category in "${BENCHMARK_CATEGORIES[@]}"; do
    echo "  - $benchmark_category"
  done
  echo "Benchmark repetitions: $BENCHMARK_REPETITIONS"
  echo "Java opts: $JAVA_OPS"
  echo "Test run identifier: $TEST_RUN_IDENTIFIER"

  echo ""
  read -r -p "If the above configuration is correct, press ENTER to continue or CRTL+C to abort ... "
}

create_output_resources() {
  for benchmark_category in "${BENCHMARK_CATEGORIES[@]}"; do
    mkdir -p $OUTPUT_FOLDER/$benchmark_category/perf
    mkdir -p $OUTPUT_FOLDER/$benchmark_category/logs
    mkdir -p $OUTPUT_FOLDER/$benchmark_category/reports
  done
}

run_benchmarks() {
  for benchmark_category in "${BENCHMARK_CATEGORIES[@]}"; do
    echo "Starting benchmark category $benchmark_category at: $(date) "

    sudo perf stat -a \
      -e "power/energy-cores/" \
      -e "power/energy-gpu/" \
      -e "power/energy-pkg/" \
      -e "power/energy-psys/" \
      -e "power/energy-ram/" \
      -o $OUTPUT_FOLDER/$benchmark_category/perf/$JVM_IDENTIFIER-run-$TEST_RUN_IDENTIFIER.stats \
      $JAVA_HOME/bin/java $JAVA_OPS -jar $BENCHMARK_HOME \
      -r $BENCHMARK_REPETITIONS \
      --csv $OUTPUT_FOLDER/$benchmark_category/reports/$JVM_IDENTIFIER-run-$TEST_RUN_IDENTIFIER.csv \
      $benchmark_category \
      >$OUTPUT_FOLDER/$benchmark_category/logs/$JVM_IDENTIFIER-run-$TEST_RUN_IDENTIFIER.log 2>&1

    # give a bit of time to the process to gracefully shut down
    sleep 10

    if [ $? -ne 0 ]; then
      echo ""
      echo "ERROR: Error encountered while running benchmark category $benchmark_category, unable to continue!"
      exit 1
    else
      echo "Benchmark category $benchmark_category successfully finished at: $(date)"
    fi

  done
}

check_command_line_options "$@"
if [ $? -ne 0 ]; then
  exit 1
fi

echo ""
echo "+========================+"
echo "| [1/4] OS configuration |"
echo "+========================+"
. ../configure-os.sh

echo ""
echo "+=========================+"
echo "| [2/4] JVM configuration |"
echo "+=========================+"
. ../configure-jvm.sh

echo ""
echo "+===============================+"
echo "| [3/4] Benchmark configuration |"
echo "+===============================+"
configure_benchmark

# make sure the output resources (e.g., folders and files) exist
create_output_resources

echo ""
echo "+======================+"
echo "| [4/4] Run benchmarks |"
echo "+======================+"
run_benchmarks

# give a bit of time to the process to gracefully shut down
sleep 10

echo ""
echo "*** Test $TEST_RUN_IDENTIFIER successfully finished! ***"
