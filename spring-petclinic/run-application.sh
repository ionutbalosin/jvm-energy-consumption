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

configure_application() {
  export APP_HOME=/home/ionutbalosin/Workspace/spring-petclinic
  export APP_BASE_URL=localhost:8080
  export JAVA_OPS="-Xms1m -Xmx4g"
  export APP_RUNNING_TIME=30

  echo ""
  echo "Application home: $APP_HOME"
  echo "Application base url: $APP_BASE_URL"
  echo "Application running time: $APP_RUNNING_TIME sec"
  echo "Java opts: $JAVA_OPS"
  echo "Test number: $TEST_RUN_NO"

  echo ""
  read -r -p "If the above configuration is correct, press ENTER to continue or CRTL+C to abort ... "
}

create_output_folders() {
  mkdir -p ${OUTPUT_FOLDER}/perf
  mkdir -p ${OUTPUT_FOLDER}/logs
}

chmod_output_folders() {
  sudo chmod 777 ${OUTPUT_FOLDER}/perf/*
  sudo chmod 777 ${OUTPUT_FOLDER}/logs/*
}

build_application() {
  if [ "$JVM_NAME" != "native-image" ]; then
    export BUILD_CMD="./mvnw clean package -Dmaven.test.skip"
  else
    export BUILD_CMD="./mvnw -Pnative clean native:compile -Dmaven.test.skip"
  fi

  echo "${BUILD_CMD}"
  cd ${APP_HOME} && ${BUILD_CMD}
  cd -
}

start_application() {
  if [ "$JVM_NAME" != "native-image" ]; then
    export RUN_CMD="${JAVA_HOME}/bin/java ${JAVA_OPS} -jar ${APP_HOME}/target/*.jar"
  else
    export RUN_CMD="${APP_HOME}/target/spring-petclinic ${JAVA_OPS}"
  fi

  echo "${RUN_CMD}"
  sudo perf stat -a \
    -e "power/energy-cores/" \
    -e "power/energy-gpu/" \
    -e "power/energy-pkg/" \
    -e "power/energy-psys/" \
    -e "power/energy-ram/" \
    -o ${OUTPUT_FOLDER}/perf/${JVM_IDENTIFIER}-run${TEST_RUN_NO}.stats \
    ${RUN_CMD} > ${OUTPUT_FOLDER}/logs/${JVM_IDENTIFIER}-run${TEST_RUN_NO}.log 2>&1 &

  export APP_PID=$!
}

time_to_first_response() {
  # wait until the application answers to the first request
  while [[ "$(curl -s -o /dev/null -w ''%{http_code}'' http://${APP_BASE_URL}/owners/find)" != "200" ]]; do
    sleep .00001
  done
}

TEST_RUN_NO="$1"

if [[ $EUID != 0 ]]; then
  echo "ERROR: sudo admin rights are needed (e.g., $ sudo ./run-application.sh [test_number])"
  echo ""
  echo "Example:"
  echo "   $ sudo ./run-application.sh 1"
  exit 1
fi

echo ""
echo "+=========================+"
echo "| [1/6] JVM configuration |"
echo "+=========================+"
. ../configure-jvm.sh

echo ""
echo "+=================================+"
echo "| [2/6] Application configuration |"
echo "+=================================+"
configure_application

# make sure the output folders exist
create_output_folders

echo ""
echo "+=============================+"
echo "| [3/6] Build the application |"
echo "+=============================+"
build_application

echo ""
echo "+=============================+"
echo "| [4/6] Start the application |"
echo "+=============================+"
start_application

time_to_first_response
stty sane
echo "Application with pid=$APP_PID successfully started"

echo ""
echo "+==============================+"
echo "| [5/6] Start the load testing |"
echo "+==============================+"
echo "Keep the application with pid=$APP_PID running for about $APP_RUNNING_TIME sec"
echo "The load test must be triggered during this time interval"
sleep $APP_RUNNING_TIME

echo ""
echo "+============================+"
echo "| [6/6] Stop the application |"
echo "+============================+"
echo "Stop the application with pid=$APP_PID"
sudo kill -INT $APP_PID

# give a bit of time to the process to gracefully shut down
sleep 10

# assign read/write permissions to the output files
chmod_output_folders

echo ""
echo "*** Test $TEST_RUN_NO successfully finished! ***"