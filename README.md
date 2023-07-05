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
  - [Renaissance Benchmark Suite](#renaissance-benchmark-suite)
- [License](#license)

## Purpose

The purpose of this project is to assess the energy consumption across multiple JVMs distributions while running different custom-made Java programs and off-the-shelf applications (e.g., including [Spring Boot](https://spring.io/projects/spring-boot) and [Quarkus](https://quarkus.io/) web-based).

It is not a goal to compare the energy consumption across different frameworks (since the code is different there won't be an apples-to-apples comparison), but keeping the same application and just changing the runtime (or the JVM) check the new energy consumption.

## Methodology

To measure energy consumption, Intel’s Running Average Power Limit (**RAPL**) interface is used. 
RAPL provides power-limiting features and accurate energy readings for several power domains. For each supported power domain, a Machine Specific Register (MSR) filled with a 32-bit integer is exposed and updated at intervals of approximately 1 millisecond.
The RAPL power domains:
- Package (PKG) domain measures the energy consumption of the entire socket. It includes the consumption of all the cores, integrated graphics and also the uncore components (last level caches, memory controller).
- Power Plane 0 (PP0) domain measures the energy consumption of all processor cores on the socket.
- Power Plane 1 (PP1) domain measures the energy consumption of processor graphics (GPU) on the socket (desktop models only).
- DRAM domain measures the energy consumption of random access memory (RAM) attached to the integrated memory controller.
- PSys domain introduced with Intel Skylake. It monitors and controls the thermal and power specifications of the entire SoC and it is useful especially when the source of the power consumption is neither the CPU nor the GPU. PSys includes the power consumption of the package domain, System Agent, PCH, eDRAM,and a few more domains on a single-socket SoC.

For multi-socket server systems, each socket reports its own RAPL values (for example, a two socket computing system has two separate PKG readings for both the packages, two separate PP0 readings, etc.).

Since the RAPL reports the entire energy of a host machine, it is important to minimize the load on the target machine and run only the application in charge. In addition, a baseline of the entire system (without any explicit load) should be measured.

While measuring the JVM energy consumption, it is important to have a realistic load (i.e., usage of the application) and to trigger as many endpoints as possible for a reasonable time interval, otherwise, the Gargabe Collector footprint or further Just-In-Time compiler optimizations are simply skipped and makes the measurements less relevant. Just starting and stopping the application is not an option. 

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

#### Load Test System Architecture

For some applications (e.g., Spring Boot and Quarkus web-based) load test scenarios must be conducted to collect the energy consumption.

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

```
$ cd /java-samples
$ sudo ./run-samples.sh
```

### Spring PetClinic Application

This set of measurements uses the off-the-shelf Spring PetClinic application.

1. Clone the repository [spring-petclinic](https://github.com/spring-projects/spring-petclinic) and build the sources
2. Open the [run-application.sh](./spring-petclinic/run-application.sh) script and update the variables `JAVA_HOME`, `APP_HOME`
2. Open the [run-hyperfoil.sh](./spring-petclinic/run-hyperfoil.sh) script and update the variable `HYPERFOIL_HOME`
3. Launch the JVM application on the **system under test**:

```
$ cd /spring-petclinic
$ sudo ./run-application.sh
```

4. After the application has successfully started, launch the Hyperfoil on the **system client test**:

```
$ cd /spring-petclinic
$ ./run-hyperfoil.sh
```

### Quarkus Hibernate ORM Panache Quickstart

This set of measurements uses the off-the-shelf Quarkus Hibernate ORM Panache quickstart.

1. Clone the repository [quarkus-quickstarts](https://github.com/quarkusio/quarkus-quickstarts) and build the **hibernate-orm-panache-quickstart** sources
2. Open the [run-application.sh](./quarkus-hibernate-orm-panache-quickstart/run-application.sh) script and update the variables `JAVA_HOME`, `APP_HOME`, `POSTGRESQL_DATASOURCE`
3. Open the [run-hyperfoil.sh](./quarkus-hibernate-orm-panache-quickstart/run-hyperfoil.sh) script and update the variable `HYPERFOIL_HOME`
4. Launch the PostgreSQL database on the **system client test**:

```
$ cd /quarkus-hibernate-orm-panache-quickstart
$ sudo ./run-postgresql.sh
```

5. After the PostgreSQL database has successfully started, launch the JVM application on the **system under test**:

```
$ cd /quarkus-hibernate-orm-panache-quickstart
$ sudo ./run-application.sh
```

6. After the application has successfully started, launch the Hyperfoil on the **system client test**:

```
$ cd /quarkus-hibernate-orm-panache-quickstart
$ sudo ./run-hyperfoil.sh
```

### Renaissance Benchmark Suite

This set of measurements uses the off-the-shelf Renaissance benchmark suite.

1. Download the [Renaissance release](https://github.com/renaissance-benchmarks/renaissance/releases)
2. Open the [run-benchmarks.sh](./renaissance/run-benchmarks.sh) script and update the variables `JAVA_HOME`, `APP_HOME`
3. Launch the benchmarks:

```
$ cd /renaissance
$ sudo ./run-benchmarks.sh
```

# License

Please see the [LICENSE](LICENSE) file for full license.

```
JVM Energy Consumption

MIT License

Copyright (c) 2023 Ionut Balosin
Copyright (c) 2023 Ko Turk

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