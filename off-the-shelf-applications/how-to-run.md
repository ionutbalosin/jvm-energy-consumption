# How to Run with Different JVMs

Each of the following command pairs should be executed on different machines, with `run-application.sh` run on the **system under test machine** and `run-wrk.sh` run on the **test client machine**.

# Test with OpenJDK HotSpot VM
```bash
# On the system under test machine, launch the application:
$ ./run-application.sh --jvm-identifier=openjdk-hotspot-vm

# Once the application successfully starts, switch to the test client machine and execute the following command:
$ ./run-wrk.sh --jvm-identifier=openjdk-hotspot-vm
```

# Test with GraalVM CE
```bash
$ ./run-application.sh --jvm-identifier=graalvm-ce

$ ./run-wrk.sh --jvm-identifier=graalvm-ce
```

# Test with Oracle GraalVM
```bash
$ ./run-application.sh --jvm-identifier=oracle-graalvm

$ ./run-wrk.sh --jvm-identifier=oracle-graalvm
```

# Test with Native Image
```bash
$ ./run-application.sh --jvm-identifier=native-image

$ ./run-wrk.sh --jvm-identifier=native-image
```

# Test with Native Image and PGO/G1GC
```bash
$ ./run-application.sh --run-identifier=pgo_g1gc --jvm-identifier=native-image --enable-pgo-g1gc

$ ./run-wrk.sh --run-identifier=pgo_g1gc --jvm-identifier=native-image
```

# Test with Azul Prime VM
```bash
$ ./run-application.sh --jvm-identifier=azul-prime-vm

$ ./run-wrk.sh --jvm-identifier=azul-prime-vm
```

# Test with Eclipse OpenJ9 VM
```bash
$ ./run-application.sh --jvm-identifier=eclipse-openj9-vm

$ ./run-wrk.sh --jvm-identifier=eclipse-openj9-vm
```