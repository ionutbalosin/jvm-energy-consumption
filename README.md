# JVM Energy Consumption

This repository contains different Java Virtual Machine (JVM) benchmarks to measure the JVM energy consumption using various off-the-shelf applications implemented with multiple technology stacks.

## Content

- [Purpose](#purpose)
- [Methodology](#methodology)
- [Prerequisites](#prerequisites)
- [JVM Coverage](#jvm-coverage)
- [Measurements](#measurements)
  - [Spring PetClinic Application](#spring-petclinic-application)
  - [Renaissance Benchmark Suite](#renaissance-benchmark-suite)
  - [Quarkus Hibernate ORM Panache Quickstart](#quarkus-hibernate-orm-panache-quickstart)
- [License](#license)

## Purpose

The main purpose of this project is to assess the energy consumption across different JVMs distributions. 

It is not intented to compare the energy consumption across different frameworks (since the code is different there won't be an apples-to-apples comparison), but keeping the same application and just changing the runtime (or the JVM) check the new energy consumption.

Different off-the-shelf applications using different technologies are included to cover a spread spectrum of applications.

## Methodology

To measure energy consumption, Intel’s Running Average Power Limit (**RAPL**) interface is used. RAPL provides power-limiting features and accurate energy readings for several domains, like Package, Core, Uncore, and DRAM. For each supported power domain, a Machine Specific Register (MSR) filled with a 32-bit integer is exposed and updated at intervals of approximately 1 millisecond.

Since the **RAPL** reports the entire energy of a host machine, it is important to minimize the load on the target machine and run only the application in charge. In addition, a baseline of the entire system (without any explicit load) should be measured.

While measuring the JVM energy consumption, it is important to have a realistic load (i.e., usage of the application) and to trigger as many endpoints as possible for a reasonable time interval, otherwise, the Gargabe Collector footprint or further Just-In-Time compiler optimizations are simply skipped and makes the measurements less relevant. Just starting and stopping the application is not an option.

#### Load Test System Architecture

The load testing tool should run on a different host than the target JVM application, otherwise, the energy measurements will be negatively impacted.

[![load-test-system-architecture.svg](./docs/load-test-system-architecture.svg?raw=true)](./docs/load-test-system-architecture.svg?raw=true)

On **system under test** runs only the target JVM application. The command pattern used to start it that also reports at the end the energy stats rely on `perf`:

```
$ perf stat -a \
   -e "power/energy-cores/" \
   -e "power/energy-gpu/" \
   -e "power/energy-pkg/" \
   -e "power/energy-psys/" \
   -e "power/energy-ram/" \
   <application_runner_path>
```

On **system client test** runs the load testing tool (e.g., JMeter or Hyperfoil) as well as any additional resource needed for the application (e.g., PostgreSQL database).

The network latency between the system under test and the system client test (i.e., round trip time) must be constant and neglectable, that's why a wired connection is preferred.

## Prerequisites

In order to properly run the scripts you need to:
- install `perf` on Linux
- download and install any JDK (you could also use [sdkman](https://sdkman.io/install))
- download and install [Hyperfoil](https://hyperfoil.io)
- download and install [JMeter](https://jmeter.apache.org/download_jmeter.cgi), including a few plugins. These plugins are needed to generate the plots, after each load test:
    - [Command-Line Graph Plotting Tool](https://jmeter-plugins.org/wiki/JMeterPluginsCMD)
    - [Filter Results Tool](https://jmeter-plugins.org/wiki/FilterResultsTool)
    - [Synthesis Report](https://jmeter-plugins.org/wiki/SynthesisReport)
    - [Response Times Over Time](https://jmeter-plugins.org/wiki/ResponseTimesOverTime)
    - [Response Times vs Threads](https://jmeter-plugins.org/wiki/ResponseTimesVsThreads)
    - [Response Times Distribution](https://jmeter-plugins.org/wiki/RespTimesDistribution)

Some tests use **Hyperfoil** and others use **JMeter** (due to historical reasons) but this is not so important regarding our goal (i.e., measuring the JVM energy consumption).

> **JMeter**: if the number of threads is not properly chosen, the [Coordinated Omission](https://groups.google.com/g/mechanical-sympathy/c/icNZJejUHfE) problem might cause inaccurate results. Please have a look at [JMeter best practices](https://jmeter.apache.org/usermanual/best-practices.html).

## JVM Coverage

The table below summarizes the full list of JVM distributions included in the measurements:

No. | JVM distribution
-------------- |--------------------
1 | [OpenJDK HotSpot](https://projects.eclipse.org/projects/adoptium.temurin/downloads)
2 | [GraalVM CE](https://www.graalvm.org/downloads)
3 | [GraalVM EE](https://www.graalvm.org/downloads)
4 | [Native-Image](https://www.graalvm.org/22.0/reference-manual/native-image/)
5 | [Azul Prime (Zing)](https://www.azul.com/products/prime)
6 | [Eclipse OpenJ9](https://www.eclipse.org/openj9) 

## Measurements

### Spring PetClinic Application

1. Clone the repository [spring-petclinic](https://github.com/spring-projects/spring-petclinic) and build the sources
2. Open the [run-application.sh](./spring-petclinic/run-application.sh) script and update the mandatory variables `JAVA_HOME`, `APP_HOME`
2. Open the [run-jmeter.sh](./spring-petclinic/run-jmeter.sh) script and update the mandatory variable `JMETER_HOME`
3. Launch the JVM application on the **system under test**:

```
$ cd /spring-petclinic
$ sudo ./run-application.sh
```

4. After the application has successfully started, launch the JMeter on the **system client test**:

```
$ cd /spring-petclinic
$ sudo ./run-jmeter.sh
```

**Notes**:
- `sudo` mode is needed, otherwise the tests will not be executed

### Renaissance Benchmark Suite

1. Download the [Renaissance release](https://github.com/renaissance-benchmarks/renaissance/releases)
2. Open the [run-benchmarks.sh](./renaissance/run-benchmarks.sh) script and update the mandatory variables `JAVA_HOME`, `APP_HOME`
3. Launch the benchmarks:

```
$ cd /renaissance
$ sudo ./run-benchmarks.sh
```

**Notes**:
- `sudo` mode is needed, otherwise the benchmarks will not be executed

### Quarkus Hibernate ORM Panache Quickstart


1. Clone the repository [quarkus-quickstarts](https://github.com/quarkusio/quarkus-quickstarts) and build the **hibernate-orm-panache-quickstart** sources
2. Open the [run-application.sh](./quarkus-hibernate-orm-panache-quickstart/run-application.sh) script and update the mandatory variables `JAVA_HOME`, `APP_HOME`, `POSTGRESQL_DATASOURCE`
3. Open the [run-hyperfoil.sh](./quarkus-hibernate-orm-panache-quickstart/run-hyperfoil.sh) script and update the mandatory variable `HYPERFOIL_HOME`
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

**Notes**:
- `sudo` mode is needed, otherwise the tests will not be executed


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