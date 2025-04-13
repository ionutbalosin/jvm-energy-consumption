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

disable_aslr() {
  aslr="/proc/sys/kernel/randomize_va_space"
  echo "Current ASLR configuration: $(cat "$aslr")"
  if [ "$DRY_RUN" != "--dry-run" ]; then
    echo "Disabling ASLR..."
    echo 0 >"$aslr"
  fi
  echo "New ASLR configuration: $(cat "$aslr")"
}

configure_aslr() {
  while :; do
    read -p "Do you want to disable ASLR? (yes/no) " INPUT_KEY
    case $INPUT_KEY in
    yes)
      disable_aslr
      break
      ;;
    no)
      break
      ;;
    *)
      echo "Sorry, I don't understand. Please try again!"
      ;;
    esac
  done
}

disable_turbo_boost() {
  # Intel
  no_turbo="/sys/devices/system/cpu/intel_pstate/no_turbo"
  if [ -f "$no_turbo" ]; then
    echo "Current turbo boost configuration for Intel: $(cat "$no_turbo")"
    if [ "$DRY_RUN" != "--dry-run" ]; then
      echo "Enabling turbo boost..."
      echo 1 >"$no_turbo"
    fi
    echo "New turbo boost configuration: $(cat "$no_turbo")"
  fi
  # AMD
  boost="/sys/devices/system/cpu/cpufreq/boost"
  if [ -f "$boost" ]; then
    echo "Current turbo boost configuration for AMD: $(cat "$no_turbo")"
    if [ "$DRY_RUN" != "--dry-run" ]; then
      echo "Disabling turbo boost..."
      echo 0 >"$boost"
    fi
    echo "New turbo boost configuration: $(cat "$boost")"
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
      echo "Sorry, I don't understand. Please try again!"
      ;;
    esac
  done
}

set_scaling_governor() {
  cat /sys/bus/cpu/drivers/processor/cpu*/cpufreq/affected_cpus | grep -v '^[[:space:]]*$' | while IFS= read -r cpu; do
    echo "Available CPU$cpu governors: $(cat /sys/bus/cpu/drivers/processor/cpu$cpu/cpufreq/scaling_available_governors)"
    if [ "$DRY_RUN" != "--dry-run" ]; then
      echo "Setting CPU$cpu governor to performance..."
      echo "performance" >"/sys/devices/system/cpu/cpu$cpu/cpufreq/scaling_governor"
    fi
    echo "New CPU$cpu governor configuration: $(cat /sys/devices/system/cpu/cpu$cpu/cpufreq/scaling_governor)"
    echo ""
  done
}

configure_scaling_governor() {
  while :; do
    read -p "Do you want to set the CPU governor to performance? (yes/no) " INPUT_KEY
    case $INPUT_KEY in
    yes)
      set_scaling_governor
      break
      ;;
    no)
      break
      ;;
    *)
      echo "Sorry, I don't understand. Please try again!"
      ;;
    esac
  done
}

disable_hyper_threading() {
  siblings=$(grep -F , /sys/devices/system/cpu/cpu*/topology/thread_siblings_list | cut -d, -f2 | sort -u)
  if [ "$siblings" == "" ]; then
    echo "WARNING: No logical CPU siblings found. Hyper-threading may already be disabled."
  else
    for sibling in $siblings; do
      cpu="/sys/devices/system/cpu/cpu$sibling/online"
      echo "Current hyper-threading configuration for logical CPU$sibling: $(cat "$cpu")"
      if [ "$DRY_RUN" != "--dry-run" ]; then
        echo "Disabling hyper-threading configuration for logical CPU$sibling..."
        echo 0 >"$cpu"
      fi
      echo "New hyper-threading configuration for logical CPU$sibling: $(cat "$cpu")"
      echo ""
    done
  fi
}

configure_hyper_threading() {
  while :; do
    read -p "Do you want to disable CPU hyper-threading? (yes/no) " INPUT_KEY
    case $INPUT_KEY in
    yes)
      disable_hyper_threading
      break
      ;;
    no)
      break
      ;;
    *)
      echo "Sorry, I don't understand. Please try again!"
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
        echo "WARNING: OS configuration requires sudo admin rights (e.g., 'sudo ./run-benchmarks.sh'). Otherwise, the configuration may fail."
        read -r -p "Press ENTER to continue, or CTRL+C to abort ... "
      fi
      return 0
      ;;
    no)
      return 1
      ;;
    *)
      echo "Sorry, I don't understand. Please try again!"
      ;;
    esac
  done
}

DRY_RUN="$1"

echo ""
echo "+-------------------+"
echo "| Linux OS Settings |"
echo "+-------------------+"
echo "Summary:"
echo " - For reliable benchmarking and consistent measurements, a proper OS configuration is crucial. However, the effectiveness can vary across different operating systems."
echo " - This includes:"
echo "   - Disabling address space layout randomization (ASLR)"
echo "   - Disabling turbo boost mode"
echo "   - Setting the CPU governor to performance"
echo "   - Disabling CPU hyper-threading"
echo "WARNING: The current configuration has been tested on a Debian-based Linux distribution (e.g., Ubuntu)."
echo ""
confirm_os_settings
if [ $? -ne 0 ]; then
  return 1
fi

echo ""
echo "+---------------------------------------------------+"
echo "| Disable Address Space Layout Randomization (ASLR) |"
echo "+---------------------------------------------------+"
echo "ASLR - a security technique that randomizes memory addresses to prevent exploitation of memory corruption vulnerabilities."
echo "WARNING: Disabling ASLR is optional and mostly preferable for local testing environments to obtain consistent measurements."
echo ""
configure_aslr

echo ""
echo "+--------------------------+"
echo "| Disable Turbo Boost Mode |"
echo "+--------------------------+"
echo "Turbo Boost Mode - increases CPU frequency during demanding tasks."
echo "WARNING: Disabling turbo boost is recommended for consistent measurements."
echo ""
configure_turbo_boost

echo ""
echo "+---------------------------------+"
echo "| Set CPU Governor to Performance |"
echo "+---------------------------------+"
echo "CPU Governor to Performance - ensures maximum frequency and avoids underclocking."
echo "WARNING: Setting the CPU governor to performance is recommended for consistent measurements."
echo ""
configure_scaling_governor

echo ""
echo "+-----------------------------+"
echo "| Disable CPU Hyper-Threading |"
echo "+-----------------------------+"
echo "CPU Hyper-Threading - enhances parallelization by allowing one physical core to handle two threads."
echo "In general, the CPU's architectural state is replicated, not the execution resources."
echo "WARNING: Disabling hyper-threading is recommended for consistent measurements."
echo ""
configure_hyper_threading
