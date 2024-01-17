# JVM Energy Consumption

This repository contains different Java Virtual Machine (JVM) benchmarks to measure the JVM energy consumption using various off-the-shelf applications implemented with multiple technology stacks.

## Content

- [Purpose](#purpose)
- [Methodology](#methodology)
- [Prerequisites](#prerequisites)
- [OS Coverage](#os-coverage)
- [JVM Coverage](#jvm-coverage)
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

To measure energy consumption, Intel's Running Average Power Limit (**RAPL**) interface is utilized. RAPL offers power-limiting capabilities and precise energy readings for multiple power domains. Each supported power domain exposes a Machine Specific Register (MSR) containing a 32-bit integer, which is updated at approximately 1-millisecond intervals. The RAPL power domains include:

- Package (PKG) domain: Measures the energy consumption of the entire socket, including all cores, integrated graphics, and uncore components like last-level caches and memory controller.
- Power Plane 0 (PP0) domain: Measures the energy consumption of all processor cores on the socket.
- Power Plane 1 (PP1) domain: Measures the energy consumption of the processor graphics (GPU) on the socket (desktop models only).
- DRAM domain: Measures the energy consumption of the random access memory (RAM) attached to the integrated memory controller.
- PSys domain: Introduced with Intel Skylake, it monitors and controls the thermal and power specifications of the entire SoC (System on a Chip). It is especially useful when the power consumption source is neither the CPU nor the GPU. PSys includes power consumption from the package domain, System Agent, PCH, eDRAM, and other domains within a single-socket SoC.

In multi-socket server systems, each socket reports its own RAPL values. For example, a two-socket computing system has separate PKG readings for both packages, separate PP0 readings, and so on.


The command pattern used to start the JVM application that also reports at the end the energy stats rely on `perf` (available only on Linux):

```
$ perf stat -a \
   -e "power/energy-cores/" \
   -e "power/energy-gpu/" \
   -e "power/energy-pkg/" \
   -e "power/energy-psys/" \
   -e "power/energy-ram/" \
   <application_runner_path>
```

The unit of energy reported by this command is **Joule** (symbol J).

Analogous, a **watt-second** (symbol W s or W⋅s) is a derived unit of energy equivalent to the Joule. The watt-second is the energy equivalent to the power of one watt sustained for one second.
While the watt-second is equivalent to the Joule in both units and meaning (e.g., 1 W⋅s = 1 J), I favor using the term "watt-second" instead of "Joule", which is, in general, easier to understand (and correlate with real-life examples) when speaking about the power consumption.

**Note:** In general, the overhead introduced by `perf` is relatively low, especially when using hardware performance counters. The impact on system performance is typically considered acceptable for most profiling and performance analysis tasks.

### Load Test System Architecture

When measuring JVM energy consumption, it is crucial to simulate a realistic application workload, ensuring the usage of the application and triggering as many endpoints as possible within a reasonable time interval. Merely starting and stopping the application is insufficient, as it may skip critical factors such as the Garbage Collector footprint and Just-In-Time compiler optimizations, thereby rendering the measurements less relevant. In this context, load test scenarios should be conducted for certain applications, such as Spring Boot and Quarkus web-based applications.

The load testing tool should run on a different host than the target JVM application, otherwise, the energy measurements will be negatively impacted.

[![load-test-system-architecture.svg](./docs/load-test-system-architecture.svg?raw=true)](./docs/load-test-system-architecture.svg?raw=true)

On **system under test** runs only the target JVM application.

On **system client test** runs the load testing tool (e.g., Hyperfoil) as well as any additional resource needed for the application (e.g., PostgreSQL database).

The network latency between the system under test and the system client test (i.e., round trip time) must be constant and neglectable, that's why a wired connection is preferred.

## Prerequisites

In order to properly run the scripts you need to:
- install `perf` on Linux
- download and install any JDK distribution (please see below the list of the recommended ones)
- download and install [Hyperfoil](https://hyperfoil.io) (i.e., a microservice-oriented distributed benchmark framework)

## OS Coverage

The table below summarizes the list of Operating Systems included in the measurements:

No. | OS      | Covered
----|---------|--------
1   | linux   | yes
2   | mac     | no
3   | windows | no

## JVM Coverage

The table below summarizes the list of JVM distributions included in the measurements:

No. | JVM distribution
----|--------------------
1   | [OpenJDK HotSpot VM](https://projects.eclipse.org/projects/adoptium.temurin/downloads)
2   | [GraalVM CE](https://www.graalvm.org/downloads)
3   | [GraalVM EE](https://www.graalvm.org/downloads)
4   | [Native-Image](https://www.graalvm.org/22.0/reference-manual/native-image/)
5   | [Azul Prime VM](https://www.azul.com/products/prime)
6   | [Eclipse OpenJ9 VM](https://www.eclipse.org/openj9) 

## Measurements

### Baseline Idle OS

This set of measurements captures the idle power consumption, and it is used to understand (and remove) the overhead of the hardware system:

```
$ cd /baseline-idle-os
$ sudo ./run-baseline.sh
```

### Java Samples

This set of measurements relies on specific code patterns to identify what is the most friendly energy coding paradigm. It includes the most common patterns as:

- logging patterns
- memory access patterns
- throwing exception patterns
- (sorting) algorithms complexities
- virtual calls

```
$ cd /java-samples
$ sudo ./run-samples.sh
```

### Spring PetClinic Application

This set of measurements uses the off-the-shelf Spring PetClinic application.

1. Clone the repository [spring-petclinic](https://github.com/spring-projects/spring-petclinic)
2. Append the custom [application properties](off-the-shelf-applications/spring-petclinic/application.properties) to the existing [application.properties](https://github.com/spring-projects/spring-petclinic/blob/main/src/main/resources/application.properties) and then build the application
3. Open the [run-application.sh](off-the-shelf-applications/spring-petclinic/run-application.sh) script and update the variables `JAVA_HOME`, `APP_HOME`
4. Open the [run-hyperfoil.sh](off-the-shelf-applications/spring-petclinic/run-hyperfoil.sh) script and update the variable `HYPERFOIL_HOME`
5. Launch the JVM application on the **system under test**:

```
$ cd /spring-petclinic
$ sudo ./run-application.sh
```

6. After the application has successfully started, launch the Hyperfoil on the **system client test**:

```
$ cd /spring-petclinic
$ ./run-hyperfoil.sh
```

### Quarkus Hibernate ORM Panache Quickstart

This set of measurements uses the off-the-shelf Quarkus Hibernate ORM Panache quickstart.

1. Clone the repository [quarkus-quickstarts](https://github.com/quarkusio/quarkus-quickstarts) and build the **hibernate-orm-panache-quickstart** sources
2. Append the custom [application properties](off-the-shelf-applications/quarkus-hibernate-orm-panache-quickstart/application.properties) to the existing [application.properties](https://github.com/quarkusio/quarkus-quickstarts/blob/main/hibernate-orm-panache-quickstart/src/main/resources/application.properties) and then build the application
3. Open the [run-application.sh](off-the-shelf-applications/quarkus-hibernate-orm-panache-quickstart/run-application.sh) script and update the variables `JAVA_HOME`, `APP_HOME`, `POSTGRESQL_DATASOURCE`
4. Open the [run-hyperfoil.sh](off-the-shelf-applications/quarkus-hibernate-orm-panache-quickstart/run-hyperfoil.sh) script and update the variable `HYPERFOIL_HOME`
5. Launch the PostgreSQL database on the **system client test**:

```
$ cd /quarkus-hibernate-orm-panache-quickstart
$ sudo ./run-postgresql.sh
```

6. After the PostgreSQL database has successfully started, launch the JVM application on the **system under test**:

```
$ cd /quarkus-hibernate-orm-panache-quickstart
$ sudo ./run-application.sh
```

7. After the application has successfully started, launch the Hyperfoil on the **system client test**:

```
$ cd /quarkus-hibernate-orm-panache-quickstart
$ sudo ./run-hyperfoil.sh
```

# License

### Generate the plots

```
./plot-results.sh
```

The plots are saved under `results/$OS/$ARCH/jdk-$JDK_VERSION/plot` directory.

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