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
  if [ $# -eq 0 ]; then
    echo "Usage: ./plot-results <jdk-version> [<os>] [<arch>]"
    echo ""
    echo "Options:"
    echo "  jdk-version   java version identifier for the generated results. The supported values are {21}"
    echo "  os            operating system. The supported values are {linux, windows, mac}. The default value is linux"
    echo "  arch          target architecture. The supported values are {x86_64, arm64}. The default value is detected based on the current target architecture."
    echo ""
    echo "Examples:"
    echo "  ./plot-results 21"
    echo "  ./plot-results 21 linux x86_64"
    echo ""
    return 1
  fi

  if [ "$1" ]; then
    export JDK_VERSION="$1"
  fi

  if [ "$2" ]; then
    export OS="$2"
  else
    export OS="linux"
  fi

  if [ "$3" ]; then
    export ARCH="$2"
  else
    export ARCH=$(uname -m)
  fi
}

set_environment_variables() {
  export OUTPUT_FOLDER=results/jdk-$JDK_VERSION/$ARCH/$OS

  echo "Output folder: $OUTPUT_FOLDER"
}

check_folder_exists() {
  folder="$1"
  if [ ! -d "$folder" ]; then
    echo ""
    echo "ERROR: Folder $folder do not exist, unable to continue!"
    return 1
  fi
}

plot_results() {
  R <./ggplot2/plot-result.r --no-save --args "$(pwd)" "$OUTPUT_FOLDER"
  if [ $? -ne 0 ]; then
    echo ""
    echo "ERROR: Error encountered while plotting results, unable to continue!"
    return 1
  fi

  echo ""
  echo "Plots successfully generated."
}

echo ""
echo "+========================+"
echo "| Results Plot Generator |"
echo "+========================+"
check_command_line_options "$@"
if [ $? -ne 0 ]; then
  exit 1
fi

echo ""
echo "+-----------------------+"
echo "| Environment variables |"
echo "+-----------------------+"
set_environment_variables

echo ""
echo "+-------------------+"
echo "| Plot test results |"
echo "+-------------------+"
plot_results