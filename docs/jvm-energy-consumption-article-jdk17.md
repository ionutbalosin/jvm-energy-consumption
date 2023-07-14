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

# Introduction

## The Importance of Power Consumption in Modern Computing

Power consumption is a crucial consideration in modern computing. Firstly, it directly impacts the energy efficiency of devices, contributing to reduced electricity costs and environmental sustainability. With the proliferation of technology and the increasing number of devices we use, minimizing power consumption helps conserve energy resources.
Power consumption plays a role in thermal management. 

High power consumption generates more heat, which can lead to increased temperatures within devices. Effective thermal management is vital to prevent overheating and maintain optimal performance and reliability.

Lastly, power consumption is a consideration in data centers and large-scale computing infrastructure. These facilities consume massive amounts of energy, and reducing power consumption can result in significant cost savings and a smaller environmental footprint.

Overall, managing power consumption is important in modern computing to promote energy efficiency, ensure thermal stability, and support sustainable practices.

## Motivation

Conducting power consumption experiments can provide valuable insights and benefits. Here are a few reasons that lead me to conduct such an experiment:
- **Curiosity, Innovation and Research**: These power consumption experiments were a fascinating field of exploration to me. By conducting these experiments, I hoped to discover techniques, approaches that help me to further minimize power consumption on real applications
- **Energy Efficiency**: By measuring power consumption, we can identify opportunities to optimize energy usage in our computing devices or systems. This knowledge can lead to more energy-efficient designs, reduced electricity costs, and a smaller environmental impact
- **Performance Optimization**: Power consumption experiments can help us understand how different software configurations, algorithms, or hardware choices affect power usage. By optimizing power consumption, we may also improve overall system performance, as power-efficient designs often lead to better thermal management and reduced bottlenecks
- **Sustainable Computing**: Power consumption experiments align with the growing emphasis on sustainability in technology. By investigating and mitigating power inefficiencies, we actively contribute to reducing energy waste and minimizing the carbon footprint associated with computing

## Objectives

Below is a list of several objectives I considered for my experiments:

- **Comparative Analysis**: Compare the power consumption of different types of applications (and code patterns) running on different Java Virtual Machines (JVM), to identify variations and determine which JVMs are more energy-efficient
- **Power Measurement Techniques**: An approach about how to run applications under different workloads and measure the overall power consumption 
- **Performance-Optimized Power Efficiency**: Investigate how power consumption correlates with system performance

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

It is worth mentioning that since RAPL reports only the energy consumption of a few domains (e.g., CPU, GPU, DRAM), but the system overall consumes much more energy for other components that are not included, as follows:
- any networking interface as Ethernet, Wi-Fi (Wireless Fidelity), Bluetooth, etc.
- any attached storage device (as hard disk drives, solid-state drives, and optical drives) relying on SATA (Serial AT Attachment), NVMe (Non-Volatile Memory Express), USB (Universal Serial Bus), Thunderbolt, SCSI (Small Computer System Interface), FireWire (IEEE 1394), Fibre Channel, etc.
- any display interface using HDMI (High-Definition Multimedia Interface), VGA (Video Graphics Array), DVI (Digital Visual Interface), Thunderbolt, DisplayPort, etc.
- the motherboard

In other words, for a typical JVM application, this means that any I/O operation that involves reading data from or writing data to any storage device but also any networking operation that uses I/O to send or receive data over a network are not captured.

#### RAPL Validity, and Accuracy

Proof of the measurement methods' validity, which depend on RAPL, is necessary. Therefore, below are some noteworthy RAPL-based studies.

Desrochers et al. [2] measured DRAM energy consumption while minimizing interference, comparing it with RAPL measurements. These findings validate the DRAM domain, utilizing diverse systems and benchmarks. Variances between physical and RAPL measurements are below 20% [2]. Recent processors, like Intel Haswell microarchitecture, exhibit improved precision compared to earlier generations.

Zhang et al. [3] set a power consumption limit using RAPL and evaluated its adherence. Out of 16 benchmarks, 14 had a 2% mean absolute percentage error (MAPE), while 2 had an error rate exceeding 5% [3]. The study also highlighted RAPL's improved accuracy in high energy consumption scenarios.

Khan et al. [4] compared RAPL to wall power measurements in Taito supercomputer, finding a strong 99% correlation. The estimation error (MAPE) was only 1.7%. Performance overhead of reading RAPL was <1% [4].

Based to these extensive studies, RAPL is considered to be a reliable and widely used tool for power consumption analysis on Intel-based systems.

In addition to that, it is worth noting that on the newer CPUs, including the Intel Haswell microarchitecture, the RAPL precision is better than in previous generations.

### Wall Power Meter

A wall power meter directly measures the power consumption at the socket, providing a holistic view of the energy consumed by the entire system, including the CPU, GPU, memory, storage, and other peripherals.

It provides real-world energy consumption data, which can be particularly useful when evaluating the energy efficiency of an application in practical scenarios.

By using a wall power meter alongside reported RAPL stats, we can obtain more accurate, reliable, and comprehensive measurements of the energy consumption of an application, helping us to make informed decisions regarding energy optimization and efficiency.

