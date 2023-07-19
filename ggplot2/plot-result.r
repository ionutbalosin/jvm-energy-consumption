#
# JVM Energy Consumption
#
# MIT License
#
# Copyright (c) 2023 Ionut Balosin
# Copyright (c) 2023 Ko Turk
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.
#

source("./ggplot2/plot-utils.r")

# retrieve command line arguments in a very specific order
args <- commandArgs(TRUE)
base_path <- args[1]
output_folder <- args[2]

# Define the color palette (corresponding to each JVM) to be used in the final generated plot
# Note: use a color blindness palette (e.g., https://davidmathlogic.com/colorblind/)
full_color_palette <- c("OpenJDK HotSpot VM" = "#648FFF", "GraalVM CE" = "#FFB000", "GraalVM EE" = "#FE6100", "Native Image" = "#DC267F", "Azul Prime VM" = "#785EF0", "Eclipse OpenJ9 VM" = "#009E73")

# Apply column data changes on the initial data frame
processCsvColumns <- function(data) {
  # delete all spaces from all column values
  data <- as.data.frame(apply(data, 2, function(x) gsub("\\s+", "", x)))

  # rename columns
  colnames(data)[colnames(data) == "Test.Category"] <- "Category"
  colnames(data)[colnames(data) == "Test.Type"] <- "Type"
  colnames(data)[colnames(data) == "Energy.Mean..Watt.sec."] <- "EnergyScore"
  colnames(data)[colnames(data) == "Energy.Score.Error..90.0.."] <- "EnergyError"
  colnames(data)[colnames(data) == "Elapsed.Mean..sec."] <- "TimeScore"
  colnames(data)[colnames(data) == "Elapsed.Score.Error..90.0.."] <- "TimeError"

  # convert from string to numeric the Score and Error columns. In addition, convert commas to dots
  data$EnergyScore <- as.numeric(gsub(",", ".", data$EnergyScore))
  data$EnergyError <- as.numeric(gsub(",", ".", data$EnergyError))
  data$TimeScore <- as.numeric(gsub(",", ".", data$TimeScore))
  data$TimeError <- as.numeric(gsub(",", ".", data$TimeError))

  # add a new Unit column
  data$EnergyUnit <- "Watt⋅sec"
  data$TimeUnit <- "sec"

  # add a JVM identifier column
  data$JvmIdentifier <- data$Category
  data$JvmIdentifier[data$JvmIdentifier == "openjdk-hotspot-vm"] <- "OpenJDK HotSpot VM"
  data$JvmIdentifier[data$JvmIdentifier == "graalvm-ce"] <- "GraalVM CE"
  data$JvmIdentifier[data$JvmIdentifier == "graalvm-ee"] <- "GraalVM EE"
  data$JvmIdentifier[data$JvmIdentifier == "native-image"] <- "Native Image"
  data$JvmIdentifier[data$JvmIdentifier == "azul-prime-vm"] <- "Azul Prime VM"
  data$JvmIdentifier[data$JvmIdentifier == "eclipse-openj9-vm"] <- "Eclipse OpenJ9 VM"

  data
}

