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

loadLibrary <- function(name) {
  if (!require(name, character.only = TRUE)) {
    install.packages(name)
    library(name, character.only = TRUE)
  }
}

# Load the necessary libraries
loadLibrary("ggplot2")
loadLibrary("svglite")
loadLibrary("styler")
loadLibrary("plyr")
loadLibrary("psych")
#loadLibrary("tools")

# apply styles to all R and/or Rmd files in the directory
style_dir()

# use a large positive value like 999 to prevent the scientific notation
options(scipen = 999)

# Read the CSV results from file
readCsvResultsFromFile <- function(file_path) {
  result <- data.frame()

  tryCatch(
    {
      csv_file <- file(file_path, "r")
      result <- read.csv(file_path, sep = ";", comment = "#", header = TRUE)
    },
    warning = function(w) {
      print(paste("Cannot read from", file_path, "File skipped.", sep = " "))
    },
    error = function(e) {
      print(paste("Cannot read from", file_path, "File skipped.", sep = " "))
    },
    finally = {
      if (exists("csv_file")) {
        close(csv_file)
      }
    }
  )

  return(result)
}
