# Analyzing JVM Energy Consumption for JDK 21: An Empirical Study

**Status: DRAFT, work in progress version. Please do not share, publish it at this stage.**

# Content

- [Introduction](#introduction)
  - [The importance of power consumption in modern computing](#the-importance-of-power-consumption-in-modern-computing)
  - [Motivation](#motivation)
  - [Objectives](#objectives)
- [Methodology](#methodology)
  - [Measurement Tools](#measurement-tools)
  - [Key Measurement Considerations](#key-measurement-considerations)
  - [Hardware Configuration](#hardware-configuration)
  - [Application Categories](#application-categories)
  - [JVM Coverage](#jvm-coverage)
- [Applications Runtime Execution Results](#applications-runtime-execution-results)
  - [Off-the-Shelf Applications](#off-the-shelf-applications)
    - [Spring PetClinic Application](#spring-petclinic-application)
    - [Quarkus Hibernate ORM Panache](#quarkus-hibernate-orm-panache)
  - [Custom-Made Java Applications](#custom-made-java-applications)
    - [Memory Access patterns](#memory-access-patterns)
    - [Logging Patterns](#logging-patterns)
    - [Throwing Exception Patterns](#throwing-exception-patterns)
    - [String Concatenation Patterns](#string-concatenation-patterns)
    - [Sorting Algorithms Complexities](#sorting-algorithms-complexities)
    - [Virtual Calls](#virtual-calls)
    - [Virtual/Platform Threads](#virtualplatform-threads)
  - [Runtime Normalized Energy](#runtime-normalized-energy)
- [Applications Build Time Execution Results](#applications-build-time-execution-results)
  - [Build Time Normalized Energy](#build-time-normalized-energy)
- [How energy consumption correlates with performance](#how-energy-consumption-correlates-with-performance)
- [From energy consumption to carbon emissions](#from-energy-consumption-to-carbon-emissions)
- [Conclusions](#conclusions)
- [Future Work](#future-work)
- [Acknowledgements](#acknowledgements)
- [References](#references)

# Introduction

## The Importance of Power Consumption in Modern Computing

Power consumption is a crucial consideration in modern computing. Firstly, it directly impacts the energy efficiency of devices, contributing to reduced electricity costs and environmental sustainability. With the proliferation of technology and the increasing number of devices we use, minimizing power consumption helps conserve energy resources.

Power consumption plays a role in thermal management. High power consumption generates more heat, which can lead to increased temperatures within devices. Effective thermal management is vital to prevent overheating and maintain optimal performance and reliability.

Lastly, power consumption is a consideration in data centers and large-scale computing infrastructure. These facilities consume massive amounts of energy, and reducing power consumption can result in significant cost savings and a smaller environmental footprint.

Overall, managing power consumption is important in modern computing to promote energy efficiency, ensure thermal stability, and support sustainable practices.

## Motivation

Conducting energy consumption experiments can provide valuable insights and benefits. Here are a few reasons that lead me to conduct such an experiment:
- **Curiosity, Innovation and Research**: These energy consumption experiments were a fascinating field of exploration to me. By conducting them, I hoped to discover techniques, approaches that help me to further minimize energy consumption on real applications.
- **Energy Efficiency**: By measuring energy consumption, we can identify opportunities to optimize energy usage. Such experiments can help us understand how different software configurations, algorithms, or hardware choices affect power usage. This knowledge can lead to more energy-efficient designs, reduced electricity costs, and a smaller environmental impact.
- **Sustainable Computing**: Energy consumption experiments align with the growing emphasis on sustainability in technology. By investigating and mitigating power inefficiencies, we actively contribute to reducing energy waste and minimizing the carbon footprint associated with computing.

## Objectives

Below is a list of several objectives I considered for my experiments:

- **Comparative Analysis**: Compare the energy consumption of different types of applications (and code patterns) running on different Java Virtual Machines (JVM), to identify variations and determine which JVMs are more energy-efficient.
- **Power Measurement Techniques**: An approach about how to run applications under different workloads and measure the overall energy consumption. 
- **Performance-Optimized Power Efficiency**: Investigate how energy consumption correlates with system performance.

> Please note that this analysis **does not primarily focus on JVM performance comparison**. The intention is to understand the energy consumption of a JVM under specific loading factors, rather than determining the fastest JVM. Therefore, please refrain from viewing it from a performance standpoint. All throughput-related plots are included to provide complementary insights into the system's load.

# Methodology

## Measurement Tools

The command line tools I used for measuring energy consumption, as well as additional tools utilized to either generate a proper load or monitor some basic system hardware counters (e.g., CPU, memory), vary based on the architecture, as follows:

 OS        | Arcitecture   | Power Consumption Tools | Auxiliar Tools         
-----------|---------------|-------------------------|----------------
 GNU/Linux | x86_64        | `powerstat`             | `wrk`, `ps`
 macOS     | arm64         | `powermetrics`          | `wrk`, `ps`

To achieve a more comprehensive measurement, I supplemented these measurements with the use of a **wall power meter**.

### Powerstat

[Powerstat](https://github.com/ColinIanKing/powerstat) measures the power consumption of a machine using the battery stats or the Intel's Running Average Power Limit (**RAPL**) interface.

RAPL offers power-limiting capabilities and precise energy readings for multiple power domains. Each supported power domain exposes a Machine Specific Register (MSR) containing a 32-bit integer, which is updated at approximately 1-millisecond intervals. The RAPL power domains include:

- Package (PKG) domain: Measures the energy consumption of the entire socket, including all cores, integrated graphics, and uncore components like last-level caches and memory controller.
- Power Plane 0 (PP0) domain: Measures the energy consumption of all processor cores on the socket.
- Power Plane 1 (PP1) domain: Measures the energy consumption of the processor graphics (GPU) on the socket (desktop models only).
- DRAM domain: Measures the energy consumption of the random access memory (RAM) attached to the integrated memory controller.
- PSys domain: Introduced with Intel Skylake, it monitors and controls the thermal and power specifications of the entire SoC (System on a Chip). It is especially useful when the power consumption source is neither the CPU nor the GPU. PSys includes power consumption from the package domain, System Agent, PCH, eDRAM, and other domains within a single-socket SoC.

**Note:** The DRAM domain has been [deprecated on newer Intel CPUs](https://edc.intel.com/content/www/br/pt/design/products/platforms/details/raptor-lake-s/13th-generation-core-processors-datasheet-volume-1-of-2/006/deprecated-technologies), starting with the 13th Generation.

#### RAPL Domains Coverage

It is worth mentioning that RAPL reports only the energy consumption of a few domains (e.g., CPU, GPU, probably DRAM), but the system overall consumes much more energy for other components that are not included, as follows:
- any networking interface as Ethernet, Wi-Fi (Wireless Fidelity), Bluetooth, etc.
- any attached storage device (as hard disk drives, solid-state drives, and optical drives) relying on SATA (Serial AT Attachment), NVMe (Non-Volatile Memory Express), USB (Universal Serial Bus), Thunderbolt, SCSI (Small Computer System Interface), FireWire (IEEE 1394), Fibre Channel, etc.
- any display interface using HDMI (High-Definition Multimedia Interface), VGA (Video Graphics Array), DVI (Digital Visual Interface), Thunderbolt, DisplayPort, etc.
- the motherboard
- etc.

In other words, for a typical JVM application, this means that any I/O operation that involves reading data from or writing data to any storage device but also any networking operation that uses I/O to send or receive data over a network are not captured.

### Powermetrics

`Powermetrics` is a utility tool available on macOS systems, designed to provide detailed insights into energy consumption and system performance. It can monitor various aspects of system behavior in real-time, including CPU utilization, thermal levels, power usage, energy efficiency metrics, and identify energy-intensive processes or applications.

Similar to RAPL interface, for a typical JVM application, any I/O operation involving reading from or writing to a storage device, as well as any networking operation using I/O to send or receive data over a network, is not captured.

### Wall Power Meter

A wall power meter directly measures the power consumption at the socket, providing a holistic view of the energy consumed by the entire system, including the CPU, GPU, memory, storage, and other peripherals.

By using a wall power meter alongside reported command-line power monitoring statistics, we can obtain more comprehensive measurements of the energy consumption of an application from different perspectives, both inside and outside of the JVM.

### wrk

[wrk](https://github.com/wg/wrk) is a modern HTTP benchmarking tool capable of generating significant load when run on a single multi-core CPU.

Even though `wrk`'s reported latency histogram suffers from **coordinated omission**, latency was not the focus of my measurements. Instead, I relied on the load generated by `wrk`, including the maximum throughput, as key factors to assess the energy consumption of a highly loaded system.

For latency-sensitive measurements, `wrk` might be complemented with [wrk2](https://github.com/giltene/wrk2). However, this is not within the scope of these energy consumption-focused measurements.

### ps

`ps` was utilized throughout the measurement duration to monitor the JVM process, capturing basic hardware counters primarily related to the memory and CPU utilization (e.g., CPU, MEM, Resident Set Size (RSS), Virtual Memory Size (VSZ), Proportional Set Size (PSS)). Its role was to ensure that the system maintained an appropriate load level - not underloaded nor overloaded - while energy consumption was being measured.

## High-Level Architecture

The high-level target system architecture, including all components, looks as follows.

[![jvm-energy-consumption-article-jdk21.svg](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/docs/load-test-system-target-architecture.svg?raw=true)](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/docs/load-test-system-target-architecture.svg?raw=true)

## Key Measurement Considerations

When conducting power measurements, it's important to consider the following key points:

### Have Baseline Measurements

The command line tools (e.g., `powerstat`, `powermetrics`) measures all components and applications running on the same JVM host machine. However, they do not provide a breakdown of power consumption per individual JVM process or component.

Therefore, it's very important to establish a **baseline measurement** of the system's power consumption during idle or minimal background processes, which must then be subtracted from the actual measurements.

### Run With a Properly Loaded JVM

The JVM process must be adequately loaded (in other words, it should not be in an idle state or running very low workloads). Otherwise, the accuracy of power consumption measurement might not capture significant results.

As both `powerstat` and `powermetrics` primarily target the CPU, it's essential to monitor the CPU usage and ensure that it is properly loaded, in accordance with the number of physical cores.

### Eliminate The Noise

Excessive heat can impact both the overall power consumption and performance of a system, indirectly affecting power consumption measurements. For this reason, disabling both **turbo-boost** and **hyper-threading** can effectively reduce CPU heat and improve the consistency of power consumption measurements.

Other sources of potential power consumption instabilities include:

- Ensuring the machine (or laptop) is fully charged and connected to a stable power source.
- Disabling all potential sources of instability, such as scheduled updates, scans, and (Wi-Fi) networking, etc.
- Avoiding running any other user-intensive applications simultaneously. It is usually recommended not to use the machine for any other tasks while conducting measurements.
- Variations in ambient temperature.

### Limit Micro-benchmarking

Measuring energy consumption for smaller tasks (i.e., micro-benchmarking), such as short code instructions, that run very quickly can be challenging or even less relevant. In such cases, the overall results are often dominated by the JVM footprint rather than the specific code being tested.

## Hardware Configuration

The system under test, where the JVM applications were launched, has the following configuration:
- CPU: Intel Core i9-13900HX Raptor Lake
- Memory: 64GB DDR5 5200 MHz
- OS: Ubuntu 23.10 / 6.5.0-25-generic

The test client machine where `wrk` was launched has the following configuration:
- CPU: Intel i7-8550U Kaby Lake R
- Memory: 32GB DDR4 2400 MHz
- OS: Ubuntu 22.04.2 LTS / 6.5.0-25-generic

The specific model of the wall power meter used is the [Ketotek KTEM02-1](https://www.amazon.de/-/en/dp/B0B2953JM5).

## Application Categories

Multiple application categories were included in these measurements.

**Off-the-shelf applications**, such as:
  - [Spring PetClinic](https://github.com/spring-projects/spring-petclinic)
  - [Quarkus Hibernate ORM Panache](https://github.com/quarkusio/quarkus-quickstarts/tree/main/hibernate-orm-panache-quickstart)

**Custom-made Java applications** relying on specific (but generaly common) code patterns, such as:
  - Logging patterns
  - Memory access patterns
  - Throwing exception patterns
  - String Concatenation Patterns
  - (Sorting) algorithms complexities
  - Virtual calls
  - Virtual/Physical thread

## JVM Coverage

The list of included JMVs is:

JVM distribution                                                                        | JDK version |Architecture
----------------------------------------------------------------------------------------|-------------|---------------
[OpenJDK HotSpot VM](https://projects.eclipse.org/projects/adoptium.temurin/downloads)  | 21.0.1      |x86_64
[GraalVM CE 21](https://www.graalvm.org/downloads)                                      | 21.0.1      |x86_64
[Oracle GraalVM 21](https://www.graalvm.org/downloads)                                  | 21.0.1      |x86_64
[Graal Native Image (shipped with Oracle GraalVM 21)](https://www.graalvm.org/downloads)| 21.0.1      |x86_64
[Azul Prime VM](https://www.azul.com/products/prime)                                    | 21.0.1      |x86_64
[Eclipse OpenJ9 VM](https://www.eclipse.org/openj9)                                     | 21.0.1      |x86_64

For each JVM, the specific tuning parameters were the initial heap size, typically set to 1m (e.g., `-Xms1m`), and the maximum heap size, which varies depending on the application category (e.g., `-Xmx1g`, `-Xmx8g`, `-Xmx12g`).

Only in the case of Graal Native Image additional compilation parameters were used (i.e., **Profile-Guided Optimizations**, and **G1 GC**). 
This was primarily to enable a fairer comparison between the native image and other JVMs (i.e., AOT vs JIT, serial GC vs other parallel and concurrent garbage collectors) in the case of long-running applications.

# Applications Runtime Execution Results

This section presents the measurement results obtained during the execution of each application category.

## Off-the-Shelf Applications

For each category of off-the-shelf application, the total running time is set to **2 hours**. Within this 2-hour runtime, various metrics are captured:

- The **intermediate power consumption** metrics (including thermal zone sensors, package temperature, etc.) are sampled every second.
- Memory and CPU utilization metrics (including CPU, MEM, RSS, VSZ, PSS) are sampled every second.

The **total end-to-end energy** consumption is then calculated based on the intermediate power consumption over the 2-hour sampling interval. 

This approach considers the energy consumed from the moment an application starts until it stops, taking into account factors such as JVM initialization and application warmup periods. 
The rationale behind this approach is to reflect the total energy consumption (i.e., the total bill), encompassing all phases, rather than just the ideal state of each JVM. 
While this approach may initially appear to favor short-running applications that are Ahead-of-Time compiled, running them for a longer duration (e.g., 2 hours) enables a more realistic evaluation.

### Spring PetClinic Application

This experiment assesses the energy consumption of the [Spring PetClinic](https://github.com/spring-projects/spring-petclinic) application based on Spring Boot 3.2.1 setting a maximum heap size of `12GB`.

The load generator (and consequently the maximum throughput reported) is triggered using `wrk`, which hits five different endpoints (creating owners, pets, and clinic visits, and reading the owners, pets, and their visits) in a round-robin manner, using 8 threads at a rate of 900 requests per second for a duration of 2 hours.

An important note to make is that during the benchmarking time, all requests consist solely of CREATE/READ operations, continuously adding/retrieving object entities to the embedded in-memory database. From a Garbage Collector's perspective, this results in the retained heap memory continuously growing.

#### Total End-to-End Energy Consumption

[![SpringPetClinicEnergyConsumption.svg](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/off-the-shelf-applications/spring-petclinic/results/jdk-21/x86_64/linux/plot/energy-report-run.svg?raw=true)](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/off-the-shelf-applications/spring-petclinic/results/jdk-21/x86_64/linux/plot/energy-report-run.svg?raw=true)

*This plot represents the total energy consumption during the entire duration (e.g., 2 hours) of the JVM, including all phases.*

#### Intermediate Power Consumption

[![SpringPetClinicPowerConsumption.svg](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/off-the-shelf-applications/spring-petclinic/results/jdk-21/x86_64/linux/plot/raw-power-run.svg?raw=true)](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/off-the-shelf-applications/spring-petclinic/results/jdk-21/x86_64/linux/plot/raw-power-run.svg?raw=true)

*This plot represents the evolution of power consumption (sampled every second) throughout the entire duration (e.g., 2 hours) of the JVM.*

During the final part of this plot, the power consumption decreases for all JVMs. This is because the load test generator finishes slightly ahead of the JVM, relieving the pressure on JVM threads.

#### wrk Throughput

[![SpringPetClinicWrkThroughput.svg](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/off-the-shelf-applications/spring-petclinic/results/jdk-21/x86_64/linux/plot/performance-report-run.svg?raw=true)](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/off-the-shelf-applications/spring-petclinic/results/jdk-21/x86_64/linux/plot/performance-report-run.svg?raw=true)

*This plot represents the maximum throughput reported by wrk at the end of the load test execution.*

Additional resources:
- wrk [load test](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/off-the-shelf-applications/spring-petclinic/test-plan.lua) plan
- wrk [reports](https://github.com/ionutbalosin/jvm-energy-consumption/tree/main/off-the-shelf-applications/spring-petclinic/results/jdk-21/x86_64/linux/wrk)

### Quarkus Hibernate ORM Panache

This experiment assesses the energy consumption of the [Quarkus Hibernate ORM Panache](https://github.com/quarkusio/quarkus-quickstarts/tree/main/hibernate-orm-panache-quickstart) application based on Quarkus 3.8.2 setting a maximum heap size of `1GB`.

The load generator (and consequently the maximum throughput reported) is triggered using `wrk`, which hits four different endpoints (creating, reading, updating, and deleting fruits) in a round-robin manner, using 8 threads at a rate of 900 requests per second for a duration of 2 hours.

An important note to make is that during the benchmarking time, all requests consist of CREATE/READ/UPDATE/DELETE operations on object entities from the embedded in-memory database. From a Garbage Collector's perspective, this results in a relatively small retained heap memory.

#### Total End-to-End Energy Consumption

[![QuarkusHibernateORMPanacheEnergyConsumption.svg](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/off-the-shelf-applications/quarkus-hibernate-orm-panache-quickstart/results/jdk-21/x86_64/linux/plot/energy-report-run.svg?raw=true)](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/off-the-shelf-applications/quarkus-hibernate-orm-panache-quickstart/results/jdk-21/x86_64/linux/plot/energy-report-run.svg?raw=true)

*This plot represents the total energy consumption during the entire duration (e.g., 2 hours) of the JVM, including all phases.*

#### Intermediate Power Consumption

[![QuarkusHibernateORMPanachePowerConsumption.svg](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/off-the-shelf-applications/quarkus-hibernate-orm-panache-quickstart/results/jdk-21/x86_64/linux/plot/raw-power-run.svg?raw=true)](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/off-the-shelf-applications/quarkus-hibernate-orm-panache-quickstart/results/jdk-21/x86_64/linux/plot/raw-power-run.svg?raw=true)

*This plot represents the evolution of power consumption (sampled every second) throughout the entire duration (e.g., 2 hours) of the JVM.*

During the final part of this plot, the power consumption decreases for all JVMs. This is because the load test generator finishes slightly ahead of the JVM, relieving the pressure on JVM threads.

#### wrk Throughput

[![QuarkusHibernateORMPanacheWrkThroughput.svg](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/off-the-shelf-applications/quarkus-hibernate-orm-panache-quickstart/results/jdk-21/x86_64/linux/plot/performance-report-run.svg?raw=true)](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/off-the-shelf-applications/quarkus-hibernate-orm-panache-quickstart/results/jdk-21/x86_64/linux/plot/performance-report-run.svg?raw=true)

*This plot represents the maximum throughput reported by wrk at the end of the load test execution.*

Additional resources:
- wrk [load test](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/off-the-shelf-applications/quarkus-hibernate-orm-panache-quickstart/test-plan.lua) plan
- wrk [reports](https://github.com/ionutbalosin/jvm-energy-consumption/tree/main/off-the-shelf-applications/quarkus-hibernate-orm-panache-quickstart/results/jdk-21/x86_64/linux/wrk)

## Custom-Made Java Applications

In addition to the off-the-shelf applications, a collection of custom-made Java (mini) programs employing various coding paradigms was developed. These programs encompass the most common paradigms encountered in the majority of commercial Java applications.

For each category of custom-made application, the total running time is set to **20 minutes** and the maximum heap size is `8GB`.
During this 20-minute runtime, consecutive runs of the same application are triggered until the time limit is reached.
The reported throughput is computed at the end of the application execution runs, excluding a fixed initial warm-up duration of 5 minutes, which is considered sufficient for these micro-benchmarks..

During each application execution, **intermediate power consumption metrics**, including the thermal zone sensors and package temperature, are collected at a sampling interval of one second.

The **total end-to-end energy consumption** is then calculated based on the intermediate power consumption recorded over the 20-minute sampling interval.

This approach considers the energy consumed from the moment an application starts until it stops, taking into account factors such as JVM initialization and application warmup periods.
The rationale behind this approach is to reflect the total energy consumption (i.e., the total bill), encompassing all phases, rather than just the ideal state of each JVM.

### Memory Access Patterns

This program aims to analyze the relationship between memory access patterns and energy consumption. There are three primary memory access patterns:
- **Temporal**: memory that has been recently accessed is likely to be accessed again in the near future.
- **Spatial**: adjacent memory locations are likely to be accessed in close succession.
- **Striding**: memory access follows a predictable pattern, typically with a fixed interval between accesses.

The program creates a large array of longs, occupying approximately `4GB` of RAM memory. Then, over a period of 20 minutes, consecutive runs access the array elements based on one of the described patterns. After each iteration, the validity of the results is verified.

Source code: [MemoryAccessPatterns.java](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/src/main/java/com/ionutbalosin/jvm/energy/consumption/MemoryAccessPatterns.java)

#### Total End-to-End Energy Consumption

[![MemoryAccessPatternsEnergyConsumption.svg](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/results/jdk-21/x86_64/linux/MemoryAccessPatterns/plot/energy-report-run.svg?raw=true)](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/results/jdk-21/x86_64/linux/MemoryAccessPatterns/plot/energy-report-run.svg?raw=true)

*This plot represents the total energy consumption during the entire duration (e.g., 20 minutes) of the JVM , including all phases.*

#### Throughput

[![MemoryAccessPatternsThroughput.svg](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/results/jdk-21/x86_64/linux/MemoryAccessPatterns/plot/performance-report-run.svg?raw=true)](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/results/jdk-21/x86_64/linux/MemoryAccessPatterns/plot/performance-report-run.svg?raw=true)

*This plot represents the computed throughput at the end of the load test execution after excluding a fixed initial warmup duration of 5 minutes.*

#### Remarks

The variation in trends observed in energy consumption does not differ as significantly as the trends observed in throughput.

Based on the hardware I tested (especially for this application), the power consumption differences are smaller and less obvious compared to the differences in throughput across the three categories of memory access patterns.
The JVM footprint looks to dominate the energy consumption and not specifically memory access pattern itself.

### Logging Patterns

When it comes to logging, performance is one of the major concerns. The manner in which we log and the volume of logs can significantly impact the performance of our applications. This is due to the associated costs of heap allocations and the additional work performed by the garbage collector to clean up the heap. In addition to allocations, there are also expenses related to I/O operations when writing and flushing data to disk. All of these factors contribute to increased utilization of hardware resources (e.g., CPU and memory), resulting in higher energy consumption, which is reflected in our monthly bills.

The program measures various logging patterns using UTF-16 characters. It runs in multiple iterations over a period of 20 minutes, and within each iteration, the logging framework (e.g., `java.util.logging.Logger`) is invoked. After each iteration, the validity of the results is verified.

It is crucial to note that none of these logs are physically written to disk; instead, they are written to the `Null OutputStream`. This approach is preferable since the power consumption tools cannot capture any I/O-related power activity.

Source code: [LoggingPatterns.java](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/src/main/java/com/ionutbalosin/jvm/energy/consumption/LoggingPatterns.java)

#### Total End-to-End Energy Consumption

[![LoggingPatternsEnergyConsumption.svg](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/results/jdk-21/x86_64/linux/LoggingPatterns/plot/energy-report-run.svg?raw=true)](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/results/jdk-21/x86_64/linux/LoggingPatterns/plot/energy-report-run.svg?raw=true)

*This plot represents the total energy consumption during the entire duration (e.g., 20 minutes) of the JVM , including all phases.*

#### Throughput

[![LoggingPatternsThroughput.svg](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/results/jdk-21/x86_64/linux/LoggingPatterns/plot/performance-report-run.svg?raw=true)](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/results/jdk-21/x86_64/linux/LoggingPatterns/plot/performance-report-run.svg?raw=true)

*This plot represents the computed throughput at the end of the load test execution after excluding a fixed initial warmup duration of 5 minutes.*

#### Remarks

Across all results, one interesting observation is that Eclipse OpenJ9 VM consumes less energy overall but also exhibits the lowest throughput compared to other JVMs.

In other cases, there are situations where, for example, Azul Prime VM in the `guarded_parametrized` scenario consumes the most energy but offers the best throughput. 

Additionally, there are cases where one JVM does not necessarily consume the most energy but offers a very good throughput, as seen with Native Image with PGO in the `lambda` and `guarded_unparametrized` scenarios.

### Throwing Exception Patterns

Similar to logging, the creation, throwing, and handling of exceptions introduce additional runtime overhead, impacting both the performance and energy consumption of software applications.

This program measures different exception throwing patterns. It runs in multiple iterations over a period of 20 minutes, and in each iteration, a different type of exception is thrown when the execution stack reaches a specific depth (in this case, `1024`). After each iteration, the validity of the results is verified.

It is worth noting that the depth of the call stack can also impact performance, and the time spent on filling in the stack trace (abbreviated `first`) dominates the associated costs.

Source code: [ThrowExceptionPatterns.java](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/src/main/java/com/ionutbalosin/jvm/energy/consumption/ThrowExceptionPatterns.java)

#### Total End-to-End Energy Consumption

[![ThrowExceptionPatternsEnergyConsumption.svg](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/results/jdk-21/x86_64/linux/ThrowExceptionPatterns/plot/energy-report-run.svg?raw=true)](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/results/jdk-21/x86_64/linux/ThrowExceptionPatterns/plot/energy-report-run.svg?raw=true)

*This plot represents the total energy consumption during the entire duration (e.g., 20 minutes) of the JVM , including all phases.*

#### Throughput

[![ThrowExceptionPatternsThroughput.svg](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/results/jdk-21/x86_64/linux/ThrowExceptionPatterns/plot/performance-report-run.svg?raw=true)](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/results/jdk-21/x86_64/linux/ThrowExceptionPatterns/plot/performance-report-run.svg?raw=true)

*This plot represents the computed throughput at the end of the load test execution after excluding a fixed initial warmup duration of 5 minutes.*

#### Remarks

It's very obvious here that the energy consumption trends are more stable and less fluctuating compared to the throughput trends.
While the throughput trends vary significantly across different exception throwing patterns, the differences in energy consumption across the JVMs are not as pronounced.

Again, the JVM footprint dominates the energy consumption costs, diminishing the effects of the different exception throwing patterns in this microbenchmark.

### String Concatenation Patterns

This program assesses the energy consumption of various concatenation methods using different data types (e.g., `String`, `int`, `float`, `char`, `long`, `double`, `boolean`, `Object`), employing common techniques such as `StringBuilder`, `plus` operator, and `String Template`.
It runs in multiple iterations over a period of 20 minutes, and, after each iteration, the validity of the results is verified.
The input characters are UTF-16 encoded.

**Note:** This benchmark may involve different allocations, potentially impacting the overall results.

Source code: [StringConcatenationPatterns.java](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/src/main/java/com/ionutbalosin/jvm/energy/consumption/StringConcatenationPatterns.java)

#### Total End-to-End Energy Consumption

[![StringConcatenationPatternsEnergyConsumption.svg](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/results/jdk-21/x86_64/linux/StringConcatenationPatterns/plot/energy-report-run.svg?raw=true)](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/results/jdk-21/x86_64/linux/StringConcatenationPatterns/plot/energy-report-run.svg?raw=true)

*This plot represents the total energy consumption during the entire duration (e.g., 20 minutes) of the JVM , including all phases.*

#### Throughput

[![StringConcatenationPatternsThroughput.svg](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/results/jdk-21/x86_64/linux/StringConcatenationPatterns/plot/performance-report-run.svg?raw=true)](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/results/jdk-21/x86_64/linux/StringConcatenationPatterns/plot/performance-report-run.svg?raw=true)

*This plot represents the computed throughput at the end of the load test execution after excluding a fixed initial warmup duration of 5 minutes.*

#### Remarks

One interesting observation here is that the Eclipse OpenJ9 VM consumes less energy overall but also exhibits the lowest throughput compared to other JVMs.

In other cases, for example, Native Image (with and without PGO), as well as the Azul Prime VM, consume the most energy but offer better throughput as a trade-off.

### Sorting Algorithms Complexities

This program utilizes various sorting algorithms with different complexities, ranging from logarithmic to linear, to sort an array of integers occupying `1GB` of memory. 
It runs multiple iterations over a 20-minute period, and after each iteration, the validity of the results is verified. 
Prior to each iteration, the array is initialized in reverse order, thereby creating a worst-case scenario for the sorting algorithms.

Source code: [SortingAlgorithms.java](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/src/main/java/com/ionutbalosin/jvm/energy/consumption/SortingAlgorithms.java)

#### Total End-to-End Energy Consumption

*This plot represents the total energy consumption during the entire duration (e.g., 20 minutes) of the JVM , including all phases.*

[![SortingAlgorithmsEnergyConsumption.svg](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/results/jdk-21/x86_64/linux/SortingAlgorithms/plot/energy-report-run.svg?raw=true)](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/results/jdk-21/x86_64/linux/SortingAlgorithms/plot/energy-report-run.svg?raw=true)

#### Throughput

[![SortingAlgorithmsThroughput.svg](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/results/jdk-21/x86_64/linux/SortingAlgorithms/plot/performance-report-run.svg?raw=true)](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/results/jdk-21/x86_64/linux/SortingAlgorithms/plot/performance-report-run.svg?raw=true)

*This plot represents the computed throughput at the end of the load test execution after excluding a fixed initial warmup duration of 5 minutes.*

#### Remarks

It's also very obvious here that the energy consumption trends are more stable and less fluctuating compared to the throughput trends.

In some cases, one JVM consumes less energy compared to others but offers better throughput, as seen with Native Image in the `radix_sort` scenario, or with Oracle GraalVM in the `merge_sort` scenario.

While algorithm complexities can impact energy consumption, the relationship is not always straightforward.
In theory, algorithms with higher time or space complexities would generally require more computational effort to execute, leading to increased energy consumption. 
However, when running these algorithms on hardware, there are a few additional factors to consider. 
- **Memory access patterns**: Algorithms with poor memory access patterns, such as excessive random or cache-unfriendly accesses, can increase energy consumption.
- **The underlying hardware**: The characteristics and efficiency of the hardware on which the algorithm is executed can also affect energy consumption.
- **The JVM footprint**

### Virtual Calls

The program evaluates the energy consumption of virtual calls using various scenarios:

- A virtual call with one target implementation (also known as monomorphic).
- A virtual call with two target implementations (also known as bimorphic).
- A virtual call with three target implementations (also known as megamorphic).
- A virtual call with eight different target implementations (also known as megamorphic).

**Note:** Monomorphic and bimorphic call sites are more commonly encountered, while having 8 target implementations for the same call site is less usual.

Source code: [VirtualCalls.java](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/src/main/java/com/ionutbalosin/jvm/energy/consumption/VirtualCalls.java)

#### Total End-to-End Energy Consumption

[![VirtualCallsEnergyConsumption.svg](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/results/jdk-21/x86_64/linux/VirtualCalls/plot/energy-report-run.svg?raw=true)](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/results/jdk-21/x86_64/linux/VirtualCalls/plot/energy-report-run.svg?raw=true)

*This plot represents the total energy consumption during the entire duration (e.g., 20 minutes) of the JVM , including all phases.*

#### Throughput

[![VirtualCallsThroughput.svg](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/results/jdk-21/x86_64/linux/VirtualCalls/plot/performance-report-run.svg?raw=true)](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/results/jdk-21/x86_64/linux/VirtualCalls/plot/performance-report-run.svg?raw=true)

*This plot represents the computed throughput at the end of the load test execution after excluding a fixed initial warmup duration of 5 minutes.*

#### Remarks

In the case of the Eclipse OpenJ9 VM `bimorphic` use case, a new interesting pattern emerges where it consumes the highest energy and has one of the lowest throughputs.

In all other cases, Oracle GraalVM tends to consume the most energy but also offers the highest throughput.

### Virtual/Platform Threads

This program measures the throughput of producing and consuming elements between producer and consumer threads (both virtual and platform) using a `BlockingQueue`. 
The level of parallelism for both platform and virtual threads is set to the same value to facilitate an evaluation of their performance under comparable conditions.

Source code: [VPThreadQueueThroughput.java](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/src/main/java/com/ionutbalosin/jvm/energy/consumption/VPThreadQueueThroughput.java)

**Note:** It is worth mentioning that the virtual threads use case failed on Azul Prime VM and Eclipse OpenJ9 VM, therefore they were excluded from the energy consumption calculation.

#### Total End-to-End Energy Consumption

[![VPThreadQueueThroughputEnergyConsumption.svg](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/results/jdk-21/x86_64/linux/VPThreadQueueThroughput/plot/energy-report-run.svg?raw=true)](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/results/jdk-21/x86_64/linux/VPThreadQueueThroughput/plot/energy-report-run.svg?raw=true)

*This plot represents the total energy consumption during the entire duration (e.g., 20 minutes) of the JVM , including all phases.*

#### Throughput

[![VPThreadQueueThroughputThroughput.svg](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/results/jdk-21/x86_64/linux/VPThreadQueueThroughput/plot/performance-report-run.svg?raw=true)](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/results/jdk-21/x86_64/linux/VPThreadQueueThroughput/plot/performance-report-run.svg?raw=true)

*This plot represents the computed throughput at the end of the load test execution after excluding a fixed initial warmup duration of 5 minutes.*

## Runtime Normalized Energy

This section describes the normalized energy for all application categories during runtime execution. It is purely informative and provides a high-level understanding of the overall energy consumption scores across all JVMs.

No. | JVM  distribution  | Architecture | Normalized Energy | Phase 
----|--------------------|--------------|-------------------|--------
1   | Native Image       | x86_64       | 0.821             | runtime
2   | Azul Prime VM      | x86_64       | 0.839             | runtime
3   | Eclipse OpenJ9 VM  | x86_64       | 0.870             | runtime
4   | Native Image (PGO) | x86_64       | 0.941             | runtime
5   | Oracle GraalVM     | x86_64       | 0.944             | runtime
6   | GraalVM CE         | x86_64       | 0.964             | runtime
7   | OpenJDK HotSpot VM | x86_64       | 1.000             | runtime

Based on the central tendency of the data, the first in the row can be considered the most eco-friendly JVM, while the last in the row consumes the most energy.

# Applications Build Time Execution Results

This section presents the measurement results obtained during the execution of the build process for each application category, using both Just-in-Time and Ahead-of-Time compilation.

Since they all exhibit a consistent trend in terms of energy consumption across every JVM, I have included only a few of them in this section, representing each distinct application category.

[![SpringPetClinicEnergyConsumption.svg](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/off-the-shelf-applications/spring-petclinic/results/jdk-21/x86_64/linux/plot/energy-report-build.svg?raw=true)](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/off-the-shelf-applications/spring-petclinic/results/jdk-21/x86_64/linux/plot/energy-report-build.svg?raw=true)

[![QuarkusHibernateORMPanacheEnergyConsumption.svg](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/off-the-shelf-applications/quarkus-hibernate-orm-panache-quickstart/results/jdk-21/x86_64/linux/plot/energy-report-build.svg?raw=true)](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/off-the-shelf-applications/quarkus-hibernate-orm-panache-quickstart/results/jdk-21/x86_64/linux/plot/energy-report-build.svg?raw=true)

[![MemoryAccessPatternsEnergyConsumption.svg](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/results/jdk-21/x86_64/linux/MemoryAccessPatterns/plot/energy-report-build.svg?raw=true)](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/results/jdk-21/x86_64/linux/MemoryAccessPatterns/plot/energy-report-build.svg?raw=true)

[![LoggingPatternsEnergyConsumption.svg](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/results/jdk-21/x86_64/linux/LoggingPatterns/plot/energy-report-build.svg?raw=true)](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/results/jdk-21/x86_64/linux/LoggingPatterns/plot/energy-report-build.svg?raw=true)

**Note:** Once the build with native image is done, the resulting binary can be executed multiple times without the need to recompile it (i.e., the compilation cost is paid once), as long as it runs on the specific machine and architecture for which the compilation was performed.

Additional resources:

- [Throw Exception Patterns](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/results/jdk-21/x86_64/linux/ThrowExceptionPatterns/plot/energy-report-build.svg?raw=true) build time energy consumption plot.
- [String Concatenation Patterns](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/results/jdk-21/x86_64/linux/StringConcatenationPatterns/plot/energy-report-build.svg?raw=true) build time energy consumption plot.
- [Sorting Algorithms](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/results/jdk-21/x86_64/linux/SortingAlgorithms/plot/energy-report-build.svg?raw=true) build time energy consumption plot.
- [Virtual Calls](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/results/jdk-21/x86_64/linux/VirtualCalls/plot/energy-report-build.svg?raw=true) build time energy consumption plot.
- [Virtual/Platform Threads](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/results/jdk-21/x86_64/linux/VPThreadQueueThroughput/plot/energy-report-build.svg?raw=true) build time energy consumption plot.

## Build Time Normalized Energy

This section describes the normalized energy geometric mean for all application categories during build time. It is purely informative and provides a high-level understanding of the overall energy consumption scores across all JVMs.

No. | JVM distribution   | Architecture | Normalized Energy | Phase
----|--------------------|--------------|-------------------|-------
2   | GraalVM CE         | x86_64       | 0.980             | build time
3   | OpenJDK HotSpot VM | x86_64       | 1.000             | build time
1   | Oracle GraalVM     | x86_64       | 1.183             | build time
5   | Eclipse OpenJ9 VM  | x86_64       | 1.246             | build time
4   | Azul Prime VM      | x86_64       | 3.586             | build time
6   | Native Image       | x86_64       | 25.187            | build time
4   | Native Image (PGO) | x86_64       | 55.952            | build time

Based on the central tendency of the data, the first in the row can be considered the most eco-friendly JVM, while the last in the row consumes the most energy.

# How energy consumption correlates with performance

Based on the evidence gathered from all of these measurements, there were a mixture of cases where:
- Higher energy consumption correlated with better throughput.
- Higher energy consumption correlated with lower throughput.
- Higher energy consumption did not necessarily result in higher throughput.
- Lower energy consumption did not necessarily result in lower throughput.

Therefore, it is very hard to correlate energy consumption with performance, as each case is unique, and the JVM may incorporate a broader range of optimizations that could improve performance but impact energy consumption on the other side.

In addition, as demonstrated in the experiments, in general the energy consumption trends in the case of any micro application do not correlate (i.e., are not proportional) with performance trends because the JVM footprint has a larger energy footprint overall.

To summarize, there is no direct relationship between energy consumption and performance. In general, energy consumption and performance are trade-offs within a system. While they often support each other, there can be cases where they are not aligned.

# From energy consumption to carbon emissions

Energy consumption and carbon emissions are closely correlated. To convert energy consumption from `Watt⋅sec` to `CO₂` emissions, we would first need to know the energy source (e.g., coal, natural gas, renewable energy) and its associated carbon emissions factor. Next, we multiply the energy consumption by the carbon emissions factor specific to our region (or the region of our data center) for the given energy source.

Let's consider our use case. The table below presents a summary of the total CO₂ emissions for each JVM, calculated based on the energy consumption during applications runtime execution time. 

No. | JVM distribution   | Architecture | Total Energy (Watt⋅sec) | CO₂ Emission Factor (gCO₂eq/kWh) | CO₂ Emissions (gCO₂)
----|--------------------|--------------|-------------------------|----------------------------------|-----------------------
1   | Native Image       | x86_64       | 1,819,642.816           | TODO                             |  TODO                     
2   | Azul Prime VM      | x86_64       | 1,860,940.776           | TODO                             |  TODO                       
3   | Eclipse OpenJ9 VM  | x86_64       | 1,929,280.408           | TODO                             |  TODO
4   | Native Image (PGO) | x86_64       | 2,086,482.972           | TODO                             |  TODO                     
5   | Oracle GraalVM     | x86_64       | 2,092,295.995           | TODO                             |  TODO                  
6   | GraalVM CE         | x86_64       | 2,136,435.392           | TODO                             |  TODO                     
7   | OpenJDK HotSpot VM | x86_64       | 2,217,293.347           | TODO                             |  TODO                      

Based on the total energy consumption, the JVM in the first row consumes less energy overall, while the JVM in the last row emits the highest amount of carbon dioxide.

**Legend:** 
- `CO₂` - carbon dioxide.
- `gCO₂eq/kWh` - grams of carbon dioxide equivalent per kWh.
- `gCO₂` - grams of carbon dioxide.
- `TODO` - is the [current carbon emission factor](https://github.com/ionutbalosin/jvm-energy-consumption/blob/v1.0.0/docs/carbon-emission-factor-17_07_2023-austria.png) for Austria as of TODO, as reported by the [Electricity Maps](https://app.electricitymaps.com/zone/AT) website.

# Conclusions

This article presents an empirical investigation into the variations in energy consumption among key JVM platforms on the x86_64 Intel chipset. The study explores the differences observed when running off-the-shelf web-based applications as well as common code patterns such as logging, memory accesses, exception throwing, algorithms with different time complexities, etc.

The selected JVM implementations exhibit varying levels of energy efficiency depending on the software and workloads tested, often displaying significant differences.

At the cost of increased compilation expenses, GraalVM Native Image showcased the highest energy efficiency overall for the runtime execution.

OpenJDK HotSpot VM, Oracle GraalVM, and GraalVM CE exhibited similar efficiency in the majority of tests, with marginal differences.

Azul Prime VM's energy consumption varied depending on the test case, but generally, it consumed more energy than other HotSpot-based JVMs.

Eclipse OpenJ9 VM exhibited comparatively lower energy efficiency.

When it comes to software development, to write more eco-friendly code (i.e., code with reduced power consumption), programmers can employ various techniques covered in this report (but not only). These techniques include using cache-friendly data structures, avoiding inefficient algorithms, limiting the number of logged lines and thrown exceptions, minimizing object allocations, defining the scope of allocated objects as close as possible to their usage, etc.

This study was conducted using generally available and common features across the selected JVMs, with little to no tuning (i.e., only adjusting the initial and maximum heap size). However, it is important to note that there are specific JVM features available (to improve start-up response times, reducing memory footprint, and thus reducing energy consumption) that might change the picture in a real-world scenario. Examples of such features include Eclipse OpenJ9's [shared class cache (SCC)](https://eclipse.dev/openj9/docs/shrc), Azul Prime VM's [ReadyNow!](https://www.azul.com/products/components/readynow), or the novel technology [CRaC](https://wiki.openjdk.org/display/crac) introduced in the OpenJDK.

Therefore, the report should not be considered as the final determination of the most energy-efficient JVM distribution. Instead, it serves as an initial exploration, providing an approach to quantify energy consumption in real-world application scenarios.

# Future Work

If you have any suggestions or are interested in contributing to this project, please feel free to reach out or open a pull request on [GitHub](https://github.com/ionutbalosin/jvm-energy-consumption). 

Your contributions are welcome and appreciated. 

**Looking forward to contributing to a more eco-friendly world!**

# Acknowledgements

TODO

# References

1. Tom Strempel. Master’s Thesis [Measuring the Energy Consumption of Software written in C on x86-64 Processors](https://ul.qucosa.de/api/qucosa%3A77194/attachment/ATT-0)

2. Spencer Desrochers, Chad Paradis, and Vincent M. Weaver. “A validation of DRAM RAPL power measurements”. In: ACM International Conference Proceed- ing Series 03-06-October-2016 (2016). DOI: [10.1145/2989081.2989088.](https://doi.org/10.1145/2989081.2989088)

3. Zhang Huazhe and Hoffman H. _“A quantitative evaluation of the RAPL power control system”_. In: _Feedback Computing_ (2015).

4. Kashif Nizam Khan et al. “RAPL in action: Experiences in using RAPL for power measurements”. In: ACM Transactions on Modeling and Performance Evaluation of Computing Systems 3 (2 2018). ISSN: 23763647. DOI: [10.1145/3177754](https://doi.org/10.1145/3177754).

5. Zakaria Ournani, Mohammed Chakib Belgaid, Romain Rouvoy, Pierre Rust, Joel Penhoat: [Evaluating the Impact of Java Virtual Machines on Energy Consumption](https://inria.hal.science/hal-03275286/document)

6. Ko Turk: [Green Software Engineering: Best Practices](https://www.adesso.nl/en/news/blog/green-software-engineering-best-practices.jsp)

7. Martin Thompson: [Memory Access Patterns Are Important](https://mechanical-sympathy.blogspot.com/2012/08/memory-access-patterns-are-important.html)

