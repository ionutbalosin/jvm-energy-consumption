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

source("../scripts/ggplot2/utils.r")

# Apply column data changes on the initial data frame
processCsvColumns <- function(data) {
  # trim all spaces from all column values
  data <- as.data.frame(apply(data, 2, function(x) trimws(x)))

  # rename some columns
  colnames(data)[colnames(data) == "Score.Metric"] <- "ScoreMetric"
  colnames(data)[colnames(data) == "Run.Identifier"] <- "RunIdentifier"
  colnames(data)[colnames(data) == "Sample.Identifier"] <- "SampleIdentifier"
  colnames(data)[colnames(data) == "Normalised.Energy"] <- "NormalisedEnergy"
  colnames(data)[colnames(data) == "Normalised.Throughput"] <- "NormalisedThroughput"

  # convert from string to numeric the Score column. In addition, convert commas to dots.
  if (!is.null(data$Score)) {
    data$Score <- as.numeric(gsub(",", ".", data$Score))
  }
  if (!is.null(data$NormalisedEnergy)) {
    data$NormalisedEnergy <- as.numeric(gsub(",", ".", data$NormalisedEnergy))
  }
  if (!is.null(data$NormalisedThroughput)) {
    data$NormalisedThroughput <- as.numeric(gsub(",", ".", data$NormalisedThroughput))
  }

  # if exists, convert from string to numeric the SampleIdentifier column
  if (!is.null(data$SampleIdentifier)) {
    data$SampleIdentifier <- as.numeric(gsub(",", ".", data$SampleIdentifier))
  }

  # add a new JVM identifier column based on Category
  data$JvmIdentifier <- data$Category

  # Special case: update the JVM identifier for the "native-image" with "pgo"
  data$JvmIdentifier[(data$Category == "native-image") & (data$RunIdentifier == "pgo")] <- "native-image-pgo"

  # update the JVM identifier values to be properly displayed in legend
  data$JvmIdentifier[data$JvmIdentifier == "openjdk-hotspot-vm"] <- "OpenJDK HotSpot VM"
  data$JvmIdentifier[data$JvmIdentifier == "graalvm-ce"] <- "GraalVM CE"
  data$JvmIdentifier[data$JvmIdentifier == "oracle-graalvm"] <- "Oracle GraalVM"
  data$JvmIdentifier[data$JvmIdentifier == "native-image"] <- "Native Image"
  data$JvmIdentifier[data$JvmIdentifier == "native-image-pgo"] <- "Native Image (pgo)"
  data$JvmIdentifier[data$JvmIdentifier == "azul-prime-vm"] <- "Azul Prime VM"
  data$JvmIdentifier[data$JvmIdentifier == "eclipse-openj9-vm"] <- "Eclipse OpenJ9 VM"

  # add a new Benchmark column
  if (is.null(data$Type)) {
    # if there are no test types (i.e., variations of the same benchmark application)
    # Special case: for the "native-image" with "pgo", concatenate it, otherwise use the Category
    category_run_identifier <- paste(data$Category, " (", data$RunIdentifier, ")", sep = "")
    data$Benchmark <- ifelse(data$RunIdentifier == "pgo", category_run_identifier, data$Category)
  } else {
    # if there are test types, use them as a benchmark name
    data$Benchmark <- data$Type
  }

  data
}

# Generate and saves the bar plot
plotBar <- function(data, output_folder, report_basename, report_type, plot_y_label, plot_title) {
  print(paste("Plotting scatter for",  plot_title, "(", report_basename, ",", report_type, ") ...", sep = " "))
  # Sort the data frame by 'JvmIdentifier column to appear chronologically
  data <- data[order(data$JvmIdentifier), ]
  plot <- generateBarPlot(data, "JvmIdentifier", "Legend", "", plot_y_label, plot_title, jdk_arch, jvm_color_palette_map)
  saveBarPlot(data, plot, paste(output_folder, "plot", sep = "/"), paste(report_basename, report_type , sep = "-"))
}

