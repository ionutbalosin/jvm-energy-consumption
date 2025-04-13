#!/bin/bash
#
# JVM Energy Consumption
#
# Copyright (C) 2023-2025 Ionut Balosin
# Website:      www.ionutbalosin.com
# Social Media:
#   LinkedIn:   ionutbalosin
#   Bluesky:    @ionutbalosin.bsky.social
#   X:          @ionutbalosin
#   Mastodon:   ionutbalosin@mastodon.social
#
# MIT License
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
  JDK_VERSION="21"
  OS="linux"
  ARCH=$(uname -m)

  if [ $# -gt 2 ]; then
    echo "Usage: ./plot-summary-reports.sh [--os=<os>] [--arch=<arch>]"
    echo ""
    echo "Options:"
    echo "  --os=<os>     An optional parameter to specify the operating system. Supported values are {linux, windows, mac}. Default value is $OS."
    echo "  --arch=<arch> An optional parameter to specify the target architecture. Supported values are {x86_64, arm64}. Default value $ARCH is detected based on the current architecture."
    echo ""
    echo "Examples:"
    echo "  ./plot-summary-reports.sh"
    echo "  ./plot-summary-reports.sh --os=linux --arch=x86_64"
    echo ""
    return 1
  fi

  while [ $# -gt 0 ]; do
    case "$1" in
      --os=*)
        OS="${1#*=}"
        ;;
      --arch=*)
        ARCH="${1#*=}"
        ;;
      *)
        echo "ERROR: Unknown parameter $1"
        return 1
        ;;
    esac
    shift
  done
}

set_environment_variables() {
  BASE_DIR="$(pwd)/.."
  OUTPUT_FOLDER=results/jdk-$JDK_VERSION/$ARCH/$OS

  echo "JDK version: $JDK_VERSION"
  echo "Operating system: $OS"
  echo "Hardware Architecture: $ARCH"
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
  R < ../scripts/ggplot2/plot-summary-reports.r --no-save \
      --args "$BASE_DIR" "$OUTPUT_FOLDER" "$JDK_VERSION" "$ARCH" \
      "$OPENJDK_HOTSPOT_VM_IDENTIFIER" "$GRAAL_VM_CE_IDENTIFIER" "$ORACLE_GRAAL_VM_IDENTIFIER" "$GRAAL_VM_NATIVE_IMAGE_IDENTIFIER" "$AZUL_PRIME_VM_IDENTIFIER" "$ECLIPSE_OPEN_J9_VM_IDENTIFIER" \
      "$OPENJDK_HOTSPOT_VM_NAME" "$GRAAL_VM_CE_NAME" "$ORACLE_GRAAL_VM_NAME" "$GRAAL_VM_NATIVE_IMAGE_NAME" "$AZUL_PRIME_VM_NAME" "$ECLIPSE_OPEN_J9_VM_NAME" \
      "$OPENJDK_HOTSPOT_VM_COLOR_PALETTE" "$GRAAL_VM_CE_COLOR_PALETTE" "$ORACLE_GRAAL_VM_COLOR_PALETTE" "$GRAAL_VM_NATIVE_IMAGE_COLOR_PALETTE" "$AZUL_PRIME_VM_COLOR_PALETTE" "$ECLIPSE_OPEN_J9_COLOR_PALETTE";
  if [ $? -ne 0 ]; then
    echo ""
    echo "ERROR: Error encountered while plotting results, unable to continue!"
    return 1
  fi

  echo ""
  echo "Plots successfully generated."
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

echo ""
echo "+=============================+"
echo "| [4/7] Environment variables |"
echo "+=============================+"
set_environment_variables

echo ""
echo "+=========================+"
echo "| [4/7] Plot test results |"
echo "+=========================+"
plot_results