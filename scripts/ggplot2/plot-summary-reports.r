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
jvm_color_palettes <- args[17:22]

jdk_arch <- paste("JDK-", jdk_version, " / ", arch, sep = "")

# Creates jvm color palette map
jvm_names_map <- setNames(as.list(jvm_names), jvm_identifiers)
jvm_color_palettes_map <- setNames(as.list(jvm_color_palettes), jvm_names)

# Special case: since there is a new test category for the "native-image" with "pgo_g1gc",
# add an additional category color palette to properly render the graphs."
jvm_color_palettes_map["Native Image (pgo_g1gc)"] <- "#FF007F"

# Apply column data changes on the initial data frame
processCsvColumns <- function(data) {
  # delete all spaces from all column values
  data <- as.data.frame(apply(data, 2, function(x) gsub("\\s+", "", x)))

  # rename "Total Energy (Watt⋅sec)" and "Throughput (Ops/sec)" columns
  if ("Total.Energy..Watt.sec." %in% colnames(data)) {
    colnames(data)[colnames(data) == "Total.Energy..Watt.sec."] <- "Score"
    data$ScoreUnit <- "Watt⋅sec"
  }
  if ("Throughput..Ops.sec." %in% colnames(data)) {
    colnames(data)[colnames(data) == "Throughput..Ops.sec."] <- "Score"
    data$ScoreUnit <- "Ops/sec"
  }

  # rename Run Identifier column
  colnames(data)[colnames(data) == "Run.Identifier"] <- "RunIdentifier"

  # if needed, convert from string to numeric the Score and Error columns. In addition, convert commas to dots
  data$Score <- as.numeric(gsub(",", ".", data$Score))

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

plotBarAndScatter <- function(data, output_folder, report_basename, report_type, plot_y_label, plot_title) {
  # 1. generate the bar plots (i.e., energy/performance plots)
  print(paste("Plotting bar of type", report_type, "for", plot_title, "...", sep = " "))

  plot <- generateBarPlot(data, "JvmIdentifier", "Legend", "", plot_y_label, plot_title, jdk_arch, jvm_color_palettes_map)
  saveBarPlot(data, plot, paste(output_folder, "plot", sep = "/"), paste(report_basename, report_type , sep = "-"))
}

plotEnergyReports <- function(output_folder, report_basename, plot_y_label_prefix, plot_title) {
  # Define the report types that might be generated
  report_types <- c("run", "build")

  # For each report type generate the corresponding plots
  for (report_type in report_types) {
    file_basename <- paste(report_basename, "-", report_type, ".csv", sep = "")
    data <- readCsvResultsFromFile(paste(output_folder, "summary-reports", file_basename, sep = "/"))

    if (!empty(data)) {
      data <- processCsvColumns(data)
      plot_y_label <- paste(plot_y_label_prefix, " (", data$ScoreUnit[1], ")", sep = "")
      plotBarAndScatter(data, output_folder, report_basename, report_type, plot_y_label, plot_title)
    } else {
      cat("Skipping", file_basename, "Found empty content ...\n")
    }
  }
}

# Generate plots for off the shelf applications
off_the_shelf_applications <- list(
  "Spring PetClinic" = "spring-petclinic",
  "Quarkus Hibernate ORM Panache Quickstart" = "quarkus-hibernate-orm-panache-quickstart"
)
for (application_name in names(off_the_shelf_applications)) {
  full_path <- file.path(base_path, "off-the-shelf-applications", off_the_shelf_applications[[application_name]], output_folder, sep = "/")
  plotEnergyReports(full_path, "energy-report", "Energy", application_name)
  plotEnergyReports(full_path, "raw-performance-report", "Throughput", application_name)
}

# Generate plots for java samples
java_samples <- list(
  "Logging Patterns" = "LoggingPatterns",
  "Memory Access Patterns" = "MemoryAccessPatterns",
  "Throw Exception Patterns" = "ThrowExceptionPatterns",
  "Sorting Algorithms" = "SortingAlgorithms",
  "Virtual Calls" = "VirtualCalls"
)
for (sample_name in names(java_samples)) {
  full_path <- paste(base_path, "java-samples", output_folder, java_samples[[sample_name]], sep = "/")
  plotEnergyReports(full_path, "energy-report", "Energy", sample_name)
  plotEnergyReports(full_path, "raw-performance-report", "Throughput", sample_name)
}