### RAPL vs. Wall Power Meter

There are a few significant differences between these two power measuring methods, that make them complementary rather than interchangeable:

- the sampling periods may differ between RAPL and the wall power meter.
- the measurement scales are also different, with RAPL reporting in Joules and the wall power meter typically using kilowatt-hours (1 kWh = 3.6 x 10^6 Watt⋅sec). Measuring short-running applications, which typically consume only a few Watt⋅sec, using a commercial wall meter is not practical since its scale is higher. Most commercial wall meters are more suitable for applications that consume at least 1 Watt⋅hour of energy (i.e., long running applications).
- the wall power meter provides measurements for the overall system power consumption, encompassing all components, whereas RAPL focuses specifically on individual domains (e.g., CPU, GPU, DRAM) within the system.

## Measurement Considerations

Reduced RAPL accuracy may be expected when the processor is not running heavy workloads and is in an idle state

RAPL measures the total power consumption of the entire system or package, encompassing all components and applications running on the same machine. It does not provide a breakdown of power consumption per individual application or component. Therefore, it is crucial to establish a **baseline measurement** of the system's power consumption during idle or minimal background processes

Excessive heat can impact both the overall power consumption and performance of a system, indirectly affecting RAPL measurements. For that reason, disabling both:
- **turbo-boost** mode and
- **hyper-threading**

can effectively reduce CPU heat and enhance the consistency of RAPL measurements.
It is also important to account for any external factors that may influence power consumption, such as variations in ambient temperature or fluctuations in power supply. These factors can introduce additional variability to RAPL measurements and should be taken into consideration during data analysis and interpretation

Measuring power consumption for smaller tasks (such as **micro-benchmarking**) that complete quickly can be challenging, as the overall results are often dominated by the JVM footprint rather than the specific code being tested. This challenge can be partially addressed by employing iteration loops around the code snapshots being measured.

## Unit of Measurement

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

## Hardware and Software Components

All the tests were launched on a machine having below configuration:
- CPU: Intel i7-8550U Kaby Lake R
- Memory: 32GB DDR4 2400 MHz
- OS: Ubuntu 22.04.2 LTS / 5.19.0-46-generic

The specific model of the wall power meter used is the [Ketotek KTEM02-1](https://www.amazon.de/-/en/dp/B0B2953JM5).

### Application Categories

Multiple application categories were included in these measurements:

- off-the-shelf applications, such as:
  - [Spring PetClinic](https://github.com/spring-projects/spring-petclinic) Application
  - [Quarkus Hibernate ORM Panache](https://github.com/quarkusio/quarkus-quickstarts/tree/main/hibernate-orm-panache-quickstart)
  - [Renaissance](https://github.com/renaissance-benchmarks/renaissance/releases) Benchmark Suite
- custom-made Java applications relying on specific code patterns, such as:
  - logging patterns
  - memory access patterns
  - throwing exception patterns
  - (sorting) algorithms complexities
  - virtual calls

In addition to these categories, a baseline measurement of the system's power consumption while it is idle or running minimal background processes is provided. This will help establish a reference point.

Multiple measurements were taken within each category. The results were aggregated using the arithmetic mean (average), and a margin of error was calculated based on a [confidence](https://en.wikipedia.org/wiki/Confidence_interval) interval for each group. This error score is depicted in each bar plot to provide additional information.

To enable a high-level comparison of overall power consumption scores across different JVMs, the normalized [geometric mean](https://en.wikipedia.org/wiki/Geometric_mean) was calculated across all categories. This serves as an informative metric for assessing relative power consumption.

### JVM Coverage

The list of included JMVs on arch x86_64 is:

No. | JVM distribution
----|--------------------
1   | [OpenJDK HotSpot VM](https://projects.eclipse.org/projects/adoptium.temurin/downloads)
2   | [GraalVM CE](https://www.graalvm.org/downloads)
3   | [GraalVM EE](https://www.graalvm.org/downloads)
4   | [Native-Image](https://www.graalvm.org/22.0/reference-manual/native-image/)
5   | [Azul Prime VM](https://www.azul.com/products/prime)
6   | [Eclipse OpenJ9 VM](https://www.eclipse.org/openj9) 

# Results

- Present your measured power consumption data in the form of tables or figures.
- Include plots that clearly visualize the power consumption trends across different programs and usage scenarios.
- Discuss any noteworthy observations or patterns you observed during the experiments.

# Analysis and Discussion

- Interpret the results obtained and explain their significance.
- Compare the power consumption of different programs and identify any variations or outliers.
- Analyze the factors that contribute to higher or lower power consumption in specific scenarios.
- Discuss the implications of your findings on energy efficiency and potential optimizations.

# Limitations and Future Work

- Acknowledge any limitations or constraints of your study, such as hardware limitations or external factors that could have influenced the results.
- Suggest areas for further research or potential improvements to the experimental setup.

# Conclusions

- Summarize the key findings of your study.
- Emphasize the practical implications and potential applications of your research.
- Encourage further investigation or exploration based on your findings.
