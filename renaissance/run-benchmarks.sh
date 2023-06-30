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

configure_benchmark() {
  export BENCHMARK_HOME=/home/ionutbalosin/Workspace/renaissance/renaissance-gpl-0.14.2.jar
  export BENCHMARK_REPETITIONS=100
  export JAVA_OPS="-Xms1m -Xmx1g"

  echo ""
  echo "Benchmark home: $BENCHMARK_HOME"
  echo "Benchmark repetitions: $BENCHMARK_REPETITIONS"
  echo "Java opts: $JAVA_OPS"

  echo ""
  read -r -p "If the above configuration is correct, press ENTER to continue or CRTL+C to abort ... "
}

create_output_folders() {
  mkdir -p $OUTPUT_FOLDER/perf
  mkdir -p $OUTPUT_FOLDER/logs
  mkdir -p $OUTPUT_FOLDER/reports
}

run_benchmarks(){
  # declare renaissance benchmark categories
  declare -a benchmark_categories=("concurrency" "functional" "scala" "web" "dummy")

  for benchmark_category in "${benchmark_categories[@]}"; do
    echo "Start benchmark category $benchmark_category"

    sudo perf stat -a \
      -e "power/energy-cores/" \
      -e "power/energy-gpu/" \
      -e "power/energy-pkg/" \
      -e "power/energy-psys/" \
      -e "power/energy-ram/" \
      -o $OUTPUT_FOLDER/perf/$JVM_IDENTIFIER-$benchmark_category.stats \
      $JAVA_HOME/bin/java $JAVA_OPS -jar $BENCHMARK_HOME \
      -r $BENCHMARK_REPETITIONS \
      --csv $OUTPUT_FOLDER/reports/$JVM_IDENTIFIER-$benchmark_category.csv \
      $benchmark_category \
      > $OUTPUT_FOLDER/logs/$JVM_IDENTIFIER-$benchmark_category.log 2>&1

    # give a bit of time to the process to gracefully shut down
    sleep 10

    if [ $? -ne 0 ]; then
      echo ""
      echo "ERROR: Error encountered while running benchmark category $benchmark_category, unable to continue!"
      exit 1
    else
      echo "Benchmark category $benchmark_category successfully finished!"
    fi

  done
}

if [[ $EUID != 0 ]]; then
  echo "ERROR: sudo admin rights are needed (e.g., $ sudo ./run-benchmarks.sh)"
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
configure_application

# make sure the output resources (e.g., folders and files) exist
create_output_resources

echo ""
echo "+======================+"
echo "| [4/4] Run benchmarks |"
echo "+======================+"
run_benchmarks