# Generate and saves the scatter plot
plotScatter <- function(data, output_folder, report_basename, report_type, plot_y_label, plot_title) {
  print(paste("Plotting scatter for",  plot_title, "(", report_basename, ",", report_type, ") ...", sep = " "))
  # Sort the data frame by 'JvmIdentifier' and then by 'SampleIdentifier' columns to appear chronologically
  data <- data[order(data$JvmIdentifier, data$SampleIdentifier), ]
  plot <- generateScatterPlot(data, "JvmIdentifier", "Legend", "Time", plot_y_label, plot_title, jdk_arch, jvm_color_palette_map)
  saveScatterPlot(data, plot, paste(output_folder, "plot", sep = "/"), paste(report_basename, report_type , sep = "-"))
}

# Generate and saves the scatter plot
plotSummaryScatter <- function(data, output_folder, report_basename, report_type, plot_title) {
  print(paste("Plotting scatter for",  plot_title, "(", report_basename, ",", report_type, ") ...", sep = " "))
  # Sort the data frame by 'JvmIdentifier' and then by 'SampleIdentifier' columns to appear chronologically
  data <- data[order(data$JvmIdentifier, data$NormalisedEnergy), ]
  plot <- generateSummaryScatterPlot(data, "JvmIdentifier", "Legend", "Energy", "Throughput", plot_title, jdk_arch, jvm_color_palette_map)
  saveScatterPlot(data, plot, paste(output_folder, "plot", sep = "/"), paste(report_basename, report_type , sep = "-"))
}

# Generate the bar plot
generateBarPlot <- function(data, fill, fillLabel, xLabel, yLabel, title, caption, color_palette) {
  plot <- ggplot(data, aes(x = Benchmark, y = Score, fill = data[, fill]))
  plot <- plot + geom_bar(stat = "identity", color = NA, position = "dodge", width = .7)
  plot <- plot + geom_text(aes(label = Score), color = "black", hjust = -0.05, vjust = .50, position = position_dodge(.7), size = 4)
  plot <- plot + labs(x = xLabel, y = yLabel, fill = fillLabel, title = title, caption = caption)
  plot <- plot + geom_hline(yintercept = 0)
  plot <- plot + coord_flip(clip = "off")
  plot <- plot + theme(
    text = element_text(size = 18),
    panel.background = element_rect(fill = NA, colour = NA, linewidth = 0.5, linetype = "solid"),
    panel.grid.major = element_line(linewidth = 0.5, linetype = "solid", colour = "grey95"),
    panel.grid.minor = element_line(linewidth = 0.25, linetype = "solid", colour = "grey95"),
    legend.spacing.y = unit(0.3, "cm"),
    legend.position = "bottom",
    plot.title = element_text(size = 18),
    plot.caption.position = "plot",
    plot.caption = element_text(hjust = 1),
    plot.margin = unit(c(0.5, 0.5, 0.5, 0.5), "cm"),
    axis.text.x = element_blank()
  )
  plot <- plot + guides(fill = guide_legend(byrow = TRUE, reverse = TRUE))
  plot <- plot + scale_fill_manual(fillLabel, values = color_palette, breaks = unique(data[[fill]]))

  plot
}

