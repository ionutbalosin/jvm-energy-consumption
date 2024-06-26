# How to Run with Different JVMs

Each of the following command pairs should be executed on different machines, with `run-application.sh` run on the **system under test machine** and `run-wrk.sh` run on the **test client machine**.

**Note:** Each `run-application.sh` includes the parameter `--skip-os-tuning` for convenience only, to bypass the OS configuration prompt. However, this does not imply that these configurations, especially on Linux, should be skipped. We recommend applying them once at the beginning.

# Test with OpenJDK HotSpot VM
```bash
# On the system under test machine, launch the application:
$ ./run-application.sh --jvm-identifier=openjdk-hotspot-vm --skip-os-tuning

# Once the application successfully starts, switch to the test client machine and execute the following command:
$ ./run-wrk.sh --jvm-identifier=openjdk-hotspot-vm --app-base-url=192.168.0.2:8080
```

# Test with GraalVM CE
```bash
$ ./run-application.sh --jvm-identifier=graalvm-ce --skip-os-tuning

$ ./run-wrk.sh --jvm-identifier=graalvm-ce --app-base-url=192.168.0.2:8080
```

# Test with Oracle GraalVM
```bash
$ ./run-application.sh --jvm-identifier=oracle-graalvm --skip-os-tuning

$ ./run-wrk.sh --jvm-identifier=oracle-graalvm --app-base-url=192.168.0.2:8080
```

# Test with Native Image
```bash
$ ./run-application.sh --jvm-identifier=native-image --skip-os-tuning

$ ./run-wrk.sh --jvm-identifier=native-image --app-base-url=192.168.0.2:8080
```

# Test with Native Image and PGO/G1GC
```bash
# Trick: Since the PGO profiles have already been generated, this step only triggers the build phase for the "--pgo-instrument" option and saves the output files (e.g., power consumption files).
# The PGO output is redirected to a temporary folder that is not saved since the run is skipped.
$ ./run-application.sh --run-identifier=pgo_instrument --jvm-identifier=native-image --enable-pgo --pgo-dir=tmp --skip-os-tuning --skip-run

# The following two commands start the actual testing using the PGO profile
$ ./run-application.sh --run-identifier=pgo --jvm-identifier=native-image --enable-pgo --skip-os-tuning

$ ./run-wrk.sh --run-identifier=pgo --jvm-identifier=native-image --app-base-url=192.168.0.2:8080
```

# Test with Azul Prime VM
```bash
$ ./run-application.sh --jvm-identifier=azul-prime-vm --skip-os-tuning

$ ./run-wrk.sh --jvm-identifier=azul-prime-vm --app-base-url=192.168.0.2:8080
```

# Test with Eclipse OpenJ9 VM
```bash
$ ./run-application.sh --jvm-identifier=eclipse-openj9-vm --skip-os-tuning

$ ./run-wrk.sh --jvm-identifier=eclipse-openj9-vm --app-base-url=192.168.0.2:8080
```