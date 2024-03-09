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
  APP_JVM_IDENTIFIERS=("openjdk-hotspot-vm" "graalvm-ce" "oracle-graalvm" "native-image" "azul-prime-vm" "eclipse-openj9-vm")
  APP_RUN_IDENTIFIER="default"
  APP_JVM_IDENTIFIER=""
  APP_RUNNING_TIME="1200"
  APP_ENABLE_PGO_G1GC=""
  APP_SKIP_OS_TUNING=""
  APP_SKIP_BUILD=""
  APP_SKIP_RUN=""

  if [[ $# -gt 5 ]]; then
    echo "Usage: ./run-samples.sh [--jvm-identifier=<jvm-identifier>] [--run-identifier=<run-identifier>] [--duration=<duration>] [--enable-pgo-g1gc] [--skip-os-tuning] [--skip-build] [--skip-run]"
    echo ""
    echo "Options:"
    echo "  --jvm-identifier=<jvm-identifier>  An optional parameter to specify the JVM to run with. If not specified, the user will be prompted to select it at the beginning of the run."
    echo "                                     Accepted options: {${APP_JVM_IDENTIFIERS[*]}}."
    echo "  --run-identifier=<run-identifier>  An optional parameter to identify the current execution run. It can be a number or any other string identifier. If not specified, it defaults to the value '$APP_RUN_IDENTIFIER'."
    echo "  --duration=<duration>              An optional parameter to specify the duration in seconds. If not specified, it is set by default to $APP_RUNNING_TIME seconds."
    echo "  --enable-pgo-g1gc                  An optional parameter to enable PGO and G1 GC for the native image."
    echo "  --skip-os-tuning                   An optional parameter to skip the OS tuning. Since only Linux has specific OS tunings, they will be skipped. Configurations like disabling address space layout randomization, disabling turbo boost mode, setting the CPU governor to performance, disabling CPU hyper-threading will not be applied."
    echo "  --skip-build                       An optional parameter to skip the build process."
    echo "  --skip-run                         An optional parameter to skip the run."
    echo ""
    echo "Examples:"
    echo "  $ ./run-samples.sh"
    echo "  $ ./run-samples.sh --jvm-identifier=openjdk-hotspot-vm"
    echo "  $ ./run-samples.sh --jvm-identifier=openjdk-hotspot-vm --duration=60"
    echo "  $ ./run-samples.sh --run-identifier=default --jvm-identifier=openjdk-hotspot-vm --duration=60 --skip-os-tuning"
    echo "  $ ./run-samples.sh --run-identifier=pgo_g1gc --jvm-identifier=openjdk-hotspot-vm --duration=60 --enable-pgo-g1gc --skip-os-tuning"
    echo "  $ ./run-samples.sh --run-identifier=pgo_g1gc --jvm-identifier=openjdk-hotspot-vm --duration=60 --enable-pgo-g1gc --skip-os-tuning --skip-build"
    echo "  $ ./run-samples.sh --run-identifier=pgo_g1gc --jvm-identifier=openjdk-hotspot-vm --duration=60 --enable-pgo-g1gc --skip-os-tuning --skip-run"
    echo ""
    return 1
  fi

  while [ $# -gt 0 ]; do
    case "$1" in
      --run-identifier=*)
        APP_RUN_IDENTIFIER="${1#*=}"
        ;;
      --jvm-identifier=*)
        APP_JVM_IDENTIFIER="${1#*=}"
        ;;
      --duration=*)
        APP_RUNNING_TIME="${1#*=}"
        ;;
      --enable-pgo-g1gc)
        APP_ENABLE_PGO_G1GC="--enable-pgo-g1gc"
        ;;
      --skip-os-tuning)
        APP_SKIP_OS_TUNING="--skip-os-tuning"
        ;;
      --skip-build)
        APP_SKIP_BUILD="--skip-build"
        ;;
      --skip-run)
        APP_SKIP_RUN="--skip-run"
        ;;
      *)
        echo "ERROR: Unknown parameter $1"
        return 1
        ;;
    esac
    shift
  done
}

