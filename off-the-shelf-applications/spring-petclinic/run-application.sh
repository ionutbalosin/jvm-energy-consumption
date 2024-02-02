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
    echo "Usage: ./run-application.sh --test-run-identifier=<test-run-identifier> [--duration=<duration>] [--skip-build]"
    echo ""
    echo "Options:"
    echo "  --test-run-identifier=<test-run-identifier>  A mandatory parameter to identify the current execution test."
    echo "  --skip-build                                 An optional parameter to skip the build process."
    echo "  --duration=<duration>                        An optional parameter to specify the duration in seconds. If not specified, it is set by default to 900 seconds."
    echo ""
    echo "Examples:"
    echo "   $ ./run-application.sh --test-run-identifier=1"
    echo "   $ ./run-application.sh --test-run-identifier=1 --duration=3600"
    echo "   $ ./run-application.sh --test-run-identifier=1 --duration=3600 --skip-build"
    echo ""
    return 1
  fi

  TEST_RUN_IDENTIFIER=""
  APP_SKIP_BUILD=""
  APP_RUNNING_TIME="900"

  while [ $# -gt 0 ]; do
    case "$1" in
      --test-run-identifier=*)
        TEST_RUN_IDENTIFIER="${1#*=}"
        ;;
      --duration=*)
        APP_RUNNING_TIME="${1#*=}"
        ;;
      --skip-build)
        APP_SKIP_BUILD="--skip-build"
        ;;
      *)
        echo "ERROR: Unknown parameter: $1"
        return 1
        ;;
    esac
    shift
  done

  if [ -z "$TEST_RUN_IDENTIFIER" ]; then
    echo "ERROR: Missing mandatory parameter test run identifier."
    return 1
  fi
}

configure_application() {
  CURR_DIR=$(pwd)
  APP_HOME="$SPRING_PETCLINIC_HOME"
  APP_PORT=8080
  APP_BASE_URL="localhost:$APP_PORT"
  JAVA_OPS="-Xms1m -Xmx1g"
  # JFR_OPS="-XX:StartFlightRecording=duration=$APP_RUNNING_TIMEs,filename=$OUTPUT_FOLDER/jfr/$JVM_IDENTIFIER-run-$TEST_RUN_IDENTIFIER.jfr"

  echo "Test run identifier: $TEST_RUN_IDENTIFIER"
  echo "Application home: $APP_HOME"
  echo "Application base url: $APP_BASE_URL"
  echo "Application running time: $APP_RUNNING_TIME sec"
  echo "Application skip build: $APP_SKIP_BUILD"
  echo "Java opts: $JAVA_OPS"
  echo "JFR opts: $JFR_OPS"
}

create_output_resources() {
  mkdir -p "$OUTPUT_FOLDER/log"
  mkdir -p "$OUTPUT_FOLDER/power"
  mkdir -p "$OUTPUT_FOLDER/perf"
  mkdir -p "$OUTPUT_FOLDER/jfr"
}

