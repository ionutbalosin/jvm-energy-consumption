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
  export declare -a SAMPLE_APPS=("ThrowExceptionPatterns" "MemoryAccessPatterns" "JulLoggingPatterns")

  echo ""
  echo "Application home: $APP_HOME"
  for sample_app_name in "${SAMPLE_APPS[@]}"; do
    echo "Java sample: $sample_app_name"
  done
  echo "Java opts: $JAVA_OPS"
  echo "Test run identifier: $TEST_RUN_IDENTIFIER"

  echo ""
  read -r -p "If the above configuration is correct, press ENTER to continue or CRTL+C to abort ... "
}

create_output_resources() {
  mkdir -p $OUTPUT_FOLDER/perf
  mkdir -p $OUTPUT_FOLDER/logs
}

build_application() {
  if [ "$JVM_IDENTIFIER" == "native-image" ]; then
    cd $APP_HOME
    for sample_app_name in "${SAMPLE_APPS[@]}"; do
      echo "Building Java sample: $sample_app_name"
      javac $sample_app_name.java
      native-image $sample_app_name -o $sample_app_name
    done
    cd -
  fi
}

start_sample() {
  sample_app_name="$1"
  sample_app_ops="$2"
  sample_app_identifier="$3"

  if [ "$JVM_IDENTIFIER" != "native-image" ]; then
    export RUN_CMD="$JAVA_HOME/bin/java $JAVA_OPS $APP_HOME/$sample_app_name.java $sample_app_ops"
  else
    export RUN_CMD="$APP_HOME/$sample_app_name $JAVA_OPS $sample_app_ops"
  fi

  echo "Starting $sample_app_name ($sample_app_ops) at: $(date) ... "
  sudo perf stat -a \
    -e "power/energy-cores/" \
    -e "power/energy-gpu/" \
    -e "power/energy-pkg/" \
    -e "power/energy-psys/" \
    -e "power/energy-ram/" \
    -o $OUTPUT_FOLDER/perf/$JVM_IDENTIFIER-run-$sample_app_identifier-$TEST_RUN_IDENTIFIER.stats \
    $RUN_CMD >$OUTPUT_FOLDER/logs/$JVM_IDENTIFIER-run-$sample_app_identifier-$TEST_RUN_IDENTIFIER.log 2>&1
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
echo "+=================================+"
echo "| [3/5] Java samples configuration |"
echo "+=================================+"
configure_application

# make sure the output resources (e.g., folders and files) exist
create_output_resources

echo ""
echo "+=============================+"
echo "| [4/5] Build the Java samples |"
echo "+=============================+"
if [ "$2" == "--skip-build" ]; then
  echo "WARNING: Skip building the application. A previously generated artifact will be used to start the application."
else
  build_application
fi

echo ""
echo "+=============================+"
echo "| [5/5] Start the Java samples |"
echo "+=============================+"
start_sample "ThrowExceptionPatterns" "const 1024" "ThrowExceptionPatterns-const-1024"
start_sample "ThrowExceptionPatterns" "lambda 1024" "ThrowExceptionPatterns-lambda-1024"
start_sample "ThrowExceptionPatterns" "new 1024" "ThrowExceptionPatterns-new-1024"
start_sample "ThrowExceptionPatterns" "override_fist 1024" "ThrowExceptionPatterns-override_fist-1024"

start_sample "MemoryAccessPatterns" "linear" "MemoryAccessPatterns-linear"
start_sample "MemoryAccessPatterns" "random_page" "MemoryAccessPatterns-random_page"
start_sample "MemoryAccessPatterns" "random_heap" "MemoryAccessPatterns-random_heap"

start_sample "JulLoggingPatterns" "string_format" "JulLoggingPatterns-string_format"
start_sample "JulLoggingPatterns" "lambda_heap" "JulLoggingPatterns-lambda_heap"
start_sample "JulLoggingPatterns" "lambda_local" "JulLoggingPatterns-lambda_local"
start_sample "JulLoggingPatterns" "guarded_parametrized" "JulLoggingPatterns-guarded_parametrized"
start_sample "JulLoggingPatterns" "guarded_unparametrized" "JulLoggingPatterns-guarded_unparametrized"
start_sample "JulLoggingPatterns" "unguarded_parametrized" "JulLoggingPatterns-unguarded_parametrized"
start_sample "JulLoggingPatterns" "unguarded_unparametrized" "JulLoggingPatterns-unguarded_unparametrized"

# give a bit of time to the process to gracefully shut down
sleep 10

echo ""
echo "*** Test $TEST_RUN_IDENTIFIER successfully finished! ***"
