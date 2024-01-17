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

configure_samples() {
  export APP_HOME=$(pwd)
  export JAVA_OPS="-Xms1m -Xmx6g"
  export SAMPLE_APPS=(
    "ThrowExceptionPatterns"
    "MemoryAccessPatterns"
    "LoggingPatterns"
    "SortingAlgorithms"
    "VirtualCalls"
  )

  echo ""
  echo "Application home: $APP_HOME"
  echo "Java samples:"
  for sample_name in "${SAMPLE_APPS[@]}"; do
    echo "  - $sample_name"
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

build_samples() {
  for sample_name in "${SAMPLE_APPS[@]}"; do
    build_output_file="$APP_HOME/$OUTPUT_FOLDER/$sample_name/logs/$JVM_IDENTIFIER-build-$TEST_RUN_IDENTIFIER.log"
    stats_output_file="$APP_HOME/$OUTPUT_FOLDER/$sample_name/perf/$JVM_IDENTIFIER-build-$TEST_RUN_IDENTIFIER"
    PREFIX_COMMAND="${OS_PREFIX_COMMAND/((statsOutputFile))/$stats_output_file}"

    if [ "$JVM_IDENTIFIER" != "native-image" ]; then
      BUILD_CMD="$APP_HOME/../mvnw clean package -DmainClass=\"com.ionutbalosin.jvm.energy.consumption.$sample_name\""
    else
      BUILD_CMD="$APP_HOME/../mvnw -D.maven.clean.skip=true -DmainClass=\"com.ionutbalosin.jvm.energy.consumption.$sample_name\" -DimageName=\"$sample_name\" -Pnative package"
    fi

    echo "Building $sample_name at: $(date) ... "
    echo "$PREFIX_COMMAND $BUILD_CMD"
    echo ""
    eval "$PREFIX_COMMAND $BUILD_CMD" > "$build_output_file" 2>&1
    if [ $? -ne 0 ]; then
      echo "ERROR: Build failed for $sample_name. Check $build_output_file for details."
      return 1
    fi
  done
}

start_sample() {
  sample_name="$1"
  sample_test_type="$2"

  run_output_file="$OUTPUT_FOLDER/$sample_name/logs/$JVM_IDENTIFIER-run-$sample_test_type-$TEST_RUN_IDENTIFIER.log"
  stats_output_file="$OUTPUT_FOLDER/$sample_name/perf/$JVM_IDENTIFIER-run-$sample_test_type-$TEST_RUN_IDENTIFIER"
  PREFIX_COMMAND="${OS_PREFIX_COMMAND/((statsOutputFile))/$stats_output_file}"

  if [ "$JVM_IDENTIFIER" != "native-image" ]; then
    export RUN_CMD="$JAVA_HOME/bin/java $JAVA_OPS $APP_HOME/src/main/java/com/ionutbalosin/jvm/energy/consumption/$sample_name.java $sample_test_type"
  else
    export RUN_CMD="$APP_HOME/target/$sample_name $JAVA_OPS $sample_test_type"
  fi

  echo "Running $sample_name ($sample_test_type) at: $(date) ... "
  echo "$PREFIX_COMMAND $RUN_CMD"
  echo ""
  eval "$PREFIX_COMMAND $RUN_CMD" > "$run_output_file" 2>&1
  if [ $? -ne 0 ]; then
    echo "ERROR: Run failed for $sample_name ($sample_test_type). Check $run_output_file for details."
    return 1
  fi
}

start_samples() {
  echo "Starting running samples at: $(date) ... "
  read -r -p "Press ENTER to continue or CRTL+C to abort ... "

  start_sample "ThrowExceptionPatterns" "const" || exit 1
  start_sample "ThrowExceptionPatterns" "lambda" || exit 1
  start_sample "ThrowExceptionPatterns" "new" || exit 1
  start_sample "ThrowExceptionPatterns" "override_fist" || exit 1

  start_sample "MemoryAccessPatterns" "linear" || exit 1
  start_sample "MemoryAccessPatterns" "random_page" || exit 1
  start_sample "MemoryAccessPatterns" "random_heap" || exit 1

  start_sample "LoggingPatterns" "string_format" || exit 1
  start_sample "LoggingPatterns" "lambda_heap" || exit 1
  start_sample "LoggingPatterns" "lambda_local" || exit 1
  start_sample "LoggingPatterns" "guarded_parametrized" || exit 1
  start_sample "LoggingPatterns" "guarded_unparametrized" || exit 1
  start_sample "LoggingPatterns" "unguarded_parametrized" || exit 1
  start_sample "LoggingPatterns" "unguarded_unparametrized" || exit 1

  start_sample "SortingAlgorithms" "bubble_sort" || exit 1
  start_sample "SortingAlgorithms" "merge_sort" || exit 1
  start_sample "SortingAlgorithms" "quick_sort" || exit 1
  start_sample "SortingAlgorithms" "radix_sort" || exit 1

  start_sample "VirtualCalls" "bimorphic" || exit 1
  start_sample "VirtualCalls" "megamorphic_24" || exit 1

  echo "Finished running samples at: $(date) ... "
}

check_command_line_options "$@"
if [ $? -ne 0 ]; then
  exit 1
fi

echo ""
echo "+================================+"
echo "| [1/7] Configuration Properties |"
echo "+================================+"
. ../scripts/shell/configure-properties.sh || exit 1

echo ""
echo "+=============================+"
echo "| [2/7] Hardware Architecture |"
echo "+=============================+"
. ../scripts/shell/configure-arch.sh

echo ""
echo "+========================+"
echo "| [3/7] OS Configuration |"
echo "+========================+"
. ../scripts/shell/configure-os.sh || exit 1
. ../scripts/shell/configure-os-$OS.sh

echo ""
echo "+=========================+"
echo "| [4/7] JVM Configuration |"
echo "+=========================+"
. ../scripts/shell/configure-jvm.sh || exit 1

echo ""
echo "+==================================+"
echo "| [5/7] Java samples configuration |"
echo "+==================================+"
configure_samples

# make sure the output resources (e.g., folders and files) exist
create_output_resources

echo ""
echo "+==============================+"
echo "| [6/7] Build the Java samples |"
echo "+==============================+"
if [ "$2" == "--skip-build" ]; then
  echo "WARNING: Skipping the build process. A previously generated artifact will be used to start the application."
else
  build_samples || exit 1
fi

echo ""
echo "+==============================+"
echo "| [7/7] Start the Java samples |"
echo "+==============================+"
start_samples || exit 1

echo ""
echo "*** Test $TEST_RUN_IDENTIFIER successfully finished! ***"