configure_samples() {
  CURR_DIR=$(pwd)
  PREVIEW_FEATURES="--enable-preview"
  JAVA_OPS="-Xms1m -Xmx8g $PREVIEW_FEATURES"
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

    "VPThreadQueueThroughput platform"
    # Note: This benchmark does not run on eclipse-openj9-vm
    "VPThreadQueueThroughput virtual"
  )

  echo "Application run identifier: $APP_RUN_IDENTIFIER"
  echo "Java samples: ${SAMPLE_APPS[@]}"
  echo "Java samples skip build: $APP_SKIP_BUILD"
  echo "Java samples skip OS tuning: $APP_SKIP_OS_TUNING"
  echo "Java samples time: $APP_RUNNING_TIME sec"
  echo "JVM identifier: $APP_JVM_IDENTIFIER"
  echo "Java opts: $JAVA_OPS"
}

create_output_resources() {
  for sample_app in "${SAMPLE_APPS[@]}"; do
    mkdir -p "$OUTPUT_FOLDER/$sample_app/logs"
    mkdir -p "$OUTPUT_FOLDER/$sample_app/power"
  done
}

# The logic for building and running with PGO enabled is as follows:
# 1) If the PGO profile does not exist, it means it was not previously generated. Therefore:
#  - Run the build with '--pgo-instrument'
#  - Run the native executable with '-XX:ProfilesDumpFile=profile.iprof' and get 'profile.iprof' at the end of the run
# 2) If the PGO profile exists, it means it was previously generated, and we have to instrument the build to use it:
#  - Build with '--pgo=profile.iprof'
#  - Run the native executable to benefit from the PGO profile
native_image_enable_pgo_g1gc() {
  sample_app="$1"
  sample_app_run_type="$2"
  PGO_G1GC_BUILD_ARGS=""
  PGO_G1GC_RUN_ARGS=""

  # Enable PGO and G1 GC for the native image; otherwise, disabled by default.
  # Note: G1GC is currently only supported on Linux AMD64 and AArch64
  if [ "$JVM_IDENTIFIER" = "native-image" ] && [ "$APP_ENABLE_PGO_G1GC" = "--enable-pgo-g1gc" ]; then
    # Enable PGO
    pgo_output_file="$CURR_DIR/pgo/native-image/$sample_app-$sample_app_run_type.iprof"
    if ! test -e "$pgo_output_file"; then
      PGO_G1GC_BUILD_ARGS="--pgo-instrument"
      PGO_G1GC_RUN_ARGS="-XX:ProfilesDumpFile=\"$pgo_output_file\""
    else
      PGO_G1GC_BUILD_ARGS="--pgo=\"$pgo_output_file\""
    fi

    # Enable G1 GC option only if the OS is Linux
    if [ "$OS" = "linux" ]; then
      PGO_G1GC_BUILD_ARGS="$PGO_G1GC_BUILD_ARGS,--gc=G1"
    fi
  fi
}

build_sample() {
  sample_app="$1"
  sample_app_run_type="$2"

  build_output_file="$CURR_DIR/$OUTPUT_FOLDER/$sample_app/logs/$JVM_IDENTIFIER-build-$sample_app_run_type-$APP_RUN_IDENTIFIER.log"

  if [ "$JVM_IDENTIFIER" != "native-image" ]; then
    BUILD_CMD="$CURR_DIR/../mvnw clean package -DmainClass=\"com.ionutbalosin.jvm.energy.consumption.$sample_app\" -Djar.finalName=$sample_app-$sample_app_run_type"
  else
    native_image_enable_pgo_g1gc $sample_app $sample_app_run_type
    # avoid adding a trailing comma to the build args when PGO_G1GC_BUILD_ARGS is empty
    build_args="${PREVIEW_FEATURES}${PGO_G1GC_BUILD_ARGS:+,}${PGO_G1GC_BUILD_ARGS}"
    BUILD_CMD="$CURR_DIR/../mvnw clean package -Pnative -DmainClass=\"com.ionutbalosin.jvm.energy.consumption.$sample_app\" -DimageName=\"$sample_app-$sample_app_run_type\" -DbuildArgs=\"$build_args\""
  fi

  sample_build_command="$BUILD_CMD > $build_output_file 2>&1"
  echo "Building $sample_app ($sample_app_run_type) at: $(date) ... "
  echo "$sample_build_command"

  eval "$sample_build_command"
  if [ $? -ne 0 ]; then
    echo "ERROR: Build failed for $sample_app. Check $build_output_file for details."
    return 1
  fi
}