plotBarAndScatter <- function(data, output_folder, report_type, plot_title) {
  test_types <- c()
  if (is.null(data$Type)) {
    # if there are no test types (e.g., variations of the same benchmark application), add a new Type column the same as Category
    # Note: the Type column is used as an identifier for the X-axis in the final generated plot
    data$Type <- data$Category
  } else {
    # otherwise just select all unique test types
    # Note: this will be used for generating the individual scatter plots
    test_types <- unique(data$Type)
  }

  # 1. generate the bar plots (i.e., energy plots)
  print(paste("Plotting bar", plot_title, "...", sep = " "))
  plot <- generateBarPlot(data, "JvmIdentifier", "Legend", "", "Energy (Watt⋅sec)", plot_title, full_color_palette)
  saveBarPlot(data, plot, paste(output_folder, "plot", sep = "/"), paste(report_type, "energy", sep = "-"))

  # 2. generate the scatter plots (i.e., energy vs time plots), as follows:
  # - if there are no test types (e.g., variations of the same benchmark application), generate just one scatter plot
  # - if there are multiple test types, for each type generate a dedicated scatter plot
  if (length(test_types) == 0) {
    print(paste("Plotting scatter", plot_title, "...", sep = " "))
    plot <- generateScatterPlot(data, "JvmIdentifier", "Legend", "Energy (Watt⋅sec)", "Time (sec)", plot_title, full_color_palette)
    saveScatterPlot(data, plot, paste(output_folder, "plot", sep = "/"), paste(report_type, "energy-vs-time", sep = "-"))
  } else {
    for (test_type in test_types) {
      data_with_same_test_types <- data[data$Type == test_type, ]
      plot_title_and_test_type <- paste(plot_title, paste("(", test_type, ")", sep = ""), sep = " ")

      print(paste("Plotting scatter", plot_title_and_test_type, "...", sep = " "))
      plot <- generateScatterPlot(data_with_same_test_types, "JvmIdentifier", "Legend", "Energy (Watt⋅sec)", "Time (sec)", plot_title_and_test_type, full_color_palette)
      saveScatterPlot(data_with_same_test_types, plot, paste(output_folder, "plot", sep = "/"), paste(report_type, "energy-vs-time", test_type, sep = "-"))
    }
  }
}

plotEnergyReports <- function(output_folder, plot_title) {
  # Define the report types that could be generated
  report_types <- c("run", "build")

  # For each report type generate the corresponding plots
  for (report_type in report_types) {
    file_basename <- paste(report_type, "energy-reports.csv", sep = "-")
    data <- readCsvResults(paste(output_folder, "energy-consumption", file_basename, sep = "/"))

    if (!empty(data)) {
      data <- processCsvColumns(data)
      plotBarAndScatter(data, output_folder, report_type, plot_title)
    }
  }
}

# define all application paths for plotting
spring_petclinic_output_folder <- paste(base_path, "spring-petclinic", output_folder, sep = "/")
quarkus_hibernate_orm_panache_output_folder <- paste(base_path, "quarkus-hibernate-orm-panache-quickstart", output_folder, sep = "/")
renaissance_concurrency_output_folder <- paste(base_path, "renaissance", output_folder, "concurrency", sep = "/")
renaissance_functional_output_folder <- paste(base_path, "renaissance", output_folder, "functional", sep = "/")
renaissance_scala_output_folder <- paste(base_path, "renaissance", output_folder, "scala", sep = "/")
renaissance_web_output_folder <- paste(base_path, "renaissance", output_folder, "web", sep = "/")
logging_patterns_output_folder <- paste(base_path, "java-samples", output_folder, "LoggingPatterns", sep = "/")
memory_access_patterns_output_folder <- paste(base_path, "java-samples", output_folder, "MemoryAccessPatterns", sep = "/")
throw_exception_patterns_output_folder <- paste(base_path, "java-samples", output_folder, "ThrowExceptionPatterns", sep = "/")
sorting_algorithms_output_folder <- paste(base_path, "java-samples", output_folder, "SortingAlgorithms", sep = "/")
virtual_calls_output_folder <- paste(base_path, "java-samples", output_folder, "VirtualCalls", sep = "/")
summary_rapl_reports_output_folder <- paste(base_path, "rapl-reports", output_folder, "..", sep = "/")

plotEnergyReports(spring_petclinic_output_folder, "Spring PetClinic")
plotEnergyReports(quarkus_hibernate_orm_panache_output_folder, "Quarkus Hibernate ORM Panache Quickstart")
plotEnergyReports(renaissance_concurrency_output_folder, "Renaissance Concurrency")
plotEnergyReports(renaissance_functional_output_folder, "Renaissance Functional")
plotEnergyReports(renaissance_scala_output_folder, "Renaissance Scala")
plotEnergyReports(renaissance_web_output_folder, "Renaissance Web")
plotEnergyReports(logging_patterns_output_folder, "Logging Patterns")
plotEnergyReports(memory_access_patterns_output_folder, "Memory Access Patterns")
plotEnergyReports(throw_exception_patterns_output_folder, "Throw Exception Patterns")
plotEnergyReports(sorting_algorithms_output_folder, "Sorting Algorithms")
plotEnergyReports(virtual_calls_output_folder, "Virtual Calls")
