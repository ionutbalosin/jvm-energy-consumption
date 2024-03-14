# How to Run with Different JVMs

Each of the following command should be executed on the **system under test machine**.

**Note:** Each `./run-samples.sh` includes the parameter `--skip-os-tuning` for convenience only, to bypass the OS configuration prompt. However, this does not imply that these configurations, especially on Linux, should be skipped. We recommend applying them once at the beginning.

# Test with OpenJDK HotSpot VM
```bash
# On the system under test machine, launch the samples:
$ ./run-samples.sh --jvm-identifier=openjdk-hotspot-vm --skip-os-tuning
```

# Test with GraalVM CE
```bash
$ ./run-samples.sh --jvm-identifier=graalvm-ce --skip-os-tuning
```

# Test with Oracle GraalVM
```bash
$ ./run-samples.sh --jvm-identifier=oracle-graalvm --skip-os-tuning
```

# Test with Native Image
```bash
$ ./run-samples.sh --jvm-identifier=native-image --skip-os-tuning
```

# Test with Native Image and PGO/G1GC
```bash
# Trick: Since the PGO profiles have already been generated, this step only triggers the build phase for the "--pgo-instrument" option and saves the output files (e.g., power consumption files).
# The PGO output is redirected to a temporary folder that is not saved since the run is skipped.
$ ./run-samples.sh --run-identifier=pgo_instrument --jvm-identifier=native-image --enable-pgo-g1gc --pgo-dir=tmp --skip-os-tuning --skip-run

$ ./run-samples.sh --run-identifier=pgo_g1gc --jvm-identifier=native-image --enable-pgo-g1gc --skip-os-tuning
```

# Test with Azul Prime VM
```bash
$ ./run-samples.sh --jvm-identifier=azul-prime-vm --skip-os-tuning
```

# Test with Eclipse OpenJ9 VM
```bash
$ ./run-samples.sh --jvm-identifier=eclipse-openj9-vm --skip-os-tuning
```