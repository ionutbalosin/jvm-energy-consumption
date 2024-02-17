<p align="center">
  <img alt="JvmEnergyConsumption" title="JvmEnergyConsumption" src="docs/images/jec_dinosaur_logo_320.png">
</p>

<h1 align="center">JVM Energy Consumption</h1>
<h4 align="center">⚡️ A Software-Based Empirical Approach to Assess JVMs Energy Efficiency ⚡️</h4>

---

This repository contains different Java Virtual Machine (JVM) benchmarks to measure the JVM energy consumption using various off-the-shelf applications implemented with multiple technology stacks.

## Content

- [Purpose](#purpose)
- [Methodology](#methodology)
  - [High-Level Architecture](#high-level-architecture)
  - [Load Test System Architecture](#load-test-system-architecture) 
- [Software-based Power Meters](#software-based-power-meters)
  - [RAPL interface on GNU/Linux](#rapl-interface-on-gnulinux)
  - [powermetrics on macOS](#powermetrics-on-macos)
- [Setup](#setup)
- [Measurements](#measurements)
  - [Baseline Idle OS](#baseline-idle-os)
  - [Java Samples](#java-samples)
  - [Spring PetClinic Application](#spring-petclinic-application)
  - [Quarkus Hibernate ORM Panache Quickstart](#quarkus-hibernate-orm-panache-quickstart)
- [License](#license)

## Purpose

The objective of this project is to evaluate energy consumption among various JVM distributions by executing custom-made Java programs employing different coding paradigms, as well as off-the-shelf applications such as [Spring Boot](https://spring.io/projects/spring-boot) and [Quarkus](https://quarkus.io/) web-based applications.

While comparing energy consumption across different frameworks is not the primary focus (due to variations in code, making direct comparisons challenging), the goal is to maintain the same application (or code samples) and assess the energy consumption when changing only the runtime or the JVM.

## Methodology

### High-Level Architecture

For a comprehensive analysis, we need to record both real-time energy consumption and additionally the internal temperature (reported by available PC hardware sensors) while running the JVM application.

Therefore, both physical and software measurements are needed:
- _Physical measurements_ rely on physical devices (e.g., wall power meters) to capture end-to-end energy consumption.
- _Software measurements_ rely on existing OS/CPU architecture-specific interfaces to report the energy consumption for different parts of the system and the available sensor temperatures.

[![high-level-system-architecture.svg](./docs/high-level-system-architecture.svg?raw=true)](./docs/high-level-system-architecture.svg?raw=true)

On **system under test machine** runs the target JVM application.

### Load Test System Architecture

When measuring JVM energy consumption, it is crucial to simulate a realistic application workload, ensuring the usage of the application and triggering as many endpoints as possible within a reasonable time interval. Merely starting and stopping the application is insufficient, as it may skip critical factors such as the Garbage Collector footprint and Just-In-Time compiler optimizations, thereby rendering the measurements less relevant. In this context, load test scenarios should be conducted for certain applications, such as Spring Boot and Quarkus web-based applications.

The load testing tool should run on a different host than the target JVM application, otherwise, the energy measurements will be negatively impacted.

[![load-test-system-architecture.svg](./docs/load-test-system-architecture.svg?raw=true)](./docs/load-test-system-architecture.svg?raw=true)

On **test client machine** runs the load testing tool (e.g., `wrk`) as well as any additional resource needed for the application (e.g., PostgreSQL database).

The network latency between the system under test machine and the test client machine (i.e., round trip time) must be constant and neglectable, that's why a wired connection is preferred.

## Software-based Power Meters

Energy consumption reporting methods vary depending on the operating system and CPU architecture.

### RAPL interface on GNU/Linux

On GNU/Linux, the Running Average Power Limit (**RAPL**) interface is utilized. RAPL offers power-limiting capabilities and precise energy readings for multiple power domains. Each supported power domain exposes a Machine Specific Register (MSR) containing a 32-bit integer, which is updated at approximately 1-millisecond intervals. 

The RAPL power domains that are available on Intel CPUs (and potentially AMD Ryzen CPUs) include:

- Package (PKG) domain: Measures the energy consumption of the entire socket, including all cores, integrated graphics, and uncore components like last-level caches and memory controller.
- Power Plane 0 (PP0) domain: Measures the energy consumption of all processor cores on the socket.
- Power Plane 1 (PP1) domain: Measures the energy consumption of the processor graphics (GPU) on the socket (desktop models only).
- DRAM domain: Measures the energy consumption of the random access memory (RAM) attached to the integrated memory controller.
- PSys domain: Monitors and controls the thermal and power specifications of the entire SoC (System on a Chip). It is especially useful when the power consumption source is neither the CPU nor the GPU. PSys includes power consumption from the package domain, System Agent, PCH, eDRAM, and other domains within a single-socket SoC.

In multi-socket server systems, each socket reports its own RAPL values. For example, a two-socket computing system has separate PKG readings for both packages, separate PP0 readings, and so on.

> Depending on the CPU manufacturer and series, some of the RAPL domains might be available or not. Please check your hardware.

### powermetrics on macOS

On macOS, the `powermetrics` command is used to display various system metrics, including CPU usage statistics, for different samplers like cpu, gpu, thermal, battery, network, and disk at a specific sampling interval.

> Depending on the CPU manufacturer and series, some of the samplers might be available or not. Please check your hardware.

## Setup

To properly run the scripts, you need to download, install, and properly configure the following tools:

### Operating system-specific tools

 OS        | Covered | Tools                               
-----------|---------|-------------------------------------
 GNU/Linux | Yes     | `ps`, `powerstat` (i.e., RAPL interface), `wrk`
 macOS     | Yes     | `ps`, `powermetrics`, `wrk`                      
 Windows   | No      | N/A                                 

### sudo root access

Please ensure that you have `sudo` (root) access; otherwise, the `powerstat` and `powermetrics` commands cannot be executed while measuring the energy consumption during tests.

**Linux only:** On system under test machine, for a smooth tests execution, we recommend extending the default `sudo timeout`, which is typically a few minutes depending on the Linux distribution, to a higher value (e.g., 1440 minutes) to accommodate the test durations and avoid the sudo prompt from appearing again while tests are running.

```
$ sudo visudo

# Add the following line to change the sudo timeout 
Defaults        timestamp_timeout=1440
```

For more information, please refer to the [How to Change Sudo Timeout Period on Linux](https://www.omglinux.com/change-sudo-timeout-linux) tutorial.

### Java Development Kit (JDK)

JVM Distribution     | Build
-------------------- | ----------------------------------------------------------------------------
OpenJDK HotSpot VM   | [Download](https://projects.eclipse.org/projects/adoptium.temurin/downloads)
GraalVM CE           | [Download](https://github.com/graalvm/graalvm-ce-builds/releases)
Oracle GraalVM       | [Download](https://www.graalvm.org/downloads)
Native-Image         | [Download](https://www.graalvm.org/downloads)
Azul Prime VM `(*)`  | [Download](https://www.azul.com/downloads)
Eclipse OpenJ9 VM    | [Download](https://www.eclipse.org/openj9) 

_`(*)` - License restrictions might apply_

### wrk

1. Clone the [wrk](https://github.com/wg/wrk) repository, a modern HTTP benchmarking tool used for the load testing.

2. Build wrk from sources
```properties
# Install the build prerequisites
$ sudo apt-get install build-essential libssl-dev libz-dev make -y 

# Clone and build the wrk repository
$ git clone https://github.com/wg/wrk.git
$ cd wrk
$ sudo make

# Move the wrk executable to PATH
$ sudo cp wrk /usr/local/bin 
```

**Note:** Latencies shown by `wrk` suffer from Coordinated Omission; nevertheless, we use it to measure maximum throughput. 
For accurate latency measurements, complement this with [wrk2](https://github.com/giltene/wrk2). 

In summary, `wrk` can be used for measuring maximum throughput, while `wrk2` is utilized to capture latency numbers under a sustained load.

### Configurations

The following configuration is required before starting any measurement:

1. Open the [config.properties](./settings/config.properties) file.

2. Update the specific **VM_HOME** property for the JDK you intend to use. You don't need to update all of them; only the one you plan to use for compiling and running.

 ```properties
OPENJDK_HOTSPOT_VM_HOME="<path_to_openjdk_hotspot>"
GRAAL_VM_CE_HOME="<path_to_graalvm_ce>"
ORACLE_GRAAL_VM_HOME="<path_to_oracle_graalvm>"
GRAAL_VM_NATIVE_IMAGE_HOME="<path_to_graalvm_native_image>"
AZUL_PRIME_VM_HOME="<path_to_azul_prime_vm>"
ECLIPSE_OPEN_J9_VM_HOME="<path_to_eclipse_openj9>"
```

3. Update the specific **APP_HOME** properties that point to the locally downloaded/installed application. 

 ```properties
SPRING_PETCLINIC_HOME="<path_to_spring_petclinic>"
QUARKUS_HIBERNATE_ORM_PANACHE_QUICKSTART_HOME="<path_to_quarkus_quickstart>"
 ```

## Measurements

### Baseline Idle OS

This set of measurements captures the idle power consumption, and it is used to understand (and remove) the overhead of the hardware system:

```
$ cd /baseline-idle-os
$ ./run-baseline.sh [--run-identifier=<run-identifier>] [--duration=<duration>]
```

Please follow the [how-to-run](./baseline-idle-os/how-to-run.md) instructions.

### Java Samples

This set of measurements relies on specific code patterns to identify the most efficient energy-friendly coding paradigm. It includes the following common patterns:

- Logging patterns
- Memory access patterns
- Exception throwing patterns
- String concatenation patterns
- (Sorting) algorithm
- Virtual calls
- Maximum throughput while using virtual/physical threads

```
$ cd /java-samples
$ ./run-samples.sh [--jvm-identifier=<jvm-identifier>] [--run-identifier=<run-identifier>] [--duration=<duration>] [--skip-os-tuning] [--skip-build]
```

Please follow the [how-to-run](./java-samples/how-to-run.md) instructions.

### Spring PetClinic Application

This set of measurements uses the off-the-shelf Spring PetClinic application.

1. Clone the repository [spring-petclinic](https://github.com/spring-projects/spring-petclinic)
2. On top of the existing code, apply the custom configurations as explained in the [application-readme.md](off-the-shelf-applications/spring-petclinic/application-readme.md)
3. Launch the JVM application on the **system under test machine**:

```
$ cd /spring-petclinic
$ ./run-application.sh [--jvm-identifier=<jvm-identifier>] [--run-identifier=<run-identifier>] [--duration=<duration>] [--enable-pgo-g1gc] [--skip-os-tuning] [--skip-build]
```

4. After the application has successfully started, launch the `wrk` on the **test client machine**:

```
$ cd /spring-petclinic
$ ./run-wrk.sh --jvm-identifier=<jvm-identifier> [--run-identifier=<run-identifier>] [--jdk-version=<jdk-version>] [--app-base-url=<app-base-url>] [--wrk-duration=<wrk-duration>] [--wrk-threads=<wrk-threads>]
```

Please follow the [how-to-run](./off-the-shelf-applications/how-to-run.md) instructions.

### Quarkus Hibernate ORM Panache Quickstart

This set of measurements uses the off-the-shelf Quarkus Hibernate ORM Panache quickstart.

1. Clone the repository [quarkus-quickstarts](https://github.com/quarkusio/quarkus-quickstarts)
2. On top of the existing **hibernate-orm-panache-quickstart** source module, apply the custom configurations as explained in the [application-readme.md](off-the-shelf-applications/quarkus-hibernate-orm-panache-quickstart/application-readme.md)
3. Launch the JVM application on the **system under test machine**:

```
$ cd /quarkus-hibernate-orm-panache-quickstart
$ ./run-application.sh [--jvm-identifier=<jvm-identifier>] [--run-identifier=<run-identifier>] [--duration=<duration>] [--enable-pgo-g1gc] [--skip-os-tuning] [--skip-build]
```

4. After the application has successfully started, launch the `wrk` on the **test client machine**:

```
$ cd /quarkus-hibernate-orm-panache-quickstart
$ ./run-wrk.sh --jvm-identifier=<jvm-identifier> [--run-identifier=<run-identifier>] [--jdk-version=<jdk-version>] [--app-base-url=<app-base-url>] [--wrk-duration=<wrk-duration>] [--wrk-threads=<wrk-threads>]
```

Please follow the [how-to-run](./off-the-shelf-applications/how-to-run.md) instructions.

### Generate the plots

```
./plot-results.sh
```

The plots are saved under `results/jdk-$JDK_VERSION/$ARCH/$OS/plot` directory.

# License

Please see the [LICENSE](LICENSE) file for full license.

```
JVM Energy Consumption

MIT License

Copyright (c) 2023-2024 Ionut Balosin, Ko Turk

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```