build_application() {
  build_output_file="$CURR_DIR/$OUTPUT_FOLDER/log/$JVM_IDENTIFIER-build-$TEST_RUN_IDENTIFIER.log"

  if [ "$JVM_IDENTIFIER" != "native-image" ]; then
    BUILD_CMD="$APP_HOME/mvnw -f $APP_HOME/pom.xml clean package -Dmaven.test.skip"
  else
    BUILD_CMD="$APP_HOME/mvnw -f $APP_HOME/pom.xml clean package -Dmaven.test.skip -Pnative native:compile"
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

check_application_port() {
  if lsof -i :$APP_PORT >/dev/null 2>&1 ; then
    echo "ERROR: There is already an application running on port $APP_PORT. Please stop it before running another one on the same port."
    return 1
  fi
}

start_application() {
  run_output_file="$OUTPUT_FOLDER/log/$JVM_IDENTIFIER-run-$TEST_RUN_IDENTIFIER.log"

  if [ "$JVM_IDENTIFIER" != "native-image" ]; then
    RUN_CMD="$JAVA_HOME/bin/java $JAVA_OPS $JFR_OPS -jar $APP_HOME/target/*.jar"
  else
    RUN_CMD="$APP_HOME/target/spring-petclinic $JAVA_OPS"
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

time_to_first_response() {
  # Set the timeout threshold (in seconds)
  # Note: the SECONDS is a built-in variable in Bash that represents the number of seconds since the shell was started.
  timeout=30
  end_time=$((SECONDS + timeout))

  # Wait until the application answers to the first request or timeout occurs
  while [[ "$(curl -s -o /dev/null -w ''%{http_code}'' http://$APP_BASE_URL/owners?lastName=)" != "200" ]]; do
    sleep .00001

    # Check if the timeout has been reached
    if [ $SECONDS -ge $end_time ]; then
      echo "ERROR: The application did not respond within $timeout seconds (timeout)."
      return 1
    fi
  done
}

check_command_line_options "$@"
if [ $? -ne 0 ]; then
  exit 1
fi

echo ""
echo "+=================================+"
echo "| [1/10] Configuration Properties |"
echo "+=================================+"
. ../../scripts/shell/configure-properties.sh || exit 1

echo ""
echo "+==============================+"
echo "| [2/10] Hardware Architecture |"
echo "+==============================+"
. ../../scripts/shell/configure-arch.sh

echo ""
echo "+=========================+"
echo "| [3/10] OS Configuration |"
echo "+=========================+"
. ../../scripts/shell/configure-os.sh || exit 1
. ../../scripts/shell/configure-os-"$OS".sh

echo ""
echo "+==========================+"
echo "| [4/10] JVM Configuration |"
echo "+==========================+"
. ../../scripts/shell/configure-jvm.sh || exit 1

echo ""
echo "+==================================+"
echo "| [5/10] Application configuration |"
echo "+==================================+"
configure_application

# make sure the output resources (e.g., folders and files) exist
create_output_resources

echo ""
echo "+==============================+"
echo "| [6/10] Build the application |"
echo "+==============================+"
if [ "$APP_SKIP_BUILD" == "--skip-build" ]; then
  echo "WARNING: Skipping the build process. A previously generated artifact will be used to start the application."
else
  build_application || exit 1
fi

echo ""
echo "+========================================================+"
echo "| [7/10] Start the system power consumption measurements |"
echo "+========================================================+"
. ../../scripts/shell/system-power-consumption-os-$OS.sh
power_output_file="$OUTPUT_FOLDER/power/$JVM_IDENTIFIER-run-$TEST_RUN_IDENTIFIER.txt"
start_power_consumption --background --output-file="$power_output_file" || exit 1

echo ""
echo "+==============================+"
echo "| [8/10] Start the application |"
echo "+==============================+"
check_application_port || { stop_power_consumption && exit 1; }
start_application || { stop_power_consumption && exit 1; }
time_to_first_response || { stop_power_consumption && exit 1; }

# reset the terminal line settings, otherwise (sometimes) it gets a wired indentation
stty sane
echo "Application with PID $APP_PID successfully started at $(date)."

echo ""
echo "+=================================================+"
echo "| [8/10] Start the process performance monitoring |"
echo "+=================================================+"
. ../../scripts/shell/process-performance-monitoring-os-$OS.sh
performance_monitoring_output_file="$OUTPUT_FOLDER/perf/$JVM_IDENTIFIER-run-$TEST_RUN_IDENTIFIER.txt"
start_process_performance_monitoring --pid="$APP_PID" --output-file="$performance_monitoring_output_file" --duration="$APP_RUNNING_TIME" || exit 1

# wait for the application to run for specified duration
echo ""
echo "Please enjoy a coffee ☕ while the application runs. This may take approximately $APP_RUNNING_TIME seconds..."
sleep "$APP_RUNNING_TIME"

echo ""
echo "+================================================+"
echo "| [9/10] Stop the process performance monitoring |"
echo "+================================================+"
stop_process_performance_monitoring

echo ""
echo "+=============================+"
echo "| [9/10] Stop the application |"
echo "+=============================+"
echo "Stopping the application with PID $APP_PID."
kill -TERM "$APP_PID"
echo "Application with PID $APP_PID successfully stopped at $(date)."

echo ""
echo "+========================================================+"
echo "| [10/10] Stop the system power consumption measurements |"
echo "+========================================================+"
stop_power_consumption

# give a bit of time to the process to gracefully shut down
sleep 5

echo ""
echo "*** Test $TEST_RUN_IDENTIFIER successfully finished! ***"