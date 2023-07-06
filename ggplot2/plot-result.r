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
spring_petclinic_output_folder <- args[1]
quarkus_hibernate_orm_panache_output_folder <- args[2]
renaissance_output_folder <- args[3]
logging_patterns_output_folder <- args[4]
memory_access_patterns_output_folder <- args[5]
throw_exception_patterns_output_folder <- args[6]

# Define the color palette (corresponding to each JVM) to be used in the final generated plot
# Note: use a color blindness palette (e.g., https://davidmathlogic.com/colorblind/)
full_color_palette <- c("OpenJDK HotSpot VM" = "#648FFF", "GraalVM CE" = "#FFB000", "GraalVM EE" = "#FE6100", "Native Image" = "#DC267F", "Azul Prime VM" = "#785EF0", "Eclipse OpenJ9 VM" = "#009E73")

plotBenchmarks <- function(output_folder, plot_title) {
  data <- readCsvResults(paste(output_folder, paste("summary", "power-consumption.csv", sep = "/"), sep = "/"))

  # delete all spaces from all column values
  data <- as.data.frame(apply(data, 2, function(x) gsub("\\s+", "", x)))

  # rename Geometric Mean column
  colnames(data)[colnames(data) == "Geometric.Mean..Watt.second."] <- "Score"

  # add a new Unit column
  data$Unit <- "Watt⋅sec"

  # if the Test column does not exist, copy the JVM column
  # Note: the Test column is used as an identifier for the X-axis in the final generated plot
  if (is.null(data$Test)) {
    data$Test <- data$JVM
  }

  # replace commas with dots for Score and Error columns
  # Note: this is needed for consistency across different platforms (e.g., Linux, macOS, etc.)
  # Example: on Linux the decimal separator could be "." but on macOS is ",", hence we need to make it consistent
  data$Score <- as.numeric(gsub(",", ".", data$Score))

  # keep only the necessary columns for plotting
  data <- data[, grep("^(JVM|Test|Score|Unit)$", colnames(data))]

  # rename JVM column values
  data$JVM[data$JVM == "openjdk-hotspot-vm"] <- "OpenJDK HotSpot VM"
  data$JVM[data$JVM == "graalvm-ce"] <- "GraalVM CE"
  data$JVM[data$JVM == "graalvm-ee"] <- "GraalVM EE"
  data$JVM[data$JVM == "native-image"] <- "Native Image"
  data$JVM[data$JVM == "azul-prime-vm"] <- "Azul Prime VM"
  data$JVM[data$JVM == "eclipse-openj9-vm"] <- "Eclipse OpenJ9 VM"

  plot <- generateJmhBarPlot(data, "JVM", "Legend", "", "Power Consumption (Watt⋅sec)", plot_title, full_color_palette)

  print(paste("Plotting", plot_title, "...", sep = " "))
  saveJmhBarPlot(data, plot, paste(output_folder, "plot", sep = "/"), "power-consumption")
}

plotBenchmarks(spring_petclinic_output_folder, "Spring PetClinic")
plotBenchmarks(quarkus_hibernate_orm_panache_output_folder, "Quarkus Hibernate ORM Panache Quickstart")
plotBenchmarks(renaissance_output_folder, "Renaissance")
plotBenchmarks(logging_patterns_output_folder, "Logging Patterns")
plotBenchmarks(memory_access_patterns_output_folder, "Memory_Access Patterns")
plotBenchmarks(throw_exception_patterns_output_folder, "Throw Exception Patterns")
