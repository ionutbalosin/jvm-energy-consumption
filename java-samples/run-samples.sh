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
  if [[ ($EUID != 0) || ($# -ne 1 && $# -ne 2) ]]; then
    echo "Usage: sudo ./run-samples.sh <test-run-identifier> [--skip-build]"
    echo ""
    echo "Options:"
    echo "  test-run-identifier  is a mandatory parameter to identify the current execution test."
    echo "  --skip-build         is an optional parameter to skip the build process."
    echo ""
    echo "Examples:"
    echo "   $ sudo ./run-samples.sh 1"
    echo "   $ sudo ./run-samples.sh 1 --skip-build"
    echo ""
    return 1
  fi

  if [ "$1" ]; then
    export TEST_RUN_IDENTIFIER="$1"
  fi
}

configure_application() {
  export APP_HOME=$(pwd)/src/main/java
  export JAVA_OPS="-Xms1m -Xmx6g"
  export SAMPLE_APPS=("ThrowExceptionPatterns" "MemoryAccessPatterns" "JulLoggingPatterns")

  echo ""
  echo "Application home: $APP_HOME"
  for sample_name in "${SAMPLE_APPS[@]}"; do
    echo "Java sample: $sample_name"
  done
  echo "Java opts: $JAVA_OPS"
  echo "Test run identifier: $TEST_RUN_IDENTIFIER"

  echo ""
  read -r -p "If the above configuration is correct, press ENTER to continue or CRTL+C to abort ... "
}

create_output_resources() {
  for sample_name in "${SAMPLE_APPS[@]}"; do
    mkdir -p $OUTPUT_FOLDER/$sample_name/perf
    mkdir -p $OUTPUT_FOLDER/$sample_name/logs
  done
}

build_application() {
  cd $APP_HOME
  for sample_name in "${SAMPLE_APPS[@]}"; do
    echo "Compiling Java sample: $sample_name"
    javac $sample_name.java -d $APP_HOME/target
  done
  cd -

  if [ "$JVM_IDENTIFIER" == "native-image" ]; then
    cd $APP_HOME/target
    for sample_name in "${SAMPLE_APPS[@]}"; do
      echo "Building Java sample: $sample_name"
      native-image $sample_name -o $sample_name
    done
    cd -
  fi
}

start_sample() {
  sample_name="$1"
  sample_opts="$2"
  sample_run_identifier="$3"

  if [ "$JVM_IDENTIFIER" != "native-image" ]; then
    export RUN_CMD="$JAVA_HOME/bin/java $JAVA_OPS $APP_HOME/$sample_name.java $sample_opts"
  else
    export RUN_CMD="$APP_HOME/target/$sample_name $JAVA_OPS $sample_opts"
  fi

  echo "Starting $sample_name ($sample_opts) at: $(date) ... "
  sudo perf stat -a \
    -e "power/energy-cores/" \
    -e "power/energy-gpu/" \
    -e "power/energy-pkg/" \
    -e "power/energy-psys/" \
    -e "power/energy-ram/" \
    -o $OUTPUT_FOLDER/$sample_name/perf/$JVM_IDENTIFIER-run-$sample_run_identifier-$TEST_RUN_IDENTIFIER.stats \
    $RUN_CMD >  $OUTPUT_FOLDER/$sample_name/logs/$JVM_IDENTIFIER-run-$sample_run_identifier-$TEST_RUN_IDENTIFIER.log 2>&1
}

check_command_line_options "$@"
if [ $? -ne 0 ]; then
  exit 1
fi

echo ""
echo "+========================+"
echo "| [1/5] OS configuration |"
echo "+========================+"
. ../configure-os.sh

echo ""
echo "+=========================+"
echo "| [2/5] JVM configuration |"
echo "+=========================+"
. ../configure-jvm.sh

echo ""
echo "+==================================+"
echo "| [3/5] Java samples configuration |"
echo "+==================================+"
configure_application

# make sure the output resources (e.g., folders and files) exist
create_output_resources

echo ""
echo "+==============================+"
echo "| [4/5] Build the Java samples |"
echo "+==============================+"
if [ "$2" == "--skip-build" ]; then
  echo "WARNING: Skip building the application. A previously generated artifact will be used to start the application."
else
  build_application
fi

echo ""
echo "+==============================+"
echo "| [5/5] Start the Java samples |"
echo "+==============================+"
start_sample "ThrowExceptionPatterns" "const" "const"
start_sample "ThrowExceptionPatterns" "lambda" "lambda"
start_sample "ThrowExceptionPatterns" "new" "new"
start_sample "ThrowExceptionPatterns" "override_fist" "override_fist"

start_sample "MemoryAccessPatterns" "linear" "linear"
start_sample "MemoryAccessPatterns" "random_page" "random_page"
start_sample "MemoryAccessPatterns" "random_heap" "random_heap"

start_sample "JulLoggingPatterns" "string_format" "string_format"
start_sample "JulLoggingPatterns" "lambda_heap" "lambda_heap"
start_sample "JulLoggingPatterns" "lambda_local" "lambda_local"
start_sample "JulLoggingPatterns" "guarded_parametrized" "guarded_parametrized"
start_sample "JulLoggingPatterns" "guarded_unparametrized" "guarded_unparametrized"
start_sample "JulLoggingPatterns" "unguarded_parametrized" "unguarded_parametrized"
start_sample "JulLoggingPatterns" "unguarded_unparametrized" "unguarded_unparametrized"

# give a bit of time to the process to gracefully shut down
sleep 10

echo ""
echo "*** Test $TEST_RUN_IDENTIFIER successfully finished! ***"
