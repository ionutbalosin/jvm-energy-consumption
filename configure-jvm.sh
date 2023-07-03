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

configure_openjdk() {
  export JAVA_HOME=/usr/lib/jvm/adoptium-temurin-jdk-17.0.7+7
  export JVM_NAME="OpenJDK HotSpot VM"
  export JVM_IDENTIFIER="openjdk-hotspot-vm"
}

configure_graalvm_ee() {
  export JAVA_HOME=/usr/lib/jvm/graalvm-ee-jdk-17.0.7+8-LTS-jvmci-23.0-b12
  export JVM_NAME="GraalVM EE"
  export JVM_IDENTIFIER="graalvm-ee"
}

configure_graalvm_ce() {
  export JAVA_HOME=/usr/lib/jvm/graalvm-ce-jdk-17.0.7+7-jvmci-23.0-b12
  export JVM_NAME="GraalVM CE"
  export JVM_IDENTIFIER="graalvm-ce"
}

configure_native_image() {
  export JAVA_HOME=/usr/lib/jvm/graalvm-ee-jdk-17.0.7+8-LTS-jvmci-23.0-b12
  export JVM_NAME="GraalVM Native Image"
  export JVM_IDENTIFIER="native-image"
}

configure_openj9() {
  export JAVA_HOME=/usr/lib/jvm/ibm-semeru-openj9-jdk-17.0.6+10
  export JVM_NAME="Eclipse OpenJ9 VM"
  export JVM_IDENTIFIER="eclipse-openj9-vm"
}

configure_azul_prime() {
  export JAVA_HOME=/usr/lib/jvm/zing23.04.0.0-2-jdk17.0.7-linux_x64
  export JVM_NAME="Azul Prime VM"
  export JVM_IDENTIFIER="azul-prime-vm"
}

echo "Select the JVM:"
echo "    1) - OpenJDK HotSpot VM"
echo "    2) - GraalVM CE"
echo "    3) - GraalVM EE"
echo "    4) - Native Image"
echo "    5) - Azul Prime VM"
echo "    6) - Eclipse OpenJ9 VM"
echo ""

while :; do
  read -r INPUT_KEY
  case $INPUT_KEY in
  1)
    configure_openjdk
    break
    ;;
  2)
    configure_graalvm_ce
    break
    ;;
  3)
    configure_graalvm_ee
    break
    ;;
  4)
    configure_native_image
    break
    ;;
  5)
    configure_azul_prime
    break
    ;;
  6)
    configure_openj9
    break
    ;;
  *)
    echo "Sorry, I don't understand. Try again!"
    ;;
  esac
done

if [ ! -x "$JAVA_HOME"/bin/java ]; then
  echo ""
  echo "ERROR: Cannot properly execute '$JAVA_HOME/bin/java' command, unable to continue!"
  exit 1
fi

export PATH=$JAVA_HOME/bin:$PATH
export JDK_VERSION=$(java -XshowSettings:properties 2>&1 >/dev/null | grep 'java.specification.version' | awk '{split($0, array, "="); print array[2]}' | xargs echo -n)
export OUTPUT_FOLDER=results/$OS/$ARCH/jdk-$JDK_VERSION

echo ""
echo "Java home: $JAVA_HOME"
echo "JDK version: $JDK_VERSION"
echo "JVM name: $JVM_NAME"
echo "JVM identifier: $JVM_IDENTIFIER"
echo "Output folder: $OUTPUT_FOLDER"
echo ""

read -r -p "If the above configuration is correct, press ENTER to continue or CRTL+C to abort ... "
