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

configure_jvm() {
  export JAVA_HOME="$1"
  export JVM_NAME="$2"
  export JVM_IDENTIFIER="$3"
}

select_jvm() {
  echo ""
  echo "Select the JVM:"
  echo "    1) - OpenJDK HotSpot VM"
  echo "    2) - GraalVM CE"
  echo "    3) - Oracle GraalVM (formerly GraalVM EE)"
  echo "    4) - GraalVM Native Image"
  echo "    5) - Azul Prime VM (formerly Azul Zing VM)"
  echo "    6) - Eclipse OpenJ9 VM"
  echo ""

  while :; do
    read -r INPUT_KEY
    case $INPUT_KEY in
    1)
      configure_jvm "$OPENJDK_HOTSPOT_VM_HOME" "$OPENJDK_HOTSPOT_VM_NAME" "$OPENJDK_HOTSPOT_VM_IDENTIFIER"
      break
      ;;
    2)
      configure_jvm "$GRAAL_VM_CE_HOME" "$GRAAL_VM_CE_NAME" "$GRAAL_VM_CE_IDENTIFIER"
      break
      ;;
    3)
      configure_jvm "$ORACLE_GRAAL_VM_HOME" "$ORACLE_GRAAL_VM_NAME" "$ORACLE_GRAAL_VM_IDENTIFIER"
      break
      ;;
    4)
      configure_jvm "$GRAAL_VM_NATIVE_IMAGE_HOME" "$GRAAL_VM_NATIVE_IMAGE_NAME" "$GRAAL_VM_NATIVE_IMAGE_IDENTIFIER"
      break
      ;;
    5)
      configure_jvm "$AZUL_PRIME_VM_HOME" "$AZUL_PRIME_VM_NAME" "$AZUL_PRIME_VM_IDENTIFIER"
      break
      ;;
    6)
      configure_jvm "$ECLIPSE_OPEN_J9_HOME" "$ECLIPSE_OPEN_J9_NAME" "$ECLIPSE_OPEN_J9_IDENTIFIER"
      break
      ;;
    *)
      echo "Sorry, I don't understand. Please try again!"
      ;;
    esac
  done
}

set_environment_variables() {
  if [ ! -x "$JAVA_HOME"/bin/java ]; then
    echo ""
    echo "ERROR: Unable to execute the '$JAVA_HOME/bin/java' command. Cannot proceed!"
    return 1
  fi

  export PATH=$JAVA_HOME/bin:$PATH
  export JDK_VERSION=$(java -XshowSettings:properties 2>&1 >/dev/null | grep 'java.specification.version' | awk '{split($0, array, "="); print array[2]}' | xargs echo -n)
  export OUTPUT_FOLDER=results/$OS/$ARCH/jdk-$JDK_VERSION

  echo "Java home: $JAVA_HOME"
  echo "JDK version: $JDK_VERSION"
  echo "JVM name: $JVM_NAME"
  echo "JVM identifier: $JVM_IDENTIFIER"
  echo "Output folder: $OUTPUT_FOLDER"
}

echo ""
echo "+------------+"
echo "| Select JVM |"
echo "+------------+"
echo "The JDK version is automatically detected based on the JDK distribution found at the preconfigured 'JAVA_HOME' path."
echo "This assumes that the 'JAVA_HOME' variable has already been specified in the benchmark configuration scripts (i.e., the ./configure-jvm file). Otherwise, the subsequent execution will fail."
select_jvm

echo ""
echo "+---------------------------+"
echo "| JVM Environment Variables |"
echo "+---------------------------+"
if ! set_environment_variables; then
  exit 1
fi



