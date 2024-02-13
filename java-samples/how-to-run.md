# How to Run with Different JVMs

Each of the following command should be executed on the **system under test machine**.

# Test with OpenJDK HotSpot VM
```bash
# On the system under test machine, launch the samples:
$ ./run-samples.sh --jvm-identifier=openjdk-hotspot-vm
```

# Test with GraalVM CE
```bash
$ ./run-samples.sh --jvm-identifier=graalvm-ce
```

# Test with Oracle GraalVM
```bash
$ ./run-samples.sh --jvm-identifier=oracle-graalvm
```

# Test with Native Image
```bash
$ ./run-samples.sh --jvm-identifier=native-image
```

# Test with Azul Prime VM
```bash
$ ./run-samples.sh --jvm-identifier=azul-prime-vm
```

# Test with Eclipse OpenJ9 VM
```bash
$ ./run-samples.sh --jvm-identifier=eclipse-openj9-vm
```