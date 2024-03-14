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
  APP_RUNNING_TIME="7200"
  APP_ENABLE_PGO=""
  APP_PGO_DIR="/pgo/native-image"
  APP_SKIP_OS_TUNING=""
  APP_SKIP_BUILD=""
  APP_SKIP_RUN=""

  if [[ $# -gt 8 ]]; then
    echo "Usage: ./run-application.sh [--jvm-identifier=<jvm-identifier>] [--run-identifier=<run-identifier>] [--duration=<duration>] [--enable-pgo] [--pgo-dir=<pgo-dir>] [--skip-os-tuning] [--skip-build] [--skip-run]"
    echo ""
    echo "Options:"
    echo "  --jvm-identifier=<jvm-identifier>  An optional parameter to specify the JVM to run with. If not specified, the user will be prompted to select it at the beginning of the run."
    echo "                                     Accepted options: {${APP_JVM_IDENTIFIERS[*]}}."
    echo "  --run-identifier=<run-identifier>  An optional parameter to identify the current execution run. It can be a number or any other string identifier. If not specified, it defaults to the value '$APP_RUN_IDENTIFIER'."
    echo "  --duration=<duration>              An optional parameter to specify the duration in seconds. If not specified, it is set by default to $APP_RUNNING_TIME seconds."
    echo "  --enable-pgo                       An optional parameter to enable PGO and G1 GC for the native image."
    echo "  --pgo-dir                          An optional parameter to specify PGO profile for the native image. If not specified, it is set by default to $APP_PGO_DIR."
    echo "  --skip-os-tuning                   An optional parameter to skip the OS tuning. Since only Linux has specific OS tunings, they will be skipped. Configurations like disabling address space layout randomization, disabling turbo boost mode, setting the CPU governor to performance, disabling CPU hyper-threading will not be applied."
    echo "  --skip-build                       An optional parameter to skip the build process."
    echo "  --skip-run                         An optional parameter to skip the run."
    echo ""
    echo "Examples:"
    echo "  $ ./run-application.sh"
    echo "  $ ./run-application.sh --jvm-identifier=openjdk-hotspot-vm"
    echo "  $ ./run-application.sh --run-identifier=default --jvm-identifier=openjdk-hotspot-vm --duration=60"
    echo "  $ ./run-application.sh --run-identifier=pgo --jvm-identifier=native-image --duration=60 --enable-pgo --skip-os-tuning"
    echo "  $ ./run-application.sh --run-identifier=pgo --jvm-identifier=native-image --duration=60 --enable-pgo --skip-os-tuning --skip-build"
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
      --enable-pgo)
        APP_ENABLE_PGO="--enable-pgo"
        ;;
      --pgo-dir=*)
        APP_PGO_DIR="${1#*=}"
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

configure_application() {
  CURR_DIR=$(pwd)
  APP_HOME="$SPRING_PETCLINIC_HOME"
  APP_PORT=8080
  APP_BASE_URL="localhost:$APP_PORT"
  JAVA_OPS="-Xms1m -Xmx12g"
  # JFR_OPS="-XX:StartFlightRecording=duration=$APP_RUNNING_TIMEs,filename=$OUTPUT_FOLDER/jfr/$JVM_IDENTIFIER-run-$APP_RUN_IDENTIFIER.jfr"

  echo "Application run identifier: $APP_RUN_IDENTIFIER"
  echo "Application running time: $APP_RUNNING_TIME sec"
  echo "Application skip build: $APP_SKIP_BUILD"
  echo "Application skip run: $APP_SKIP_RUN"
  echo "Application skip OS tuning: $APP_SKIP_OS_TUNING"
  echo "JVM identifier: $APP_JVM_IDENTIFIER"
  echo "Native Image enable PGO: $APP_ENABLE_PGO"
  echo "Native Image PGO directory: $APP_PGO_DIR"
  echo "Application home: $APP_HOME"
  echo "Application base url: $APP_BASE_URL"
  echo "Java opts: $JAVA_OPS"
  echo "JFR opts: $JFR_OPS"
}

