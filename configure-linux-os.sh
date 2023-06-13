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

set_environment_variables() {
  export ARCH="$(uname -m)"

  echo "Operating system: Linux OS"
  echo "Architecture: $ARCH"
  echo ""
  read -r -p "If the above configuration is correct, press ENTER to continue or CRTL+C to abort ... "
}

disable_turbo_boost() {
  # Intel
  no_turbo="/sys/devices/system/cpu/intel_pstate/no_turbo"
  if [ -f $no_turbo ]; then
    echo "Current turbo boost configuration for Intel: "$(cat "$no_turbo")
    echo "Enabling turbo boost ..."
    echo 1 >$no_turbo
    echo "New turbo boost configuration: "$(cat "$no_turbo")
  fi
  # AMD
  boost="/sys/devices/system/cpu/cpufreq/boost"
  if [ -f $boost ]; then
    echo "Current turbo boost configuration for AMD: "$(cat "$no_turbo")
    echo "Disabling turbo boost ..."
    echo 0 >$boost
    echo "New turbo boost configuration: "$(cat "$boost")
  fi
}

configure_turbo_boost() {
  while :; do
    read -p "Do you want to disable turbo boost mode? (yes/no) " INPUT_KEY
    case $INPUT_KEY in
    yes)
      disable_turbo_boost
      break
      ;;
    no)
      break
      ;;
    *)
      echo "Sorry, I don't understand. Try again!"
      ;;
    esac
  done
}

disable_hyper_threading() {
  siblings=$(grep -F , /sys/devices/system/cpu/cpu*/topology/thread_siblings_list | cut -d, -f2 | sort -u)
  if [ "$siblings" == "" ]; then
    echo "WARNING: no logical CPU(s) siblings found. Hyper-threading was probably disabled."
  else
    for sibling in $siblings; do
      cpu="/sys/devices/system/cpu/cpu$sibling/online"
      echo "Current logical CPU$sibling hyper-threading configuration: "$(cat "$cpu")
      echo "Disabling logical CPU$sibling hyper-threading configuration ..."
      echo 0 >$cpu
      echo "New logical CPU$sibling hyper-threading configuration: "$(cat "$cpu")
      echo ""
    done
  fi
}

configure_hyper_threading() {
  while :; do
    read -p "Do you want to disable the CPU hyper-threading? (yes/no) " INPUT_KEY
    case $INPUT_KEY in
    yes)
      disable_hyper_threading
      break
      ;;
    no)
      break
      ;;
    *)
      echo "Sorry, I don't understand. Try again!"
      ;;
    esac
  done
}

confirm_os_settings() {
  while :; do
    read -r -p "Do you want to proceed with the OS configuration settings? (yes/no) " INPUT_KEY
    case $INPUT_KEY in
    yes)
      if [[ $EUID != 0 ]]; then
        echo ""
        echo "WARNING: OS configuration requires sudo admin rights, otherwise the configuration is not possible."
        read -r -p "Press ENTER to continue or CRTL+C to abort ... "
      fi
      return 0
      ;;
    no)
      return 1
      ;;
    *)
      echo "Sorry, I don't understand. Try again!"
      ;;
    esac
  done
}

echo ""
echo "+--------------------------+"
echo "| OS environment variables |"
echo "+--------------------------+"
set_environment_variables

echo ""
echo "+-----------------------+"
echo "| OS benchmark settings |"
echo "+-----------------------+"
echo "In summary:"
echo " - for benchmarking to reduce, as much as possible, the noise and to get more consistent measurements a proper OS configuration is very important. Nevertheless, how to do that is very OS dependent and it might not be sufficient since it does not exclude the measurement bias."
echo " - these settings includes:"
echo "   - disabling the turbo boost mode"
echo "   - disabling the CPU hyper-threading"
echo "WARNING: the current configuration relies and it was tested on a Debian-based Linux distro (e.g., Ubuntu)."
echo ""
confirm_os_settings
if [ $? -ne 0 ]; then
  return 1
fi

echo ""
echo "+--------------------------+"
echo "| Disable turbo boost mode |"
echo "+--------------------------+"
echo "turbo boost mode - raises CPU operating frequency when demanding tasks are running"
echo "WARNING: we recommend disabling it to receive more consistent measurements"
echo ""
configure_turbo_boost

echo ""
echo "+-----------------------------+"
echo "| Disable CPU hyper-threading |"
echo "+-----------------------------+"
echo "CPU hyper-threading - improves parallelization of computations so that one physical core can have two simultaneous threads of execution"
echo "In general, the CPU architectural state (e.g., registers) is replicated but not the execution resources (e.g., ALUs, caches, etc.)"
echo "WARNING: we recommend disabling it to receive more consistent measurements"
echo ""
configure_hyper_threading