start_sample() {
  sample_app="$1"
  sample_app_run_type="$2"

  run_output_file="$OUTPUT_FOLDER/$sample_app/logs/$JVM_IDENTIFIER-run-$sample_app_run_type-$APP_RUN_IDENTIFIER.log"

  if [ "$JVM_IDENTIFIER" != "native-image" ]; then
    RUN_CMD="$JAVA_HOME/bin/java $JAVA_OPS -Dduration=$APP_RUNNING_TIME -jar $CURR_DIR/target/$sample_app-$sample_app_run_type.jar $sample_app_run_type"
  else
    native_image_enable_pgo_g1gc $sample_app $sample_app_run_type
    RUN_CMD="$CURR_DIR/target/$sample_app-$sample_app_run_type $sample_app_run_type $JAVA_OPS $PGO_G1GC_RUN_ARGS -Dduration=$APP_RUNNING_TIME"
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
if [ "$APP_SKIP_OS_TUNING" == "--skip-os-tuning" ]; then
  echo "WARNING: Skipping the OS tuning settings."
else
  . ../scripts/shell/configure-os-"$OS".sh
fi
. ../scripts/shell/system-power-consumption-os-"$OS".sh

echo ""
echo "+=========================+"
echo "| [4/7] JVM Configuration |"
echo "+=========================+"
. ../scripts/shell/configure-jvm.sh "$APP_JVM_IDENTIFIER" || exit 1

echo ""
echo "+==================================+"
echo "| [5/7] Java samples configuration |"
echo "+==================================+"
configure_samples

# make sure the output resources (e.g., folders and files) exist
create_output_resources

sample_run_counter=1
sample_run_limit="${#SAMPLE_APPS_WITH_RUN_TYPES[@]}"
for sample_app_with_run_type in "${SAMPLE_APPS_WITH_RUN_TYPES[@]}"; do

  read -r -a sample_app_with_run_type_array <<< "$sample_app_with_run_type"
  sample_app="${sample_app_with_run_type_array[0]}"
  sample_app_run_type="${sample_app_with_run_type_array[1]}"

  echo ""
  echo "+====================================+"
  echo "| [6/7][$sample_run_counter/$sample_run_limit] Build the Java samples |"
  echo "+====================================+"
  if [ "$APP_SKIP_BUILD" == "--skip-build" ]; then
    echo "WARNING: Skipping the build process. A previously generated artifact will be used to start the application."
  else
    power_output_file="$OUTPUT_FOLDER/$sample_app/power/$JVM_IDENTIFIER-build-$sample_app_run_type-$APP_RUN_IDENTIFIER.txt"
    start_system_power_consumption --background --output-file="$power_output_file" || exit 1
    build_sample $sample_app $sample_app_run_type || exit 1
    stop_system_power_consumption
  fi

  echo ""
  echo "+====================================+"
  echo "| [7/7][$sample_run_counter/$sample_run_limit] Start the Java samples |"
  echo "+====================================+"
  if [ "$APP_SKIP_RUN" == "--skip-run" ]; then
    echo "WARNING: Skipping the run process."
  else
    power_output_file="$OUTPUT_FOLDER/$sample_app/power/$JVM_IDENTIFIER-run-$sample_app_run_type-$APP_RUN_IDENTIFIER.txt"
    start_system_power_consumption --background --output-file="$power_output_file" || exit 1
    start_sample $sample_app $sample_app_run_type || exit 1
    stop_system_power_consumption
  fi

  ((sample_run_counter++))
done

echo "Everything went well, bye bye! 👋"