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
  if [[ $# -lt 1 || $# -gt 3 ]]; then
    echo "Usage: ./run-samples.sh --run-identifier=<run-identifier> [--duration=<duration>] [--skip-build]"
    echo ""
    echo "Options:"
    echo "  --run-identifier=<run-identifier>  A mandatory parameter to identify the current execution run(s). It can be a single value or a comma-separated list for multiple runs."
    echo "  --duration=<duration>              An optional parameter to specify the duration in seconds. If not specified, it is set by default to 900 seconds."
    echo "  --skip-build                       An optional parameter to skip the build process."
    echo ""
    echo "Examples:"
    echo "  $ ./run-samples.sh --run-identifier=1"
    echo "  $ ./run-samples.sh --run-identifier=1,2 --duration=3600"
    echo "  $ ./run-samples.sh --run-identifier=1,2,3  --duration=3600 --skip-build"
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
  JAVA_OPS="--enable-preview --source 21 -Xms1m -Xmx6g"
  # Defines the list of all Java sample apps
  SAMPLE_APPS=(
    "LoggingPatterns"
    "MemoryAccessPatterns"
    "SortingAlgorithms"
    "StringConcatenationPatterns"
    "ThrowExceptionPatterns"
    "VirtualCalls"
    "VPThreadQueueThroughput"
  )
  # Defines the list of all Java sample apps including their running types (i.e., parameters)
  SAMPLE_APPS_WITH_RUN_TYPES=(
    "LoggingPatterns lambda"
    "LoggingPatterns guarded_parametrized"
    "LoggingPatterns guarded_unparametrized"
    "LoggingPatterns unguarded_parametrized"
    "LoggingPatterns unguarded_unparametrized"

    "MemoryAccessPatterns linear"
    "MemoryAccessPatterns random_page"
    "MemoryAccessPatterns random_heap"

    "SortingAlgorithms quick_sort"
    "SortingAlgorithms merge_sort"
    "SortingAlgorithms radix_sort"

    "StringConcatenationPatterns plus_operator"
    "StringConcatenationPatterns string_builder"
    "StringConcatenationPatterns string_template"

    "ThrowExceptionPatterns const"
    "ThrowExceptionPatterns override_fist"
    "ThrowExceptionPatterns lambda"
    "ThrowExceptionPatterns new"

    "VirtualCalls monomorphic"
    "VirtualCalls bimorphic"
    "VirtualCalls megamorphic_3"
    "VirtualCalls megamorphic_8"

    "VPThreadQueueThroughput virtual"
    "VPThreadQueueThroughput platform"
  )

  echo "Java samples: ${SAMPLE_APPS[@]}"
  echo "Java samples skip build: $APP_SKIP_BUILD"
  echo "Java opts: $JAVA_OPS"
  echo "Java samples time: $APP_RUNNING_TIME sec"
  read -ra APP_RUN_IDENTIFIERS <<< "$(tr ',' ' ' <<< "$APP_RUN_IDENTIFIER")"
  echo "Run identifier(s): ${APP_RUN_IDENTIFIERS[@]}"
}

create_output_resources() {
  for sample_app in "${SAMPLE_APPS[@]}"; do
    mkdir -p "$OUTPUT_FOLDER/$sample_app/logs"
    mkdir -p "$OUTPUT_FOLDER/$sample_app/power"
  done
}

build_sample() {
  sample_app="$1"
  build_output_file="$CURR_DIR/$OUTPUT_FOLDER/$sample_app/logs/$JVM_IDENTIFIER-build-$RUN_IDENTIFIER.log"

  if [ "$JVM_IDENTIFIER" != "native-image" ]; then
    BUILD_CMD="$CURR_DIR/../mvnw clean package -DmainClass=\"com.ionutbalosin.jvm.energy.consumption.$sample_app\""
  else
    BUILD_CMD="$CURR_DIR/../mvnw -D.maven.clean.skip=true -DmainClass=\"com.ionutbalosin.jvm.energy.consumption.$sample_app\" -DimageName=\"$sample_app\" -Pnative package"
  fi

  sample_build_command="$BUILD_CMD > $build_output_file 2>&1"
  echo "Building $sample_app at: $(date) ... "
  echo "$sample_build_command"

  eval "$sample_build_command"
  if [ $? -ne 0 ]; then
    echo "ERROR: Build failed for $sample_app. Check $build_output_file for details."
    return 1
  fi
}

build_samples() {
  for sample_app in "${SAMPLE_APPS[@]}"; do
    power_output_file="$OUTPUT_FOLDER/$sample_app/power/$JVM_IDENTIFIER-build-$RUN_IDENTIFIER.txt"

    start_system_power_consumption --background --output-file="$power_output_file" || exit 1
    build_sample $sample_app || exit 1
    stop_system_power_consumption

    echo ""
  done
}

start_sample() {
  sample_app="$1"
  sample_app_run_type="$2"

  run_output_file="$OUTPUT_FOLDER/$sample_app/logs/$JVM_IDENTIFIER-run-$sample_app_run_type-$RUN_IDENTIFIER.log"

  if [ "$JVM_IDENTIFIER" != "native-image" ]; then
    RUN_CMD="$JAVA_HOME/bin/java $JAVA_OPS -Dduration=$APP_RUNNING_TIME $CURR_DIR/src/main/java/com/ionutbalosin/jvm/energy/consumption/$sample_app.java $sample_app_run_type"
  else
    RUN_CMD="$CURR_DIR/target/$sample_app $JAVA_OPS -Dduration=$APP_RUNNING_TIME $sample_app_run_type"
  fi

  sample_run_command="$RUN_CMD > $run_output_file 2>&1"
  echo "Running $sample_app ($sample_app_run_type) at: $(date) ... "
  echo "$sample_run_command"

  eval "$sample_run_command"
  if [ $? -ne 0 ]; then
    echo "ERROR: Run failed for $sample_app ($sample_app_run_type). Check $run_output_file for details."
    return 1
  fi
}

start_samples() {
  for sample_app_with_run_type in "${SAMPLE_APPS_WITH_RUN_TYPES[@]}"; do
    read -r -a sample_app_with_run_type_array <<< "$sample_app_with_run_type"
    sample_app="${sample_app_with_run_type_array[0]}"
    sample_app_run_type="${sample_app_with_run_type_array[1]}"
    power_output_file="$OUTPUT_FOLDER/$sample_app/power/$JVM_IDENTIFIER-run-$sample_app_run_type-$RUN_IDENTIFIER.txt"

    start_system_power_consumption --output-file="$power_output_file" --background || exit 1
    start_sample $sample_app $sample_app_run_type || exit 1
    stop_system_power_consumption

    echo ""
  done
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
. ../scripts/shell/system-power-consumption-os-"$OS".sh

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

app_run_counter=1
app_run_limit="${#APP_RUN_IDENTIFIERS[@]}"
for app_run_identifier in "${APP_RUN_IDENTIFIERS[@]}"; do
  RUN_IDENTIFIER="$app_run_identifier"

  echo ""
  echo "+===================================+"
  echo "| [6/7][$app_run_counter/$app_run_limit] Build the Java samples |"
  echo "+===================================+"
  if [ "$2" == "--skip-build" ]; then
    echo "WARNING: Skipping the build process. A previously generated artifact will be used to start the application."
  else
    build_samples || exit 1
  fi

  echo ""
  echo "+===================================+"
  echo "| [7/7][$app_run_counter/$app_run_limit] Start the Java samples |"
  echo "+===================================+"
  start_samples || exit 1

  ((app_run_counter++))
done

echo ""
echo "Everything went well, bye bye! 👋"