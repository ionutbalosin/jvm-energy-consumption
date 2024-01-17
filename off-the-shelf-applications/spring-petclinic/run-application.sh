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
  if [[ $EUID -ne 0 || ($# -ne 1 && $# -ne 2) ]]; then
    echo "Usage: sudo ./run-application.sh <test-run-identifier> [--skip-build]"
    echo ""
    echo "Options:"
    echo "  test-run-identifier   A mandatory parameter to identify the current execution test."
    echo "  --skip-build          An optional parameter to skip the build process."
    echo ""
    echo "Examples:"
    echo "   $ sudo ./run-application.sh 1"
    echo "   $ sudo ./run-application.sh 1 --skip-build"
    echo ""
    return 1
  fi

  export TEST_RUN_IDENTIFIER="$1"
}

configure_application() {
  export CURR_DIR=$(pwd)
  export APP_HOME=$SPRING_PETCLINIC_HOME
  export APP_BASE_URL=localhost:8080
  export APP_RUNNING_TIME=900
  export JAVA_OPS="-Xms1m -Xmx1g"
  # export JFR_OPS="-XX:StartFlightRecording=duration=$APP_RUNNING_TIMEs,filename=$OUTPUT_FOLDER/jfr/$JVM_IDENTIFIER-run-$TEST_RUN_IDENTIFIER.jfr"

  echo ""
  echo "Application home: $APP_HOME"
  echo "Application base url: $APP_BASE_URL"
  echo "Application running time: $APP_RUNNING_TIME sec"
  echo "Java opts: $JAVA_OPS"
  echo "Test run identifier: $TEST_RUN_IDENTIFIER"
  echo "JFR opts: $JFR_OPS"

  echo ""
  read -r -p "If the above configuration is correct, press ENTER to continue or CRTL+C to abort ... "
}

create_output_resources() {
  mkdir -p $OUTPUT_FOLDER/perf
  mkdir -p $OUTPUT_FOLDER/logs
  mkdir -p $OUTPUT_FOLDER/jfr
}

build_application() {
  build_output_file="$CURR_DIR/$OUTPUT_FOLDER/logs/$JVM_IDENTIFIER-build-$TEST_RUN_IDENTIFIER.log"
  stats_output_file="$CURR_DIR/$OUTPUT_FOLDER/perf/$JVM_IDENTIFIER-build-$TEST_RUN_IDENTIFIER"
  PREFIX_COMMAND="${OS_PREFIX_COMMAND/((statsOutputFile))/$stats_output_file}"

  if [ "$JVM_IDENTIFIER" != "native-image" ]; then
    export BUILD_CMD="$APP_HOME/mvnw -f $APP_HOME/pom.xml clean package -Dmaven.test.skip"
  else
    export BUILD_CMD="$APP_HOME/mvnw -f $APP_HOME/pom.xml clean package -Dmaven.test.skip -Pnative native:compile"
  fi

  echo "Building application at: $(date) ... "
  echo "$PREFIX_COMMAND $BUILD_CMD"
  echo ""
  eval "$PREFIX_COMMAND $BUILD_CMD" > "$build_output_file" 2>&1
  if [ $? -ne 0 ]; then
    echo "ERROR: Build failed for application. Check $build_output_file for details."
    return 1
  fi
}

start_application() {
  run_output_file="$OUTPUT_FOLDER/logs/$JVM_IDENTIFIER-run-$TEST_RUN_IDENTIFIER.log"
  stats_output_file="$OUTPUT_FOLDER/perf/$JVM_IDENTIFIER-run-$TEST_RUN_IDENTIFIER"
  PREFIX_COMMAND="${OS_PREFIX_COMMAND/((statsOutputFile))/$stats_output_file}"

  if [ "$JVM_IDENTIFIER" != "native-image" ]; then
    export RUN_CMD="$JAVA_HOME/bin/java $JAVA_OPS $JFR_OPS -jar $APP_HOME/target/*.jar"
  else
    export RUN_CMD="$APP_HOME/target/spring-petclinic $JAVA_OPS"
  fi

  echo "Running application at: $(date) ... "
  echo "$PREFIX_COMMAND $RUN_CMD"
  echo ""
  eval "$PREFIX_COMMAND $RUN_CMD" > "$run_output_file" 2>&1 &
  if [ $? -ne 0 ]; then
    echo "ERROR: Run failed for application. Check $run_output_file for details."
    return 1
  fi

  export APP_PID=$!
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
. ../../scripts/shell/configure-os-$OS.sh

echo ""
echo "+=========================+"
echo "| [4/8] JVM Configuration |"
echo "+=========================+"
. ../../scripts/shell/configure-jvm.sh || exit 1

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
if [ "$2" == "--skip-build" ]; then
  echo "WARNING: Skipping the build process. A previously generated artifact will be used to start the application."
else
  build_application || exit 1
fi

echo ""
echo "+=============================+"
echo "| [7/8] Start the application |"
echo "+=============================+"
start_application || exit 1
time_to_first_response || exit 1

# reset the terminal line settings, otherwise it gets a wired indentation
stty sane

echo "Application with PID $APP_PID successfully started at $(date). It will be running for approximately $APP_RUNNING_TIME seconds."
echo ""
sleep $APP_RUNNING_TIME

echo ""
echo "+============================+"
echo "| [8/8] Stop the application |"
echo "+============================+"
echo "Stopping the application with PID $APP_PID."
sudo kill -INT $APP_PID
echo "Application with PID $APP_PID successfully stopped at $(date)."

# give a bit of time to the process to gracefully shut down
sleep 10

echo ""
echo "*** Test $TEST_RUN_IDENTIFIER successfully finished! ***"