create_output_resources() {
  mkdir -p "$OUTPUT_FOLDER/log"
  mkdir -p "$OUTPUT_FOLDER/power"
  mkdir -p "$OUTPUT_FOLDER/perf"
  mkdir -p "$OUTPUT_FOLDER/jfr"
}

# The logic for building with PGO enabled is as follows:
# 1) If the PGO profile does not exist, it means it was not previously generated. Therefore:
#  - Run the build with '--pgo-instrument'
#  - Run the native executable with '-XX:ProfilesDumpFile=profile.iprof' and get 'profile.iprof' at the end of the run
# 2) If the PGO profile exists, it means it was previously generated, and we have to instrument the build to use it:
#  - Build with '--pgo=profile.iprof'
#  - Run the native executable to benefit from the PGO profile
native_image_enable_pgo_g1gc() {
  PGO_G1GC_BUILD_ARGS=""
  PGO_G1GC_RUN_ARGS=""

  # Enable PGO and G1 GC for the native image; otherwise, disabled by default.
  # Note: G1GC is currently only supported on Linux AMD64 and AArch64
  if [ "$JVM_IDENTIFIER" = "native-image" ] && [ "$APP_ENABLE_PGO" = "--enable-pgo" ]; then
    # Enable PGO
    pgo_output_file="$CURR_DIR/$APP_PGO_DIR/default.iprof"
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

build_application() {
  build_output_file="$CURR_DIR/$OUTPUT_FOLDER/log/$JVM_IDENTIFIER-build-$APP_RUN_IDENTIFIER.log"

  if [ "$JVM_IDENTIFIER" != "native-image" ]; then
    BUILD_CMD="$APP_HOME/mvnw -f $APP_HOME/pom.xml clean package -Dmaven.test.skip"
  else
    native_image_enable_pgo_g1gc
    BUILD_CMD="$APP_HOME/mvnw -f $APP_HOME/pom.xml clean package -Dmaven.test.skip -DbuildArgs=\"$PGO_G1GC_BUILD_ARGS\" -Pnative native:compile"
  fi

  app_build_command="$BUILD_CMD > $build_output_file 2>&1"
  echo "Building application at: $(date) ... "
  echo "$app_build_command"

  eval "$app_build_command"
  if [ $? -ne 0 ]; then
    echo "ERROR: Build failed for application. Check $build_output_file for details."
    return 1
  fi
}

check_application_port_availability() {
  if lsof -i :$APP_PORT >/dev/null 2>&1 ; then
    echo "ERROR: There is already an application running on port $APP_PORT. Please stop it before running another one on the same port."
    return 1
  fi
}

start_application() {
  run_output_file="$OUTPUT_FOLDER/log/$JVM_IDENTIFIER-run-$APP_RUN_IDENTIFIER.log"

  if [ "$JVM_IDENTIFIER" != "native-image" ]; then
    RUN_CMD="$JAVA_HOME/bin/java $JAVA_OPS $JFR_OPS -jar $APP_HOME/target/*.jar"
  else
    native_image_enable_pgo_g1gc
    RUN_CMD="$APP_HOME/target/spring-petclinic $JAVA_OPS $PGO_G1GC_RUN_ARGS"
  fi

  app_run_command="$RUN_CMD > $run_output_file 2>&1 &"
  echo "Running application at: $(date) ... "
  echo "$app_run_command"

  eval "$app_run_command"
  APP_PID=$!

  # Sleep for a short duration to allow the asynchronous process to start
  sleep 3

  if ! ps -p "$APP_PID" > /dev/null; then
    echo "ERROR: Run failed for application. Check $run_output_file for details."
    return 1
  fi
}

check_application_initial_request() {
  # Set the timeout threshold (in seconds)
  # Note: the SECONDS is a built-in variable in Bash that represents the number of seconds since the shell was started.
  timeout=30
  end_time=$((SECONDS + timeout))

  # Wait until the application answers to the first request or timeout occurs
  while [[ "$(curl -s -o /dev/null -w ''%{http_code}'' http://$APP_BASE_URL/owners?lastName=)" != "200" ]]; do
    sleep .00001

    # Check if the timeout has been reached
    if [ $SECONDS -ge $end_time ]; then
      echo "ERROR: Application with PID $APP_PID did not respond within $timeout seconds (timeout)."
      return 1
    fi
  done

  # reset the terminal line settings, otherwise (sometimes) it gets a wired indentation
  stty sane
  echo "Application with PID $APP_PID successfully started at $(date)."
}

stop_application() {
  echo "Stopping the application with PID $APP_PID."
  kill -TERM "$APP_PID"
  echo "Application with PID $APP_PID successfully stopped at $(date)."
}

check_command_line_options "$@"
if [ $? -ne 0 ]; then
  exit 1
fi

echo ""
echo "+================================+"
echo "| [1/8] Configuration Properties |"
echo "+================================+"
. ../../scripts/shell/configure-properties.sh || exit 1

echo ""
echo "+=============================+"
echo "| [2/8] Hardware Architecture |"
echo "+=============================+"
. ../../scripts/shell/configure-arch.sh

echo ""
echo "+========================+"
echo "| [3/8] OS Configuration |"
echo "+========================+"
. ../../scripts/shell/configure-os.sh || exit 1
if [ "$APP_SKIP_OS_TUNING" == "--skip-os-tuning" ]; then
  echo "WARNING: Skipping the OS tuning settings."
else
  . ../../scripts/shell/configure-os-"$OS".sh
fi
. ../../scripts/shell/system-power-consumption-os-"$OS".sh
. ../../scripts/shell/process-performance-monitoring-os-"$OS".sh

echo ""
echo "+=========================+"
echo "| [4/8] JVM Configuration |"
echo "+=========================+"
. ../../scripts/shell/configure-jvm.sh "$APP_JVM_IDENTIFIER" || exit 1

echo ""
echo "+=================================+"
echo "| [5/8] Application configuration |"
echo "+=================================+"
configure_application

# make sure the output resources (e.g., folders and files) exist
create_output_resources

echo ""
echo "+=============================+"
echo "| [6/8] Build the application |"
echo "+=============================+"
if [ "$APP_SKIP_BUILD" == "--skip-build" ]; then
  echo "WARNING: Skipping the build process. A previously generated artifact will be used to start the application."
else
  power_output_file="$OUTPUT_FOLDER/power/$JVM_IDENTIFIER-build-$APP_RUN_IDENTIFIER.txt"
  start_system_power_consumption --background --output-file="$power_output_file" || exit 1
  build_application || exit 1
  stop_system_power_consumption
fi

echo ""
echo "+=============================+"
echo "| [7/8] Start the application |"
echo "+=============================+"
if [ "$APP_SKIP_RUN" == "--skip-run" ]; then
    echo "WARNING: Skipping the run process."
  else
    check_application_port_availability || exit 1

    # Start system power consumption monitoring
    power_output_file="$OUTPUT_FOLDER/power/$JVM_IDENTIFIER-run-$APP_RUN_IDENTIFIER.txt"
    start_system_power_consumption --background --output-file="$power_output_file" || exit 1

    # Start the application
    start_application || { stop_system_power_consumption && exit 1; }
    check_application_initial_request || { stop_system_power_consumption && exit 1; }

    # Start process performance monitoring
    performance_monitoring_output_file="$OUTPUT_FOLDER/perf/$JVM_IDENTIFIER-run-$APP_RUN_IDENTIFIER.txt"
    start_process_performance_monitoring --pid="$APP_PID" --output-file="$performance_monitoring_output_file" --duration="$APP_RUNNING_TIME" || { stop_system_power_consumption && stop_application && exit 1; }

    echo "Please enjoy a ☕ while the application runs. This may take approximately $APP_RUNNING_TIME seconds ..."
    sleep "$APP_RUNNING_TIME"

    echo ""
    echo "+============================+"
    echo "| [8/8] Stop the application |"
    echo "+============================+"
    stop_process_performance_monitoring
    stop_application
    stop_system_power_consumption

    # give a bit of time to the process to gracefully shut down
    sleep 5
fi

echo "Everything went well, bye bye! 👋"