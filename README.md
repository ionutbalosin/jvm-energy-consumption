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

The purpose of this project is to assess the energy consumption across multiple JVMs distributions while running different custom-made Java programs (using different coding paradigms) and off-the-shelf applications (e.g., including [Spring Boot](https://spring.io/projects/spring-boot) and [Quarkus](https://quarkus.io/) web-based).

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

### RAPL Coverage

It is worth mentioning that since RAPL reports only the energy consumption of a few domains (e.g., CPU/GPU/DRAM), but the system overall consumes much more energy for other components that are not included, as follows:
- any networking interface as Ethernet, Wi-Fi (Wireless Fidelity), Bluetooth, etc.
- any attached storage device (as hard disk drives, solid-state drives, and optical drives) relying on SATA (Serial AT Attachment), NVMe (Non-Volatile Memory Express), USB (Universal Serial Bus), Thunderbolt, SCSI (Small Computer System Interface), FireWire (IEEE 1394), Fibre Channel, etc.
- any display interface using HDMI (High-Definition Multimedia Interface), VGA (Video Graphics Array), DVI (Digital Visual Interface), Thunderbolt, DisplayPort, etc.
- the motherboard

In other words, for a typical JVM application, this means that any I/O operation that involves reading data from or writing data to any storage device but also any networking operation that uses I/O to send or receive data over a network are not captured.

To compensate this gap, in addition to the RAPL, a wall power meter must be used and check the differences.

### RAPL Validity, and Accuracy

Proof of the measurement methods' validity, which depend on RAPL, is necessary. Therefore, below are some noteworthy RAPL-based studies.

Desrochers et al. [2] measured DRAM energy consumption while minimizing interference, comparing it with RAPL measurements. These findings validate the DRAM domain, utilizing diverse systems and benchmarks. Variances between physical and RAPL measurements are below 20% [2]. Recent processors, like Intel Haswell microarchitecture, exhibit improved precision compared to earlier generations.

Zhang et al. [3] set a power consumption limit using RAPL and evaluated its adherence. Out of 16 benchmarks, 14 had a 2% mean absolute percentage error (MAPE), while 2 had an error rate exceeding 5% [3]. The study also highlighted RAPL's improved accuracy in high energy consumption scenarios.

Khan et al. [4] compared RAPL to wall power measurements in Taito supercomputer, finding a strong 99% correlation. The estimation error (MAPE) was only 1.7%. Performance overhead of reading RAPL was <1% [4].

Based to these extensive studies, RAPL is considered to be a reliable and widely used tool for power consumption analysis on Intel-based systems.

In addition to that, it is worth noting that on the newer CPUs, including the Intel Haswell microarchitecture, the RAPL precision is better than in previous generations.

### Measurements Considerations

Reduced RAPL accuracy may be expected when the processor is not running heavy workloads and is in an idle state

RAPL measures the total power consumption of the entire system or package, encompassing all components and applications running on the same machine. It does not provide a breakdown of power consumption per individual application or component. Therefore, it is crucial to establish a **baseline measurement** of the system's power consumption during idle or minimal background processes

Excessive heat can impact both the overall power consumption and performance of a system, indirectly affecting RAPL measurements. For that reason, disabling both:
- **turbo-boost** mode and
- **hyper-threading**

can effectively reduce CPU heat and enhance the consistency of RAPL measurements.
It is also important to account for any external factors that may influence power consumption, such as variations in ambient temperature or fluctuations in power supply. These factors can introduce additional variability to RAPL measurements and should be taken into consideration during data analysis and interpretation

Measuring power consumption for smaller tasks (such as **micro-benchmarking**) that complete quickly can be challenging, as the overall results are often dominated by the JVM footprint rather than the specific code being tested. This challenge can be partially addressed by employing iteration loops around the code snapshots being measured.

In addition to RAPL, a **wall power meter** is necessary to assess cumulative power consumption. However, there are significant differences between these two methods that make them complementary rather than interchangeable:

- the sampling periods may differ between RAPL and the wall power meter.
- the measurement scales are also different, with RAPL reporting in Joules and the wall power meter typically using kilowatt-hours (kWh).
- the wall power meter provides measurements for the overall system power consumption, encompassing all components, whereas RAPL focuses specifically on individual domains (e.g., CPU, GPU, DRAM) within the system.

### Measurements Unit

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

#### Load Test System Architecture

While measuring the JVM energy consumption, it is important to have a realistic load (i.e., usage of the application) and to trigger as many endpoints as possible for a reasonable time interval, otherwise, the Gargabe Collector footprint or further Just-In-Time compiler optimizations are simply skipped and makes the measurements less relevant. Just starting and stopping the application is not an option.
In this regard, load test scenarios must be conducted for some applications (e.g., Spring Boot and Quarkus web-based).

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
2. Append the custom [application properties](./spring-petclinic/application.properties) to the existing [application.properties](https://github.com/spring-projects/spring-petclinic/blob/main/src/main/resources/application.properties) and then build the application
3. Open the [run-application.sh](./spring-petclinic/run-application.sh) script and update the variables `JAVA_HOME`, `APP_HOME`
4. Open the [run-hyperfoil.sh](./spring-petclinic/run-hyperfoil.sh) script and update the variable `HYPERFOIL_HOME`
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
2. Append the custom [application properties](./quarkus-hibernate-orm-panache-quickstart/application.properties) to the existing [application.properties](https://github.com/quarkusio/quarkus-quickstarts/blob/main/hibernate-orm-panache-quickstart/src/main/resources/application.properties) and then build the application
3. Open the [run-application.sh](./quarkus-hibernate-orm-panache-quickstart/run-application.sh) script and update the variables `JAVA_HOME`, `APP_HOME`, `POSTGRESQL_DATASOURCE`
4. Open the [run-hyperfoil.sh](./quarkus-hibernate-orm-panache-quickstart/run-hyperfoil.sh) script and update the variable `HYPERFOIL_HOME`
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

### Renaissance Benchmark Suite

This set of measurements uses the off-the-shelf Renaissance benchmark suite.

1. Download the [Renaissance release](https://github.com/renaissance-benchmarks/renaissance/releases)
2. Open the [run-benchmarks.sh](./renaissance/run-benchmarks.sh) script and update the variables `JAVA_HOME`, `APP_HOME`
3. Launch the benchmarks:

```
$ cd /renaissance
$ sudo ./run-benchmarks.sh
```

# References

1. Tom Strempel. Master’s Thesis [Measuring the Energy Consumption of Software written in C on x86-64 Processors](https://ul.qucosa.de/api/qucosa%3A77194/attachment/ATT-0)

2. Spencer Desrochers, Chad Paradis, and Vincent M. Weaver. “A validation of DRAM RAPL power measurements”. In: ACM International Conference Proceed- ing Series 03-06-October-2016 (2016). DOI: [10.1145/2989081.2989088.](https://doi.org/10.1145/2989081.2989088)

3. Zhang Huazhe and Hoffman H. _“A quantitative evaluation of the RAPL power control system”_. In: _Feedback Computing_ (2015).

4. Kashif Nizam Khan et al. “RAPL in action: Experiences in using RAPL for power measurements”. In: ACM Transactions on Modeling and Performance Evaluation of Computing Systems 3 (2 2018). ISSN: 23763647. DOI: [10.1145/3177754](https://doi.org/10.1145/3177754).

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