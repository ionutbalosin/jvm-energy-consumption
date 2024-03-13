#
# JVM Energy Consumption
#
# MIT License
#
# Copyright (c) 2023-2024 Ionut Balosin, Ko Turk
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

source("../scripts/ggplot2/plot-utils.r")

# retrieve command line arguments in a very specific order
args <- commandArgs(TRUE)
base_path <- args[1]
output_folder <- args[2]
jdk_version <- args[3]
arch <- args[4]
jvm_identifiers <- args[5:10]
jvm_names <- args[11:16]
jvm_color_palette <- args[17:22]

# Merge the JDK version with the arch
jdk_arch <- paste("JDK-", jdk_version, " / ", arch, sep = "")

# Creates jvm color palette map
jvm_color_palette_map <- setNames(jvm_color_palette, jvm_names)
# Special case: since there is a new test category for the "native-image" with "pgo_g1gc",
# add an additional category color palette to properly render the graphs."
jvm_color_palette_map <- c(jvm_color_palette_map, "Native Image (pgo_g1gc)" = "#882255")

print(jvm_color_palette_map)

# Apply column data changes on the initial data frame
processCsvColumns <- function(data) {
  # trim all spaces from all column values
  data <- as.data.frame(apply(data, 2, function(x) trimws(x)))

  # rename some columns
  colnames(data)[colnames(data) == "Score.Metric"] <- "ScoreMetric"
  colnames(data)[colnames(data) == "Run.Identifier"] <- "RunIdentifier"
  colnames(data)[colnames(data) == "Sample.Identifier"] <- "SampleIdentifier"

  # convert from string to numeric the Score and SampleIdentifier columns. In addition, convert commas to dots
  data$Score <- as.numeric(gsub(",", ".", data$Score))
  if (!is.null(data$SampleIdentifier)) {
    data$SampleIdentifier <- as.numeric(gsub(",", ".", data$SampleIdentifier))
  }

  # add a new JVM identifier column based on Category
  data$JvmIdentifier <- data$Category

  # Special case: update the JVM identifier for the "native-image" with "pgo_g1gc"
  data$JvmIdentifier[(data$Category == "native-image") & (data$RunIdentifier == "pgo_g1gc")] <- "native-image-pgo_g1gc"

  # update the JVM identifier values to be properly displayed in legend
  data$JvmIdentifier[data$JvmIdentifier == "openjdk-hotspot-vm"] <- "OpenJDK HotSpot VM"
  data$JvmIdentifier[data$JvmIdentifier == "graalvm-ce"] <- "GraalVM CE"
  data$JvmIdentifier[data$JvmIdentifier == "oracle-graalvm"] <- "Oracle GraalVM"
  data$JvmIdentifier[data$JvmIdentifier == "native-image"] <- "Native Image"
  data$JvmIdentifier[data$JvmIdentifier == "native-image-pgo_g1gc"] <- "Native Image (pgo_g1gc)"
  data$JvmIdentifier[data$JvmIdentifier == "azul-prime-vm"] <- "Azul Prime VM"
  data$JvmIdentifier[data$JvmIdentifier == "eclipse-openj9-vm"] <- "Eclipse OpenJ9 VM"

  # add a new Benchmark column
  if (is.null(data$Type)) {
    # if there are no test types (i.e., variations of the same benchmark application)
    # Special case: for the "native-image" with "pgo_g1gc", concatenate it, otherwise use the Category
    category_run_identifier <- paste(data$Category, " (", data$RunIdentifier, ")", sep = "")
    data$Benchmark <- ifelse(data$RunIdentifier == "pgo_g1gc", category_run_identifier, data$Category)
  } else {
    # if there are test types, use them as a benchmark name
    data$Benchmark <- data$Type
  }

  data
}

plotBar <- function(data, output_folder, report_basename, report_type, plot_y_label, plot_title) {
  print(paste("Plotting scatter for",  plot_title, "(", report_basename, ",", report_type, ") ...", sep = " "))
  plot <- generateBarPlot(data, "JvmIdentifier", "Legend", "", plot_y_label, plot_title, jdk_arch, jvm_color_palette_map)
  saveBarPlot(data, plot, paste(output_folder, "plot", sep = "/"), paste(report_basename, report_type , sep = "-"))
}

plotScatter <- function(data, output_folder, report_basename, report_type, plot_y_label, plot_title) {
  print(paste("Plotting scatter for",  plot_title, "(", report_basename, ",", report_type, ") ...", sep = " "))
  # Sort the data frame by 'JvmIdentifier' and then by 'SampleIdentifier' columns to appear chronologically
  data <- data[order(data$JvmIdentifier, data$SampleIdentifier), ]
  plot <- generateScatterPlot(data, "JvmIdentifier", "Legend", "Time", plot_y_label, plot_title, jdk_arch, jvm_color_palette_map)
  saveScatterPlot(data, plot, paste(output_folder, "plot", sep = "/"), paste(report_basename, report_type , sep = "-"))
}

plotEnergyReports <- function(output_folder, report_basename, plot_type, plot_title) {
  # Define the report types that might be generated
  report_types <- c("run", "build")

  # For each report type generate the corresponding plots
  for (report_type in report_types) {
    file_basename <- paste(report_basename, "-", report_type, ".csv", sep = "")
    data <- readCsvResultsFromFile(paste(output_folder, "summary-reports", file_basename, sep = "/"))

    if (!empty(data)) {
      data <- processCsvColumns(data)
      if (plot_type == "bar-plot") {
        plotBar(data, output_folder, report_basename, report_type, data$ScoreMetric[1], plot_title)
      } else
      if (plot_type == "scatter-plot") {
        plotScatter(data, output_folder, report_basename, report_type, data$ScoreMetric[1], plot_title)
      } else {
        cat("Unknown plot type ", plot_type, "\n")
      }
    } else {
      cat("Skipping", file_basename, "Found empty content ...\n")
    }
  }
}

# Generate plots for off-the-shelf applications
off_the_shelf_applications <- list(
  "Spring PetClinic" = "spring-petclinic",
  "Quarkus Hibernate ORM Panache Quickstart" = "quarkus-hibernate-orm-panache-quickstart"
)
for (off_the_shelf_application in names(off_the_shelf_applications)) {
  application_full_path <- file.path(base_path, "off-the-shelf-applications", off_the_shelf_applications[[off_the_shelf_application]], output_folder, sep = "/")
  plotEnergyReports(application_full_path, "energy-report", "bar-plot", off_the_shelf_application)
  plotEnergyReports(application_full_path, "performance-report", "bar-plot", off_the_shelf_application)
  plotEnergyReports(application_full_path, "raw-power", "scatter-plot", off_the_shelf_application)
}

# Generate plots for java samples
java_samples <- list(
  "Logging Patterns" = "LoggingPatterns",
  "Memory Access Patterns" = "MemoryAccessPatterns",
  "Throw Exception Patterns" = "ThrowExceptionPatterns",
  "Sorting Algorithms" = "SortingAlgorithms",
  "Virtual Calls" = "VirtualCalls"
)
for (java_sample in names(java_samples)) {
  sample_full_path <- paste(base_path, "java-samples", output_folder, java_samples[[java_sample]], sep = "/")
  plotEnergyReports(sample_full_path, "energy-report", "bar-plot", java_sample)
  plotEnergyReports(sample_full_path, "performance-report", "bar-plot", java_sample)
  # Note: Skip plotting the scatter for Java samples since it involves splitting them into individual plots (per category:type).
}