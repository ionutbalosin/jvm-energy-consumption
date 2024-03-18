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
# Special case: since there is a new test category for the "native-image" with "pgo",
# add an additional category color palette to properly render the graphs."
jvm_color_palette_map <- c(jvm_color_palette_map, "Native Image (pgo)" = "#882255")

plotReports <- function(output_folder, report_basename, plot_type, plot_title) {
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

plotSummaryReports <- function(output_folder, report_basename, plot_title) {
  # Define the report types that might be generated
  report_types <- c("run")

  # For each report type generate the corresponding plots
  for (report_type in report_types) {
    file_basename <- paste(report_basename, "-", report_type, ".csv", sep = "")
    data <- readCsvResultsFromFile(paste(output_folder, "summary-reports", file_basename, sep = "/"))

    if (!empty(data)) {
      data <- processCsvColumns(data)
      plotSummaryScatter(data, output_folder, report_basename, report_type, plot_title)
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
  plotReports(application_full_path, "energy-report", "bar-plot", off_the_shelf_application)
  plotReports(application_full_path, "performance-report", "bar-plot", off_the_shelf_application)
  plotReports(application_full_path, "raw-power", "scatter-plot", off_the_shelf_application)
}

# Generate plots for java samples
java_samples <- list(
  "Logging Patterns" = "LoggingPatterns",
  "Memory Access Patterns" = "MemoryAccessPatterns",
  "Throw Exception Patterns" = "ThrowExceptionPatterns",
  "Sorting Algorithms" = "SortingAlgorithms",
  "String Concatenation Patterns" = "StringConcatenationPatterns",
  "Virtual/Platform Thread Queue Throughput" = "VPThreadQueueThroughput",
  "Sorting Algorithms" = "SortingAlgorithms",
  "Virtual Calls" = "VirtualCalls"
)
for (java_sample in names(java_samples)) {
  sample_full_path <- paste(base_path, "java-samples", output_folder, java_samples[[java_sample]], sep = "/")
  plotReports(sample_full_path, "energy-report", "bar-plot", java_sample)
  plotReports(sample_full_path, "performance-report", "bar-plot", java_sample)
  # Note: Skip the scatter plotting for Java samples since it involves splitting them into individual plots (per category:type).
}

# Generate plots for summary results
application_full_path <- file.path(base_path, "summary-reporting", output_folder, sep = "/")
plotSummaryReports(application_full_path, "performance-energy-report", "Normalised Throughput vs. Normalised Energy")
