#!/bin/bash
#
# JVM Energy Consumption
#
# MIT License
#
# Copyright (c) 2023-2024 Ionut Balosin, Ko Turk
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
  if [[ $EUID -ne 0 || ($# -lt 1 || $# -gt 2) ]]; then
    echo "Usage: sudo ./run-samples.sh --test-run-identifier=<test-run-identifier> [--skip-build]"
    echo ""
    echo "Options:"
    echo "  --run-identifier=<run-identifier>  A mandatory parameter to identify the current execution run(s). It can be a single value or a comma-separated list for multiple runs."
    echo "  --duration=<duration>              An optional parameter to specify the duration in seconds. If not specified, it is set by default to 900 seconds."
    echo "  --skip-build                       An optional parameter to skip the build process."
    echo ""
    echo "Examples:"
    echo "  $ sudo ./run-samples.sh --run-identifier=1"
    echo "  $ sudo ./run-samples.sh --run-identifier=1,2 --duration=3600"
    echo "  $ sudo ./run-samples.sh --run-identifier=1,2,3  --duration=3600 --skip-build"
    echo ""
    return 1
  fi

  APP_SKIP_BUILD=""
  APP_RUNNING_TIME="900"
  APP_RUN_IDENTIFIER=""

  while [ $# -gt 0 ]; do
    case "$1" in
      --skip-build)
        APP_SKIP_BUILD="--skip-build"
        ;;
      --duration=*)
        APP_RUNNING_TIME="${1#*=}"
        ;;
      --run-identifier=*)
        APP_RUN_IDENTIFIER="${1#*=}"
        ;;
      *)
        echo "ERROR: Unknown parameter: $1"
        return 1
        ;;
    esac
    shift
  done

  if [ -z "$APP_RUN_IDENTIFIER" ]; then
    echo "ERROR: Missing mandatory parameter run identifier."
    return 1
  fi
}

configure_samples() {
  CURR_DIR=$(pwd)
  JAVA_OPS="-Xms1m -Xmx6g"
  # Defines the list of all Java sample apps
  SAMPLE_APPS=(
    "ThrowExceptionPatterns"
    "MemoryAccessPatterns"
    "LoggingPatterns"
    "SortingAlgorithms"
    "VirtualCalls"
  )

  echo "Java samples: ${SAMPLE_APPS[@]}"
  echo "Java samples skip build: $APP_SKIP_BUILD"
  echo "Java opts: $JAVA_OPS"
  echo "Java samples time: $APP_RUNNING_TIME sec"
  read -ra APP_RUN_IDENTIFIERS <<< "$(tr ',' ' ' <<< "$APP_RUN_IDENTIFIER")"
  echo "Run identifier(s): ${APP_RUN_IDENTIFIERS[@]}"

  echo ""
  read -r -p "If the above configuration is correct, press ENTER to continue or CRTL+C to abort ... "
}

create_output_resources() {
  for sample_app in "${SAMPLE_APPS[@]}"; do
    mkdir -p "$OUTPUT_FOLDER/$sample_app/perf"
    mkdir -p "$OUTPUT_FOLDER/$sample_app/logs"
    mkdir -p "$OUTPUT_FOLDER/$sample_app/power"
  done
}

build_samples() {
  for sample_app in "${SAMPLE_APPS[@]}"; do
    build_output_file="$CURR_DIR/$OUTPUT_FOLDER/$sample_app/logs/$JVM_IDENTIFIER-build-$RUN_IDENTIFIER.log"
    stats_output_file="$CURR_DIR/$OUTPUT_FOLDER/$sample_app/perf/$JVM_IDENTIFIER-build-$RUN_IDENTIFIER"
    PREFIX_COMMAND="${OS_PREFIX_COMMAND/((statsOutputFile))/$stats_output_file}"

    if [ "$JVM_IDENTIFIER" != "native-image" ]; then
      BUILD_CMD="$CURR_DIR/../mvnw clean package -DmainClass=\"com.ionutbalosin.jvm.energy.consumption.$sample_app\""
    else
      BUILD_CMD="$CURR_DIR/../mvnw -D.maven.clean.skip=true -DmainClass=\"com.ionutbalosin.jvm.energy.consumption.$sample_app\" -DimageName=\"$sample_app\" -Pnative package"
    fi

    echo "Building $sample_app at: $(date) ... "
    echo "$PREFIX_COMMAND $BUILD_CMD"
    echo ""

    eval "$PREFIX_COMMAND $BUILD_CMD" > "$build_output_file" 2>&1
    if [ $? -ne 0 ]; then
      echo "ERROR: Build failed for $sample_app. Check $build_output_file for details."
      return 1
    fi
  done
}

