# Analyzing JVM Energy Consumption for JDK 17: An Empirical Study

# Content

- [Introduction](#introduction)
  - [The importance of power consumption in modern computing](#the-importance-of-power-consumption-in-modern-computing)
  - [Motivation](#motivation)
  - [Objectives](#objectives)
- [Methodology](#methodology)
  - [Measurement Instruments](#measurement-instruments)
    - [RAPL](#rapl)
    - [Wall Power Meter](#wall-power-meter)
    - [RAPL vs. Wall Power Meter](#rapl-vs-wall-power-meter)
  - [Measurement Considerations](#measurement-considerations)
  - [Unit of Measurement](#unit-of-measurement)
  - [Hardware and Software Components](#hardware-and-software-components)
    - [Application Categories](#application-categories)
    - [JVM Coverage](#jvm-coverage)
- [Applications Runtime Execution Results](#applications-runtime-execution-results)
  - [Off-the-Shelf Applications](#off-the-shelf-applications)
    - [Spring PetClinic Application](#spring-petclinic-application)
    - [Quarkus Hibernate ORM Panache](#quarkus-hibernate-orm-panache)
    - [Renaissance Benchmark Suite](#renaissance-benchmark-suite)
  - [Custom-Made Java Applications](#custom-made-java-applications)
    - [Memory Access patterns](#memory-access-patterns)
    - [Logging Patterns](#logging-patterns)
    - [Throwing Exception Patterns](#throwing-exception-patterns)
    - [Sorting Algorithms Complexities](#sorting-algorithms-complexities)
    - [Virtual Calls](#virtual-calls)
  - [Runtime Geometric Mean](#runtime-geometric-mean)
- [Applications Build Time Execution Results](#applications-build-time-execution-results)
  - [Build Time Geometric Mean](#build-time-geometric-mean)
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

# Methodology

## Measurement Instruments

The primary tool I used for measuring energy consumption is Intel's Running Average Power Limit (**RAPL**) interface.

However, to achieve a more comprehensive measurement, I supplemented the reported RAPL statistics with the use of a **wall power meter**. This combination allowed for a thorough assessment of energy usage.

### RAPL

RAPL offers power-limiting capabilities and precise energy readings for multiple power domains. Each supported power domain exposes a Machine Specific Register (MSR) containing a 32-bit integer, which is updated at approximately 1-millisecond intervals. The RAPL power domains include:

- Package (PKG) domain: Measures the energy consumption of the entire socket, including all cores, integrated graphics, and uncore components like last-level caches and memory controller.
- Power Plane 0 (PP0) domain: Measures the energy consumption of all processor cores on the socket.
- Power Plane 1 (PP1) domain: Measures the energy consumption of the processor graphics (GPU) on the socket (desktop models only).
- DRAM domain: Measures the energy consumption of the random access memory (RAM) attached to the integrated memory controller.
- PSys domain: Introduced with Intel Skylake, it monitors and controls the thermal and power specifications of the entire SoC (System on a Chip). It is especially useful when the power consumption source is neither the CPU nor the GPU. PSys includes power consumption from the package domain, System Agent, PCH, eDRAM, and other domains within a single-socket SoC.

In multi-socket server systems, each socket reports its own RAPL values. For example, a two-socket computing system has separate PKG readings for both packages, separate PP0 readings, and so on.

#### RAPL Domains Coverage

It is worth mentioning that RAPL reports only the energy consumption of a few domains (e.g., CPU, GPU, DRAM), but the system overall consumes much more energy for other components that are not included, as follows:
- any networking interface as Ethernet, Wi-Fi (Wireless Fidelity), Bluetooth, etc.
- any attached storage device (as hard disk drives, solid-state drives, and optical drives) relying on SATA (Serial AT Attachment), NVMe (Non-Volatile Memory Express), USB (Universal Serial Bus), Thunderbolt, SCSI (Small Computer System Interface), FireWire (IEEE 1394), Fibre Channel, etc.
- any display interface using HDMI (High-Definition Multimedia Interface), VGA (Video Graphics Array), DVI (Digital Visual Interface), Thunderbolt, DisplayPort, etc.
- the motherboard
- etc.

In other words, for a typical JVM application, this means that any I/O operation that involves reading data from or writing data to any storage device but also any networking operation that uses I/O to send or receive data over a network are not captured.

#### RAPL Validity, and Accuracy

Proof of the measurement methods' validity, which depend on RAPL, is necessary. Therefore, below are some noteworthy RAPL-based studies.

Desrochers et al. [2] measured DRAM energy consumption while minimizing interference, comparing it with RAPL measurements. These findings validate the DRAM domain, utilizing diverse systems and benchmarks. Variances between physical and RAPL measurements are below 20%. Recent processors, like Intel Haswell microarchitecture, exhibit improved precision compared to earlier generations.

Zhang et al. [3] set a power consumption limit using RAPL and evaluated its adherence. Out of 16 benchmarks, 14 had a 2% mean absolute percentage error (MAPE), while 2 had an error rate exceeding 5%. The study also highlighted RAPL's improved accuracy in high energy consumption scenarios.

Khan et al. [4] compared RAPL to wall power measurements in Taito supercomputer, finding a strong 99% correlation. The estimation error (MAPE) was only 1.7%. Performance overhead of reading RAPL was <1%.

Based to these extensive studies, RAPL is considered to be a reliable and widely used tool for power consumption analysis on Intel-based systems.

In addition to that, it is worth noting that on the newer CPUs, including the Intel Haswell microarchitecture, the RAPL precision is better than in previous generations.

### Wall Power Meter

A wall power meter directly measures the power consumption at the socket, providing a holistic view of the energy consumed by the entire system, including the CPU, GPU, memory, storage, and other peripherals.

It provides real-world energy consumption data, which can be particularly useful when evaluating the energy efficiency of an application in practical scenarios.

By using a wall power meter alongside reported RAPL stats, we can obtain more accurate, reliable, and comprehensive measurements of the energy consumption of an application.

### RAPL vs. Wall Power Meter

There are a few differences between these two power measuring methods, that make them complementary rather than interchangeable:

- the sampling periods may differ between RAPL and the wall power meter.
- the measurement scales are also different, with RAPL reporting in Joules and the wall power meter typically using kilowatt-hours (1 kWh = 3.6 x 10^6 Watt⋅sec). Measuring short-running applications, which typically consume only a few Watt⋅sec, using a commercial wall meter is not practical since its scale is higher. Most commercial wall meters are more suitable for applications that consume at least 1 Watt⋅hour of energy (i.e., long running applications).
- the wall power meter provides measurements for the overall system power consumption, encompassing all components, whereas RAPL focuses specifically on individual domains (e.g., CPU, GPU, DRAM) within the system.

## Measurement Considerations

Reduced RAPL accuracy may be expected when the processor is not running heavy workloads and is in an idle state.

RAPL encompasses all components and applications running on the same host machine. It does not provide a breakdown of power consumption per individual application or component. Therefore, it is crucial to establish a **baseline measurement** of the system's power consumption during idle or minimal background processes.

Excessive heat can impact both the overall power consumption and performance of a system, indirectly affecting RAPL measurements. For this reason, disabling both:
- **turbo-boost** and
- **hyper-threading**

can effectively reduce CPU heat and improve the consistency of RAPL measurements.
It is also important to account for any external factors that may influence power consumption, such as variations in ambient temperature or fluctuations in power supply. These factors can introduce additional variability to RAPL measurements and should be taken into consideration during data analysis and interpretation

Measuring energy consumption for smaller tasks (such as **micro-benchmarking**) that complete quickly can be challenging, as the overall results are often dominated by the JVM footprint rather than the specific code being tested. This challenge can be partially addressed by employing iteration loops around the code snapshots being measured.

## Unit of Measurement

The command pattern used to start the JVM application that also reports at the end the energy stats relies on `perf` (available only on Linux):

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
While the watt-second is equivalent to the Joule in both units and meaning (e.g., 1 W⋅s = 1 J), I favor using the term "watt-second" instead of "Joule", which is, in general, easier to understand (and correlate with real-life examples) when speaking about the energy consumption.

**Note:** In general, the overhead introduced by `perf` is relatively low, especially when using hardware performance counters. The impact on system performance is typically considered acceptable for most profiling and performance analysis tasks.

## Hardware and Software Components

All the tests were launched on a machine having below configuration:
- CPU: Intel i7-8550U Kaby Lake R
- Memory: 32GB DDR4 2400 MHz
- OS: Ubuntu 22.04.2 LTS / 5.19.0-46-generic

The load testing tool used is [Hyperfoil](https://hyperfoil.io), a distributed benchmark framework oriented towards microservices that avoids the coordinated-omission fallacy.

The specific model of the wall power meter used is the [Ketotek KTEM02-1](https://www.amazon.de/-/en/dp/B0B2953JM5).

### Application Categories

Multiple application categories were included in these measurements:

- Off-the-shelf applications, such as:
  - [Spring PetClinic](https://github.com/spring-projects/spring-petclinic) Application
  - [Quarkus Hibernate ORM Panache](https://github.com/quarkusio/quarkus-quickstarts/tree/main/hibernate-orm-panache-quickstart)
  - [Renaissance](https://github.com/renaissance-benchmarks/renaissance) Benchmark Suite
- Custom-made Java applications relying on specific (but extremly common) code patterns, such as:
  - Logging patterns
  - Memory access patterns
  - Throwing exception patterns
  - (Sorting) algorithms complexities
  - Virtual calls

In addition to these categories, a baseline measurement of the system's power consumption while it is idle or running minimal background processes is provided. This establishes a reference point.

### JVM Coverage

The list of included JMVs is:

JVM distribution                                                                                                    | JDK version |Architecture
--------------------------------------------------------------------------------------------------------------------|-------------|---------------
[OpenJDK HotSpot VM](https://projects.eclipse.org/projects/adoptium.temurin/downloads)                              | 17.0.7      |x86_64
[GraalVM CE 23](https://www.graalvm.org/downloads)                                                                  | 17.0.7      |x86_64
[Oracle GraalVM 23](https://www.graalvm.org/downloads)                                                              | 17.0.7      |x86_64
[Graal Native Image (shipped with Oracle GraalVM 23)](https://www.graalvm.org/22.0/reference-manual/native-image/)  | 17.0.7      |x86_64
[Azul Prime VM](https://www.azul.com/products/prime)                                                                | 17.0.7      |x86_64
[Eclipse OpenJ9 VM](https://www.eclipse.org/openj9)                                                                 | 17.0.6      |x86_64

For each JVM, the only specific tuning parameters were the initial heap size, typically set to 1m (e.g., -Xms1m), and the maximum heap size, which varies depending on the application category. However, within the same category of tests, the heap tuning flags remained the same.

In the case of Graal Native Image, no specific compilation parameters were used (i.e., no Profile-Guided Optimizations, no G1 GC). An example of such a compilation log can be read [here](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/spring-petclinic/results/linux/x86_64/jdk-17/logs/native-image-build-1.log).

# Applications Runtime Execution Results

This section presents the measurement results obtained during the execution of each application category.

For each application category, the **end-to-end energy consumption** is measured. This measurement includes the energy consumed from the moment an application starts until it finishes its task, taking into account factors such as the initialization of the JVM, the application warmup period, etc. The main rationale behind this approach is to reflect the total energy consumption, including all phases, rather than just the ideal state of each JVM.

Even though at first glance this approach may seem to favor short-running applications that are Ahead-of-Time compiled, running them for a longer period of time enables a more realistic evaluation.

Therefore, the evaluated applications vary significantly in terms of total execution time that is being measured, ranging from a few minutes to several hours.

Within each category, multiple measurements were taken, and the baseline was subtracted from each of them. The results were then aggregated using the arithmetic mean (average), and a margin of error was calculated based on a [confidence](https://en.wikipedia.org/wiki/Confidence_interval) interval for each group. This error score is depicted in each bar plot.

To enable a high-level comparison of overall energy consumption scores across all the categories and JVMs, the normalized [geometric mean](https://en.wikipedia.org/wiki/Geometric_mean) was generated. This serves as an informative metric for assessing relative energy consumption.

*Note: All subsequent plots from this category represent the mean energy consumption based on the RAPL stats after subtracting the baseline measurements (i.e., idle system power consumption), including the 90% confidence level error.*

## Off-the-Shelf Applications

In the case of off-the-shelf web-based applications such as Spring and Quarkus, minimal configurations were primarily performed to enhance the database connection and application server's thread pools in order to handle increased loads.

### Spring PetClinic Application

This experiment assesses the energy consumption of the [Spring PetClinic](https://github.com/spring-projects/spring-petclinic) application while running different JVMs (utilizing both Just-in-Time and Ahead-of-Time compilation).

It involves running the Spring PetClinic with Spring Boot 3.0.6 and Hibernate ORM core version 6.1.7.Final for approximately 900 seconds, corresponding to real-world wall clock time. During this time, a load test comprising four independent phases was triggered, as described below. Each phase runs concurrently and targets different endpoints of the application:
1. The endpoints returning static data (e.g., get home page, find owners page, vets page, petclinic.css, bootstrap.bundle.min.js, font-awesome.min.css) were hit at a constant rate of 12 reqs/sec for 780 seconds.
2. The endpoint for searching owners by their last name using a wildcard of 1, 2, or 3 characters experienced an increased load ranging from 1 to 14 reqs/sec over 780 seconds.
3. The endpoints for creating/reading/editing owners and creating/reading pets encountered an increased load ranging from 1 to 12 reqs/sec over 780 seconds.
4. The endpoints for creating/reading pet visits experienced an increased load ranging from 1 to 10 reqs/sec over 780 seconds.

The number of requests per second was calibrated to match my hardware machine and effectively utilize the CPU's resources. In total, there were around 110,000 requests sent to the application, spread across all endpoints. Please refer to the Hyperfoil reports in the additional resources to see the distribution of these requests and other further details.

[![SpringPetClinic.svg](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/spring-petclinic/results/linux/x86_64/jdk-17/plot/run-energy.svg?raw=true)](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/spring-petclinic/results/linux/x86_64/jdk-17/plot/run-energy.svg?raw=true)

Additional resources:
- Hyperfoil [load test](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/spring-petclinic/test-plan.hf.yaml) plan
- Hyperfoil [reports](https://github.com/ionutbalosin/jvm-energy-consumption/tree/main/spring-petclinic/results/linux/x86_64/jdk-17/hreports)

### Quarkus Hibernate ORM Panache

This experiment assesses the energy consumption of the [Quarkus Hibernate ORM Panache](https://github.com/quarkusio/quarkus-quickstarts/tree/main/hibernate-orm-panache-quickstart) application while running different JVMs (utilizing both Just-in-Time and Ahead-of-Time compilation). This is a simple create, read, update and delete (CRUD) web-based application.

It involves running the Quarkus Hibernate ORM Panache sample application with Quarkus 3.0.3 and Hibernate ORM core version 6.2.1.Final for approximately 900 seconds, corresponding to real-world wall clock time. During this time, a load test comprising two independent phases was triggered, as described below. Each phase runs concurrently and targets different endpoints of the application:
1. The endpoint returning static data (e.g., get home page) was hit at a constant rate of 64 reqs/sec for 780 seconds.
3. The endpoints for creating/reading/updating/deleting fruits encountered an increased load ranging from 1 to 312 reqs/sec over 780 seconds.

The number of requests per second was calibrated to match my hardware machine and effectively utilize the CPU's resources. In total, there were around 650,000 requests sent to the application, spread across all endpoints. Please refer to the Hyperfoil reports in the additional resources to see the distribution of these requests and other further details.

[![QuarkusHibernateORMPanache.svg](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/quarkus-hibernate-orm-panache-quickstart/results/linux/x86_64/jdk-17/plot/run-energy.svg?raw=true)](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/quarkus-hibernate-orm-panache-quickstart/results/linux/x86_64/jdk-17/plot/run-energy.svg?raw=true)

Additional resources:
- Hyperfoil [load test](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/quarkus-hibernate-orm-panache-quickstart/test-plan.hf.yaml) plan
- Hyperfoil [reports](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/quarkus-hibernate-orm-panache-quickstart/results/linux/x86_64/jdk-17/hreports)

### Renaissance Benchmark Suite

This experiment assesses the energy consumption while running the [Renaissance](https://github.com/renaissance-benchmarks/renaissance) benchmark suite with different JVMs (using Just-in-Time compilation). 
The Renaissance suite comprises various JVM workloads grouped into categories such as Big Data, machine learning, and functional programming. 

The Renaissance version used was `renaissance-gpl-0.14.2.jar`. The categories included in these measurements are 
- Concurrency
- Functional
- Scala
- Web
 
Each category ran with 100 repetitions. The execution of these benchmarks takes from tens of minutes to a few hours, depending on the benchmark category.

[![RenaissanceConcurrency.svg](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/renaissance/results/linux/x86_64/jdk-17/concurrency/plot/run-energy.svg?raw=true)](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/renaissance/results/linux/x86_64/jdk-17/concurrency/plot/run-energy.svg?raw=true)

Additional resources: Renaissance [output results](https://github.com/ionutbalosin/jvm-energy-consumption/tree/main/renaissance/results/linux/x86_64/jdk-17/concurrency/reports)

[![RenaissanceFunctional.svg](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/renaissance/results/linux/x86_64/jdk-17/functional/plot/run-energy.svg?raw=true)](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/renaissance/results/linux/x86_64/jdk-17/functional/plot/run-energy.svg?raw=true)

Additional resources: Renaissance [output results](https://github.com/ionutbalosin/jvm-energy-consumption/tree/main/renaissance/results/linux/x86_64/jdk-17/functional/reports)

[![RenaissanceScala.svg](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/renaissance/results/linux/x86_64/jdk-17/scala/plot/run-energy.svg?raw=true)](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/renaissance/results/linux/x86_64/jdk-17/scala/plot/run-energy.svg?raw=true)

Additional resources: Renaissance [output results](https://github.com/ionutbalosin/jvm-energy-consumption/tree/main/renaissance/results/linux/x86_64/jdk-17/scala/reports)

[![RenaissanceWeb.svg](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/renaissance/results/linux/x86_64/jdk-17/web/plot/run-energy.svg?raw=true)](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/renaissance/results/linux/x86_64/jdk-17/web/plot/run-energy.svg?raw=true)

Additional resources: Renaissance [output results](https://github.com/ionutbalosin/jvm-energy-consumption/tree/main/renaissance/results/linux/x86_64/jdk-17/web/reports)

## Custom-Made Java Applications

In addition to the off-the-shelf applications, a collection of custom-made Java programs employing various coding paradigms was developed. These programs encompass the most common paradigms encountered in the majority of commercial Java applications. The execution of each program generally takes only a few minutes (i.e., relatively short running applications).

### Memory Access Patterns

This program aims to analyze the relationship between memory access patterns and energy consumption under different JVMs (utilizing both Just-in-Time and Ahead-of-Time compilation).

There are three primary memory access patterns:
- **Temporal**: memory that has been recently accessed is likely to be accessed again in the near future.
- **Spatial**: adjacent memory locations are likely to be accessed in close succession.
- **Striding**: memory access follows a predictable pattern, typically with a fixed interval between accesses.

The program creates a large array of longs, occupying approximately 4 GB of RAM memory. Then, during 10 consecutive iterations, the array elements are accesses based on one of the described patterns. After each iteration, the validity of the tests is checked.

Source code: [MemoryAccessPatterns.java](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/src/main/java/com/ionutbalosin/jvm/energy/consumption/MemoryAccessPatterns.java)

[![MemoryAccessPatterns.svg](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/results/linux/x86_64/jdk-17/MemoryAccessPatterns/plot/run-energy.svg?raw=true)](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/results/linux/x86_64/jdk-17/MemoryAccessPatterns/plot/run-energy.svg?raw=true)

The pattern of memory accesses and the co-location or non-co-location of memory significantly influence energy consumption. In general, by performing work on co-located data in chunks and following predictable memory access patterns, our algorithms can achieve significant speed improvements and reduce the energy consumption.

When we need to perform chunks of work on co-located data, arrays are widely recognized as cache-friendly due to their contiguous memory layout. Accessing array elements sequentially promotes good data locality, minimizing cache misses, enhancing cache utilization, and reducing energy consumption. 
In addition to arrays, we can utilize hash tables with open addressing and linear probing instead of bucket and chain hash tables. Similarly, we can store an array of multiple items in each node instead of employing linked lists or trees with individual items in each node.

### Logging Patterns

When it comes to logging, performance is one of the major concerns. The manner in which we log and the volume of logs can significantly impact the performance of our applications. This is due to the associated costs of heap allocations and the additional work performed by the garbage collector to clean up the heap. In addition to allocations, there are also expenses related to I/O operations when writing and flushing data to disk. All of these factors contribute to increased utilization of hardware resources (e.g., CPU and memory), resulting in higher energy consumption, which is reflected in our monthly bills.

The program measures various logging patterns using human-readable strings, which is often the most common use case in business applications. It consists of a total of 1,000,000 iterations, and within each iteration, the logging framework (e.g., `java.util.logging.Logger`) is invoked to log a line. It is crucial to note that none of these logs are physically written to disk; instead, they are written to the Null OutputStream. This approach is advantageous since the RAPL stats cannot capture any I/O-related activity.

Source code: [LoggingPatterns.java](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/src/main/java/com/ionutbalosin/jvm/energy/consumption/LoggingPatterns.java)

[![LoggingPatterns.svg](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/results/linux/x86_64/jdk-17/LoggingPatterns/plot/run-energy.svg?raw=true)](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/results/linux/x86_64/jdk-17/LoggingPatterns/plot/run-energy.svg?raw=true)

Certain logging patterns are more efficient than others. Based on these tests, examples of such patterns include `garded_unparameterized`, `lambda_local`, `lambda_heap`, `ungarded_unparameterized`, etc. It is worth noting that the energy consumption can vary significantly across different JVMs when logging the same data.

Reducing the number of logs to only essential ones, particularly those related to unrecoverable or unexpected scenarios, is a common recommendation that applies well to all business applications. Additionally, employing strategies such as using asynchronous appenders, binary logging, and writing to RAMFS or TEMPFS can further enhance efficiency.

### Throwing Exception Patterns

Similar to logging, the creation, throwing, and handling of exceptions introduce additional runtime overhead, impacting both the performance and energy consumption of software applications.

This program measures different exception throwing patterns. It involves a total of 100,000 iterations, and in each iteration, a different type of exception is thrown when the execution stack reaches a specific depth (in this case, 1024). It is worth noting that the depth of the call stack can also impact performance, and the time spent on filling in the stack trace (abbreviated `first`) dominates the associated costs.

Source code: [ThrowExceptionPatterns.java](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/src/main/java/com/ionutbalosin/jvm/energy/consumption/ThrowExceptionPatterns.java)

[![ThrowExceptionPatterns.svg](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/results/linux/x86_64/jdk-17/ThrowExceptionPatterns/plot/run-energy.svg?raw=true)](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/results/linux/x86_64/jdk-17/ThrowExceptionPatterns/plot/run-energy.svg?raw=true)

Creating constant exceptions and throwing them only when necessary is a good approach to mitigate the negative impact  on energy consumption. 

If constant exceptions do not meet the requirements, another option is to override the 'fillInStackTrace' method each time a new exception is thrown. 

If none of these alternatives are viable, at the very least, aim to minimize the number of exceptions in the application's source code.
It is important to consider that the cost increases with the actual stack depth at which the exception is created.

### Sorting Algorithms Complexities

This program utilizes various sorting algorithms with different complexities, ranging from quadratic to linear, to sort an array of 1,000,000 integers. The initial array is deliberately sorted in reverse order, creating a worst-case scenario for the sorting algorithms.

Source code: [SortingAlgorithms.java](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/src/main/java/com/ionutbalosin/jvm/energy/consumption/SortingAlgorithms.java)

[![SortingAlgorithms.svg](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/results/linux/x86_64/jdk-17/SortingAlgorithms/plot/run-energy.svg?raw=true)](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/results/linux/x86_64/jdk-17/SortingAlgorithms/plot/run-energy.svg?raw=true)

The `bubble_sort` algorithm, with its time complexity of O(n^2), typically consumes a significant amount of energy compared to other sorting algorithms when achieving the same result of providing a sorted array.

Even though `quick_sort` and `radix_sort` have different complexities, with quick sort having O(n log n) and radix sort having O(nk), they tend to consume similar amounts of energy when executed on the same JVM platform.

As we have seen, while algorithm complexities can impact energy consumption, the relationship is not always straightforward. 
In theory, algorithms with higher time or space complexities would generally require more computational effort to execute, leading to increased energy consumption. 
However, when running these algorithms on hardware, there are a few additional factors to consider. 
- **Memory access patterns**: Algorithms with poor memory access patterns, such as excessive random or cache-unfriendly accesses, can increase energy consumption.
- **The underlying hardware**: The characteristics and efficiency of the hardware on which the algorithm is executed can also affect energy consumption.

This is the reason why different algorithms with different time complexities could consume the same amount of energy. Even algorithms within the same class of complexity could end up consuming different amounts of energy. This can occur when an algorithm uses data structures that are poorly located in memory and not cache-friendly.

### Virtual Calls

The program evaluates the energy consumption of virtual calls using two different scenarios:
- one with 2 target implementations (also known as bimorphic).
- and another with 24 different target implementations (also known as megamorphic) . 

**Note:** Bimorphic call sites are more commonly encountered, while having 24 target implementations for the same call site is quite unusual.

An array of 9,600 elements of a base abstract class is initialized. Within 300,000 iterations, the program traverses the array and invokes the method on the base abstract object class for each array element.

Source code: [VirtualCalls.java](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/src/main/java/com/ionutbalosin/jvm/energy/consumption/VirtualCalls.java)

[![VirtualCalls.svg](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/results/linux/x86_64/jdk-17/VirtualCalls/plot/run-energy.svg?raw=true)](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/results/linux/x86_64/jdk-17/VirtualCalls/plot/run-energy.svg?raw=true)

In the context of modern hardware, for most business applications, virtual calls are generally not a major concern unless there is a specific need. As observed in the `bimorphic` case (for Native Image and Eclipse Open J9), it is possible that the compiler may have failed to optimize for the best-case scenario. However, in my opinion, the overall overhead of virtual calls is unlikely to be significant enough to justify avoiding them or caring too much about them.

## Runtime Geometric Mean

This section describes the normalized energy geometric mean for all application categories during runtime execution. It is purely informative and provides a high-level understanding of the overall energy consumption scores across all JVMs.

The Renaissance benchmark suite has been excluded from this report because it was not (explicitly) launched on the Native Image, which would result in an unfair comparison.

No. | JVM  distribution                                   | Architecture | Normalized Energy Geometric Mean | Phase 
----|-----------------------------------------------------|--------------|----------------------------------|--------
1   | Graal Native Image (shipped with Oracle GraalVM 23) | x86_64       | 0.373                            | runtime
2   | OpenJDK HotSpot VM                                  | x86_64       | 1.000                            | runtime
3   | Oracle GraalVM 23                                   | x86_64       | 1.026                            | runtime
4   | GraalVM CE 23                                       | x86_64       | 1.038                            | runtime
5   | Azul Prime VM                                       | x86_64       | 1.643                            | runtime
6   | Eclipse OpenJ9 VM                                   | x86_64       | 1.795                            | runtime

Based on the central tendency of the data, the first in the row can be considered the most eco-friendly JVM, while the last in the row consumes the most energy.

# Applications Build Time Execution Results

This section presents the measurement results obtained during the execution of the build process for each application category, using both Just-in-Time and Ahead-of-Time compilation.

Since they all exhibit a consistent trend in terms of energy consumption across every JVM, I have included only three of them in this section, representing each distinct application category.

*Note: All subsequent plots from this category represent the mean energy consumption based on the RAPL stats after subtracting the baseline measurements (i.e., idle system power consumption), including the 90% confidence level error.*

[![SpringPetClinic.svg](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/spring-petclinic/results/linux/x86_64/jdk-17/plot/build-energy.svg?raw=true)](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/spring-petclinic/results/linux/x86_64/jdk-17/plot/build-energy.svg?raw=true)

[![QuarkusHibernateORMPanache.svg](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/quarkus-hibernate-orm-panache-quickstart/results/linux/x86_64/jdk-17/plot/build-energy.svg?raw=true)](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/quarkus-hibernate-orm-panache-quickstart/results/linux/x86_64/jdk-17/plot/build-energy.svg?raw=true)

[![MemoryAccessPatterns.svg](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/results/linux/x86_64/jdk-17/MemoryAccessPatterns/plot/build-energy.svg?raw=true)](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/results/linux/x86_64/jdk-17/MemoryAccessPatterns/plot/build-energy.svg?raw=true)

**Note:** Once the build with native image is done, the resulting binary can be executed multiple times without the need to recompile it (i.e., the compilation cost is paid once), as long as it runs on the specific machine and architecture for which the compilation was performed.

Additional resources:

- [LoggingPatterns.svg](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/results/linux/x86_64/jdk-17/LoggingPatterns/plot/build-energy.svg?raw=true) build time energy
- [ThrowExceptionPatterns.svg](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/results/linux/x86_64/jdk-17/ThrowExceptionPatterns/plot/build-energy.svg?raw=true) build time energy
- [SortingAlgorithms.svg](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/results/linux/x86_64/jdk-17/SortingAlgorithms/plot/build-energy.svg?raw=true) build time energy
- [VirtualCalls.svg](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/results/linux/x86_64/jdk-17/VirtualCalls/plot/build-energy.svg?raw=true) build time energy

## Build Time Geometric Mean

This section describes the normalized energy geometric mean for all application categories during build time. It is purely informative and provides a high-level understanding of the overall energy consumption scores across all JVMs.

The Renaissance benchmark suite has been excluded from this report since it is provided as pre-compiled JAR.

No. | JVM distribution                                    | Architecture | Normalized Energy Geometric Mean   | Phase
----|-----------------------------------------------------|--------------|------------------------------------|-------
1   | Oracle GraalVM 23                                   | x86_64       | 0.814                              | build time
2   | GraalVM CE 23                                       | x86_64       | 0.829                              | build time
3   | OpenJDK HotSpot VM                                  | x86_64       | 1.000                              | build time
4   | Azul Prime VM                                       | x86_64       | 1.523                              | build time
5   | Eclipse OpenJ9 VM                                   | x86_64       | 2.192                              | build time
6   | Graal Native Image (shipped with Oracle GraalVM 23) | x86_64       | 26.910                             | build time

Based on the central tendency of the data, the first in the row can be considered the most eco-friendly JVM, while the last in the row consumes the most energy.

# How energy consumption correlates with performance

There is no direct relationship between energy consumption and performance. In general, energy consumption and performance are trade-offs within a system. While they often support each other, there can be cases where they are not aligned.

In regard to these  empirical studies, I can provide two examples that support my statement.

In the **first example**, higher energy consumption was observed alongside shorter response times, indicating a trade-off between being less eco-friendly but more performant.

*Note: All subsequent plots from this category represent the mean elapsed time versus the mean energy consumption based on the RAPL stats subtracting the baseline measurements (i.e., idle system power consumption), with error bars in two dimensions, including the 90% confidence level.*

[![LoggingPatterns-lambda_local.svg](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/results/linux/x86_64/jdk-17/LoggingPatterns/plot/run-energy-vs-time-lambda_local.svg?raw=true)](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/java-samples/results/linux/x86_64/jdk-17/LoggingPatterns/plot/run-energy-vs-time-lambda_local.svg?raw=true)

Based on this plot, it is noticeable that:
- Oracle GraalVM consumes more energy compared to OpenJDK HotSpot VM but completes tasks in less time.
- Azul Prime VM consumes more energy compared to GraalVM CE but also achieves faster task completion.

In the **second example**, lower energy consumption was observed, but it resulted in higher response times, indicating a trade-off between being more eco-friendly but less performant.

[![RenaissanceFunctional_energy-vs-time.svg](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/renaissance/results/linux/x86_64/jdk-17/functional/plot/run-energy-vs-time.svg?raw=true)](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/renaissance/results/linux/x86_64/jdk-17/functional/plot/run-energy-vs-time.svg?raw=true)

Based on this plot, it can be observed that Oracle GraalVM consumes slightly less energy than Azul Prime VM, but it takes more time to complete the task. However, it is important to note that Azul Prime VM stands out as the fastest option in this plot.

Let's consider now an analogy from the car industry: Is the most powerful car the most eco-friendly one? Of course not. On the contrary, a very powerful car with a larger engine tends to consume more fuel, potentially leading to more pollution. While the software realm may not directly mirror the dynamics of the car industry, this analogy serves to emphasize the difference between performance and energy consumption.

# From energy consumption to carbon emissions

Energy consumption and carbon emissions are closely correlated. To convert energy consumption from `Watt⋅sec` to `CO₂` emissions, we would first need to know the energy source (e.g., coal, natural gas, renewable energy) and its associated carbon emissions factor. Next, we multiply the energy consumption by the carbon emissions factor specific to our region (or the region of our data center) for the given energy source.

Let's consider our use case. The table below presents a summary of the total CO₂ emissions for each JVM, calculated based on the energy consumption reported by RAPL for the package and DRAM domains during applications runtime execution time. 

The Renaissance benchmark suite has been excluded from this report because it was not (explicitly) launched on the Native Image, which would result in an unfair comparison.

No. | JVM distribution                                      | Total Energy (Watt⋅sec) | CO₂ Emission Factor (gCO₂eq/kWh) | CO₂ Emissions (gCO₂)
----|-----------------------------------------------------|-------------------------|----------------------------------|-----------------------
1   | OpenJDK HotSpot VM                                  | 17,090.269              | 137                              |  0.650                      
2   | Graal Native Image (shipped with Oracle GraalVM 23) | 18,176.977              | 137                              |  0.692                     
3   | Oracle GraalVM 23                                   | 21,521.325              | 137                              |  0.819                  
4   | GraalVM CE 23                                       | 22,081.674              | 137                              |  0.840                     
5   | Azul Prime VM                                       | 27,163.172              | 137                              |  1.034                       
6   | Eclipse OpenJ9 VM                                   | 40,975.966              | 137                              |  1.559

Based on the total energy consumption, the JVM in the first row consumes less energy overall, while the JVM in the last row emits the highest amount of carbon dioxide.

**Legend:** 
- `CO₂` - carbon dioxide.
- `gCO₂eq/kWh` - grams of carbon dioxide equivalent per kWh.
- `gCO₂` - grams of carbon dioxide.
- `137` - is the [current carbon emission factor](https://github.com/ionutbalosin/jvm-energy-consumption/blob/main/docs/carbon-emission-factor-17_07_2023-austria.png) for Austria as of July 17, 2023, 1:00 PM, as reported by the [Electricity Maps](https://app.electricitymaps.com/zone/AT) website.

We can observe that when comparing the normalized geometric mean to the total energy consumption, the order of JVMs differs slightly (e.g., OpenJDK HotSpot VM consumes less energy overall compared to Graal Native Image). This discrepancy arises because the sum and geometric mean employ different mathematical operations, emphasizing distinct aspects of the data:
- The geometric mean is less affected by extreme values and offers a balanced representation of the central tendency of the data.
- In contrast, total energy consumption represents the accumulation of all measurements and directly correlates to our monthly bills.

# Conclusions

This article presents an empirical investigation into the variations in energy consumption among key JVM platforms on the x86_64 Intel chipset. The study explores the differences observed when running off-the-shelf web-based applications as well as common code patterns such as logging, memory accesses, exception throwing, algorithms with different time complexities, etc.

The selected JVM implementations exhibit varying levels of energy efficiency depending on the software and workloads tested, often displaying significant differences.

At the cost of increased compilation expenses and excluding the Renaissance benchmark suite (due to its lack of support for Ahead-of-Time compilation), GraalVM Native Image showcased the highest energy efficiency overall for the runtime execution.

OpenJDK HotSpot VM, Oracle GraalVM, and GraalVM CE exhibited similar efficiency in the majority of tests, with marginal differences.

Azul Prime VM's energy consumption varied depending on the test case, but generally, it consumed more energy than other HotSpot-based JVMs.

Eclipse OpenJ9 VM exhibited comparatively lower energy efficiency.

When it comes to software development, to write more eco-friendly code (i.e., code with reduced power consumption), programmers can employ various techniques covered in this report (but not only). These techniques include using cache-friendly data structures, avoiding inefficient algorithms, limiting the number of logged lines and thrown exceptions, minimizing object allocations, defining the scope of allocated objects as close as possible to their usage, etc.

This study was conducted using generally available and common features across the selected JVMs, with little to no tuning (i.e., only adjusting the initial and maximum heap size). However, it is important to note that there are specific JVM features available (to improve start-up response times, reducing memory footprint, and thus reducing energy consumption) that might change the picture in a real-world scenario. Examples of such features include Eclipse OpenJ9's [shared class cache (SCC)](https://eclipse.dev/openj9/docs/shrc), Azul Prime VM's [ReadyNow!](https://www.azul.com/products/components/readynow), or the novel technology [CRaC](https://wiki.openjdk.org/display/crac) introduced in the OpenJDK.

Therefore, the report should not be considered as the final determination of the most energy-efficient JVM distribution. Instead, it serves as an initial exploration, providing an approach to quantify energy consumption in real-world application scenarios.

# Future Work

An extension of this study would involve incorporating other architectures, such as arm64, and optionally exploring additional off-the-shelf applications or other code patterns.

It might also be interesting to assess the energy consumption across multiple web-based frameworks like Quarkus, Spring, Micronaut, etc. However, at the current stage, I have not found a proper way to compare them.

If you have any suggestions or are interested in contributing to this project, please feel free to reach out or open a pull request on [GitHub](https://github.com/ionutbalosin/jvm-energy-consumption). 

Your contributions are welcome and appreciated.

**Looking forward to contributing to a more eco-friendly world!**

# Acknowledgements

I am very thankful to [Gerrit Grunwald](https://twitter.com/hansolo_) and [Jiří Holuša](https://linkedin.com/in/jiří-holuša-16987874) from [Azul Systems](https://www.azul.com/), to [GraalVM](https://www.graalvm.org) team members and others for their reviews, and helpful suggestions.

# References

1. Tom Strempel. Master’s Thesis [Measuring the Energy Consumption of Software written in C on x86-64 Processors](https://ul.qucosa.de/api/qucosa%3A77194/attachment/ATT-0)

2. Spencer Desrochers, Chad Paradis, and Vincent M. Weaver. “A validation of DRAM RAPL power measurements”. In: ACM International Conference Proceed- ing Series 03-06-October-2016 (2016). DOI: [10.1145/2989081.2989088.](https://doi.org/10.1145/2989081.2989088)

3. Zhang Huazhe and Hoffman H. _“A quantitative evaluation of the RAPL power control system”_. In: _Feedback Computing_ (2015).

4. Kashif Nizam Khan et al. “RAPL in action: Experiences in using RAPL for power measurements”. In: ACM Transactions on Modeling and Performance Evaluation of Computing Systems 3 (2 2018). ISSN: 23763647. DOI: [10.1145/3177754](https://doi.org/10.1145/3177754).

5. Zakaria Ournani, Mohammed Chakib Belgaid, Romain Rouvoy, Pierre Rust, Joel Penhoat: [Evaluating the Impact of Java Virtual Machines on Energy Consumption](https://inria.hal.science/hal-03275286/document)

6. Ko Turk: [Green Software Engineering: Best Practices](https://www.adesso.nl/en/news/blog/green-software-engineering-best-practices.jsp)

7. Martin Thompson: [Memory Access Patterns Are Important](https://mechanical-sympathy.blogspot.com/2012/08/memory-access-patterns-are-important.html)

