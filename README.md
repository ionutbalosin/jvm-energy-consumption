# JVM Energy Consumption

This repository contains different Java Virtual Machine (JVM) benchmarks to measure the energy consumption under different loads and with different available off-the-shelf applications.

## Content

- [Methodology](#methodology)
- [Prerequisites](#prerequisites)
- [Measurements](#measurements)
  - [Spring PetClinic](#spring-petclinic)
  - [Renaissance Benchmark Suite](#renaissance-benchmark-suite)
  - [Quarkus Hibernate ORM Panache Quickstart](#quarkus-hibernate-orm-panache-quickstart)
- [License](#license)

## Methodology

To measure energy consumption, Intel’s Running Average Power Limit (**RAPL**) interface is used. RAPL provides power-limiting features and accurate energy readings for several domains, like Package, Core, Uncore, and DRAM. For each supported power domain, a Machine Specific Register (MSR) filled with a 32-bit integer is exposed and updated at intervals of approximately 1 millisecond.

Since the **RAPL** reports the entire energy of a host machine, it is important to minimize the load on the target machine and run only the application in charge. In addition, a baseline of the entire system (without any explicit load) should be measured.

While measuring the JVM energy consumption, it is important to have a realistic load, otherwise, the Gargabe Collector footprint or further Just-In-Time compiler optimizations are simply skipped and makes the measurements less relevant. Just starting and stopping the application is not an option.

The command pattern used to measure the energy relies on `perf`: 

```
$ perf stat -a \
   -e "power/energy-cores/" \
   -e "power/energy-gpu/" \
   -e "power/energy-pkg/" \
   -e "power/energy-psys/" \
   -e "power/energy-ram/" \
   <application_runner>
```

The table below summarizes the full list of JVM distributions included in the measurements:

No. | JVM distribution
-------------- |--------------------
1 | [OpenJDK HotSpot](https://projects.eclipse.org/projects/adoptium.temurin/downloads)
2 | [GraalVM CE](https://www.graalvm.org/downloads)
3 | [GraalVM EE](https://www.graalvm.org/downloads)
4 | [Native-Image](https://www.graalvm.org/22.0/reference-manual/native-image/)
5 | [Azul Prime (Zing)](https://www.azul.com/products/prime)
6 | [Eclipse OpenJ9](https://www.eclipse.org/openj9) 

## Prerequisites

In order to properly run the scripts you need to:
- install `perf` on Linux
- download and install [JMeter](https://jmeter.apache.org/download_jmeter.cgi), including a few plugins. These plugins are needed to generate the plots, after each load test:
    - [Command-Line Graph Plotting Tool](https://jmeter-plugins.org/wiki/JMeterPluginsCMD)
    - [Filter Results Tool](https://jmeter-plugins.org/wiki/FilterResultsTool)
    - [Synthesis Report](https://jmeter-plugins.org/wiki/SynthesisReport)
    - [Response Times Over Time](https://jmeter-plugins.org/wiki/ResponseTimesOverTime)
    - [Response Times vs Threads](https://jmeter-plugins.org/wiki/ResponseTimesVsThreads)
    - [Response Times Distribution](https://jmeter-plugins.org/wiki/RespTimesDistribution)
- download and install any JDK (you could also use [sdkman](https://sdkman.io/install))

> **JMeter**: if the number of threads is not properly chosen, the [Coordinated Omission](https://groups.google.com/g/mechanical-sympathy/c/icNZJejUHfE) problem might cause inaccurate results. Please have a look at [JMeter best practices](https://jmeter.apache.org/usermanual/best-practices.html).

## Measurements

### Spring PetClinic

1. Clone the repository [spring-petclinic](https://github.com/spring-projects/spring-petclinic) and build the sources
2. Open the [run-application.sh](./spring-petclinic/run-application.sh) script and update the mandatory variables `JAVA_HOME`, `APP_HOME`
2. Open the [run-jmeter.sh](./spring-petclinic/run-jmeter.sh) script and update the mandatory variable `JMETER_HOME`
3. Launch the scripts (on Host 1)

```
$ cd /spring-petclinic
$ sudo ./run-application.sh
```

After the application successfully started, launch the JMeter on a different host (e.g., Host 2):

```
$ cd /spring-petclinic
$ sudo ./run-jmeter.sh
```

**Notes**:
- `sudo` mode is needed, otherwise the tests will not be executed
- for more accurate results, please launch the application and the JMeter on different hosts. In addition, both Host 1 and Host 2 must have a good and stable connection in between (wireless might not be recommended).

### Renaissance Benchmark Suite

1. Download the [Renaissance release](https://github.com/renaissance-benchmarks/renaissance/releases)
2. Open the [run-benchmarks.sh](./renaissance/run-benchmarks.sh) script and update the mandatory variables `JAVA_HOME`, `APP_HOME`
3. Launch the benchmarks

```
$ cd /renaissance
$ sudo ./run-benchmarks.sh
```

**Notes**:
- `sudo` mode is needed, otherwise the benchmarks will not be executed

### Quarkus Hibernate ORM Panache Quickstart

TODO

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