# Generate the scatter plot
generateScatterPlot <- function(data, fill, fillLabel, xLabel, yLabel, title, caption, color_palette) {
    plot <- ggplot(data, aes(x = SampleIdentifier, y = Score, group = JvmIdentifier, color = JvmIdentifier))
    plot <- plot + geom_point(size = 0.6)
    #plot <- plot + geom_line(linewidth = 0.5)
    plot <- plot + labs(x = xLabel, y = yLabel, fill = fillLabel, title = title, caption = caption)
    plot <- plot + theme(
     text = element_text(size = 18),
     panel.background = element_rect(fill = NA, colour = NA, linewidth = 0.5, linetype = "solid"),
     panel.grid.major = element_line(linewidth = 0.5, linetype = "solid", colour = "grey95"),
     panel.grid.minor = element_line(linewidth = 0.25, linetype = "solid", colour = "grey95"),
     legend.spacing.y = unit(0.3, "cm"),
     legend.position = "bottom",
     plot.title = element_text(size = 18),
     plot.caption.position = "plot",
     plot.caption = element_text(hjust = 1),
     plot.margin = unit(c(0.5, 0.5, 0.5, 0.5), "cm"),
     axis.text.x = element_blank(), # Remove x-axis labels (i.e., too many, it becomes cluttered)
     axis.line.x = element_line(color = "grey95"),
     axis.ticks.x = element_blank() # Remove x-axis (i.e., too many, it becomes cluttered)
    )
    plot <- plot + guides(fill = guide_legend(byrow = TRUE, reverse = TRUE))
    plot <- plot + guides(color = guide_legend(override.aes = list(size = 5)))
    plot <- plot + scale_colour_manual(fillLabel, values = color_palette, breaks = rev(unique(data$JvmIdentifier)))

    plot
}

# Generate the scatter plot
generateSummaryScatterPlot <- function(data, fill, fillLabel, xLabel, yLabel, title, caption, color_palette) {
    plot <- ggplot(data, aes(x = NormalisedEnergy, y = NormalisedThroughput, group = JvmIdentifier, color = JvmIdentifier))
    plot <- plot + geom_point(size = 6)
    plot <- plot + labs(x = xLabel, y = yLabel, fill = fillLabel, title = title, caption = caption)
    plot <- plot + theme(
     text = element_text(size = 18),
     panel.background = element_rect(fill = NA, colour = NA, linewidth = 0.5, linetype = "solid"),
     panel.grid.major = element_line(linewidth = 0.5, linetype = "solid", colour = "grey95"),
     panel.grid.minor = element_line(linewidth = 0.25, linetype = "solid", colour = "grey95"),
     legend.spacing.y = unit(0.3, "cm"),
     legend.position = "bottom",
     plot.title = element_text(size = 18),
     plot.caption.position = "plot",
     plot.caption = element_text(hjust = 1),
     plot.margin = unit(c(0.5, 0.5, 0.5, 0.5), "cm"),
     axis.line.x = element_line(color = "grey95"),
    )
    plot <- plot + guides(fill = guide_legend(byrow = TRUE, reverse = TRUE))
    plot <- plot + guides(color = guide_legend(override.aes = list(size = 5)))
    plot <- plot + scale_colour_manual(fillLabel, values = color_palette, breaks = rev(unique(data$JvmIdentifier)))

    plot
}

# Save the bar plot to a SVG output file
saveBarPlot <- function(data, plot, path, file_basename) {
  if (!empty(data)) {
    # set the height proportional to the number of rows plus 4 cm (as a minimum)
    # TODO: may be this could be replaced by another formula
    height <- nrow(data) * 2 + 4

    # create the path if does not exist
    if (!dir.exists(path)) {
      dir.create(path)
    }

    # save the plot
    ggsave(
      file = paste(path, paste(file_basename, "svg", sep = "."), sep = "/"),
      plot = plot,
      width = 50.8, # 1920 pixels
      height = height,
      dpi = 320,
      units = "cm",
      limitsize = FALSE,
      scale = 1
    )
  }
}

# Save the scatter plot to a SVG output file
saveScatterPlot <- function(data, plot, path, file_basename) {
  if (!empty(data)) {
    # create the path if does not exist
    if (!dir.exists(path)) {
      dir.create(path)
    }

    # save the plot
    ggsave(
      file = paste(path, paste(file_basename, "svg", sep = "."), sep = "/"),
      plot = plot,
      height = 15.24, # 590 pixels
      width = 50.8, # 1920 pixels
      dpi = 320,
      units = "cm",
      limitsize = FALSE,
      scale = 1
    )
  }
}
