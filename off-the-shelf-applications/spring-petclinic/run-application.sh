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
    echo "Usage: sudo ./run-application.sh <test-run-identifier> [--skip-build]"
    echo ""
    echo "Options:"
    echo "  test-run-identifier  is a mandatory parameter to identify the current execution test."
    echo "  --skip-build         is an optional parameter to skip the build process."
    echo ""
    echo "Examples:"
    echo "   $ sudo ./run-application.sh 1"
    echo "   $ sudo ./run-application.sh 1 --skip-build"
    echo ""
    return 1
  fi

  if [ "$1" ]; then
    export TEST_RUN_IDENTIFIER="$1"
  fi
}

configure_application() {
  export CURR_DIR=$(pwd)
  export APP_HOME=/home/ionutbalosin/Workspace/spring-petclinic
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
  if [ "$JVM_IDENTIFIER" != "native-image" ]; then
    export BUILD_CMD="./mvnw clean package -Dmaven.test.skip"
  else
    export BUILD_CMD="./mvnw -Pnative clean native:compile -Dmaven.test.skip"
  fi

  echo "$BUILD_CMD"
  cd $APP_HOME
  sudo -E perf stat -a \
    -e "power/energy-cores/" \
    -e "power/energy-gpu/" \
    -e "power/energy-pkg/" \
    -e "power/energy-psys/" \
    -e "power/energy-ram/" \
    -o $CURR_DIR/$OUTPUT_FOLDER/perf/$JVM_IDENTIFIER-build-$TEST_RUN_IDENTIFIER.stats \
    $BUILD_CMD >$CURR_DIR/$OUTPUT_FOLDER/logs/$JVM_IDENTIFIER-build-$TEST_RUN_IDENTIFIER.log 2>&1
  cd -
}

start_application() {
  if [ "$JVM_IDENTIFIER" != "native-image" ]; then
    export RUN_CMD="$JAVA_HOME/bin/java $JAVA_OPS $JFR_OPS -jar $APP_HOME/target/*.jar"
  else
    export RUN_CMD="$APP_HOME/target/spring-petclinic $JAVA_OPS"
  fi

  echo ""
  echo "Command line: $RUN_CMD"

  echo ""
  read -r -p "If the above configuration is correct, press ENTER to continue or CRTL+C to abort ... "

  echo "Starting the application ... "
  sudo perf stat -a \
    -e "power/energy-cores/" \
    -e "power/energy-gpu/" \
    -e "power/energy-pkg/" \
    -e "power/energy-psys/" \
    -e "power/energy-ram/" \
    -o $OUTPUT_FOLDER/perf/$JVM_IDENTIFIER-run-$TEST_RUN_IDENTIFIER.stats \
    $RUN_CMD >$OUTPUT_FOLDER/logs/$JVM_IDENTIFIER-run-$TEST_RUN_IDENTIFIER.log 2>&1 &

  export APP_PID=$!
}

time_to_first_response() {
  # wait until the application answers to the first request
  while [[ "$(curl -s -o /dev/null -w ''%{http_code}'' http://$APP_BASE_URL/owners?lastName=)" != "200" ]]; do
    sleep .00001
  done
}

check_command_line_options "$@"
if [ $? -ne 0 ]; then
  exit 1
fi

echo ""
echo "+========================+"
echo "| [1/6] OS configuration |"
echo "+========================+"
. ../configure-os.sh

echo ""
echo "+=========================+"
echo "| [2/6] JVM configuration |"
echo "+=========================+"
. ../configure-jvm.sh

echo ""
echo "+=================================+"
echo "| [3/6] Application configuration |"
echo "+=================================+"
configure_application

# make sure the output resources (e.g., folders and files) exist
create_output_resources

echo ""
echo "+=============================+"
echo "| [4/6] Build the application |"
echo "+=============================+"
if [ "$2" == "--skip-build" ]; then
  echo "WARNING: Skip building the application. A previously generated artifact will be used to start the application."
else
  build_application
fi

echo ""
echo "+=============================+"
echo "| [5/6] Start the application |"
echo "+=============================+"
start_application

time_to_first_response

# reset the terminal line settings, otherwise it gets a wired indentation
stty sane

echo "Application with pid=$APP_PID successfully started at: $(date) and it will be running for about $APP_RUNNING_TIME sec"
echo ""

sleep $APP_RUNNING_TIME

echo ""
echo "+============================+"
echo "| [6/6] Stop the application |"
echo "+============================+"
echo "Stop the application with pid=$APP_PID"
sudo kill -INT $APP_PID
echo "Application with pid=$APP_PID successfully stopped at: $(date)"

# give a bit of time to the process to gracefully shut down
sleep 10

echo ""
echo "*** Test $TEST_RUN_IDENTIFIER successfully finished! ***"