start_sample() {
  sample_app="$1"
  sample_app_test_type="$2"

  run_output_file="$OUTPUT_FOLDER/$sample_app/logs/$JVM_IDENTIFIER-run-$sample_app_test_type-$RUN_IDENTIFIER.log"
  stats_output_file="$OUTPUT_FOLDER/$sample_app/perf/$JVM_IDENTIFIER-run-$sample_app_test_type-$RUN_IDENTIFIER"
  PREFIX_COMMAND="${OS_PREFIX_COMMAND/((statsOutputFile))/$stats_output_file}"

  if [ "$JVM_IDENTIFIER" != "native-image" ]; then
    RUN_CMD="$JAVA_HOME/bin/java $JAVA_OPS -Dduration=$APP_RUNNING_TIME $CURR_DIR/src/main/java/com/ionutbalosin/jvm/energy/consumption/$sample_app.java $sample_app_test_type"
  else
    RUN_CMD="$CURR_DIR/target/$sample_app $JAVA_OPS -Dduration=$APP_RUNNING_TIME $sample_app_test_type"
  fi

  echo "Running $sample_app ($sample_app_test_type) at: $(date) ... "
  echo "$PREFIX_COMMAND $RUN_CMD"
  echo ""

  eval "$PREFIX_COMMAND $RUN_CMD" > "$run_output_file" 2>&1
  if [ $? -ne 0 ]; then
    echo "ERROR: Run failed for $sample_app ($sample_app_test_type). Check $run_output_file for details."
    return 1
  fi
}

start_samples() {
  echo "Starting running samples at: $(date) ... "

  # Defines the list of all Java sample apps including their running parameters
  SAMPLE_APPS_WITH_TEST_TYPES=(
    "ThrowExceptionPatterns const"
    "ThrowExceptionPatterns lambda"
    "ThrowExceptionPatterns new"
    "ThrowExceptionPatterns override_fist"

    "MemoryAccessPatterns linear"
    "MemoryAccessPatterns random_page"
    "MemoryAccessPatterns random_heap"

    "LoggingPatterns lambda_heap"
    "LoggingPatterns lambda_local"
    "LoggingPatterns guarded_parametrized"
    "LoggingPatterns guarded_unparametrized"
    "LoggingPatterns unguarded_parametrized"
    "LoggingPatterns unguarded_unparametrized"

    "SortingAlgorithms bubble_sort"
    "SortingAlgorithms merge_sort"
    "SortingAlgorithms quick_sort"
    "SortingAlgorithms radix_sort"

    "VirtualCalls bimorphic"
    "VirtualCalls megamorphic_24"
  )

  iteration=1
  loop_counter="${#SAMPLE_APPS_WITH_TEST_TYPES[@]}"

  for sample_app_with_test_type in "${SAMPLE_APPS_WITH_TEST_TYPES[@]}"; do
    read -r -a sample_app_components <<< "$sample_app_with_test_type"
    sample_app="${sample_app_components[0]}"
    sample_app_test_type="${sample_app_components[1]}"

    echo ""
    echo "+---------------------------------------------------+"
    echo "| [7.$iteration/$loop_counter] Start the power consumption measurements |"
    echo "+---------------------------------------------------+"
    power_output_file="$OUTPUT_FOLDER/$sample_app/power/$JVM_IDENTIFIER-run-$sample_app_test_type-$RUN_IDENTIFIER.stats"
    start_power_consumption --background --output-file="$power_output_file" || exit 1

    echo "+--------------------------------+"
    echo "| [7.$iteration/$loop_counter] Start the Java sample |"
    echo "+--------------------------------+"
    start_sample $sample_app $sample_app_test_type || exit 1

    echo "+--------------------------------------------------+"
    echo "| [7.$iteration/$loop_counter] Stop the power consumption measurements |"
    echo "+--------------------------------------------------+"
    stop_power_consumption

    ((iteration++))
  done

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
. ../scripts/shell/configure-os-"$OS".sh

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

for app_run_identifier in "${APP_RUN_IDENTIFIERS[@]}"; do
  RUN_IDENTIFIER="$app_run_identifier"

  echo ""
  echo "*** Starting run $RUN_IDENTIFIER at: $(date) ... ***"

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
  . ../scripts/shell/power-consumption-os-$OS.sh
  start_samples || exit 1

  echo ""
  echo "*** Run $RUN_IDENTIFIER successfully finished at: $(date) ***"

done