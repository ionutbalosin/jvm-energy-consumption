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
  if [[ $# -lt 1 || $# -gt 2 ]]; then
    echo "Usage: ./run-hyperfoil.sh --run-identifier=<run-identifier> [--jvm-identifier=<jvm-identifier>]"
    echo ""
    echo "Options:"
    echo "  --run-identifier=<run-identifier>  A mandatory parameter to identify the current execution run."
    echo "  --jvm-identifier=<jvm-identifier>  An optional parameter to specify the JVM to run with. If not specified, the user will be prompted to select it at the beginning of the run."
    echo "                                     Accepted options: {openjdk-hotspot-vm, graalvm-ce, oracle-graalvm, native-image, azul-prime-vm, eclipse-openj9-vm}."
    echo ""
    echo "Examples:"
    echo "  $ ./run-hyperfoil.sh --run-identifier=1"
    echo "  $ ./run-hyperfoil.sh --run-identifier=1 --jvm-identifier=openjdk-hotspot-vm"
    echo ""
    return 1
  fi

  APP_RUN_IDENTIFIER=""
  APP_JVM_IDENTIFIER=""

  while [ $# -gt 0 ]; do
    case "$1" in
      --run-identifier=*)
        APP_RUN_IDENTIFIER="${1#*=}"
        ;;
      --jvm-identifier=*)
        APP_JVM_IDENTIFIER="${1#*=}"
        ;;
      *)
        echo "ERROR: Unknown parameter $1"
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

configure_hyperfoil() {
  CURR_DIR=$(pwd)

  echo ""
  echo "Hyperfoil home: $HYPERFOIL_HOME"
  echo "Application run identifier: $APP_RUN_IDENTIFIER"
  echo "JVM identifier: $APP_JVM_IDENTIFIER"

  if [ ! -x "$JAVA_HOME"/bin/java ]; then
    echo ""
    echo "ERROR: Unable to execute the '$JAVA_HOME/bin/java' command. Cannot proceed!"
    return 1
  fi
}

create_output_resources() {
  mkdir -p $OUTPUT_FOLDER/hreports
}

start_hyperfoil() {
  $HYPERFOIL_HOME/bin/cli.sh <<EOF
start-local
upload test-plan.hf.yaml
run test-plan-benchmark
report --destination=$CURR_DIR/$OUTPUT_FOLDER/hreports/$JVM_IDENTIFIER-run-$APP_RUN_IDENTIFIER.html
exit
EOF
}

check_command_line_options "$@"
if [ $? -ne 0 ]; then
  exit 1
fi

echo ""
echo "+================================+"
echo "| [1/4] Configuration Properties |"
echo "+================================+"
. ../../scripts/shell/configure-properties.sh || exit 1

echo ""
echo "+=============================+"
echo "| [2/4] Hardware Architecture |"
echo "+=============================+"
. ../../scripts/shell/configure-arch.sh

echo ""
echo "+=========================+"
echo "| [3/5] JVM Configuration |"
echo "+=========================+"
. ../../scripts/shell/configure-jvm.sh "$APP_JVM_IDENTIFIER" || exit 1

echo ""
echo "+===============================+"
echo "| [4/5] Hyperfoil configuration |"
echo "+===============================+"
configure_hyperfoil || exit 1

# make sure the output resources (e.g., folders and files) exist
create_output_resources

echo ""
echo "+=======================+"
echo "| [5/5] Start Hyperfoil |"
echo "+=======================+"
echo "IMPORTANT: The following commands will be automatically executed in the CLI to trigger the load test, save the report, and exit at the end."
echo "$ start-local"
echo "$ upload test-plan.hf.yaml"
echo "$ run test-plan-benchmark"
echo "$ report --destination=$CURR_DIR/$OUTPUT_FOLDER/hreports/$JVM_IDENTIFIER-run-$APP_RUN_IDENTIFIER.html"
echo "$ exit"
echo ""
echo "Please enjoy a coffee ☕ while the application runs. This may take some time ..."
echo ""
start_hyperfoil

echo "Bye bye! 👋"