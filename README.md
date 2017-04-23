# CSVGrapher
A graphing application desigend to provide efficient rendering independent of the number of data points.
The only limiting factor is computer memory.

Zoomed out sample data:

![zoomed-out](http://i.imgur.com/6TJ5UHJ.png)

Same sample data zoomed into the bottom left section:

![zoomed-in](http://i.imgur.com/Av36nEZ.png)

## Usage
Until the first release, the input CSV file path is set through a constant in the `Main` class. The program
will then read in the file, determine the bounds of the data, and then populate a quad tree with it.
The interface then immediately appears. In short:
  1. Set the path to the CSV file that contains the data
    * This file should have two columns, one for the x axis, one for the y axis
  2. Run the main function in `Main`
  3. Wait for interface to appear
  
## Controls
* Use **click and drag** to pan around the graph
* Use **scrolling** to zoom in and out
* Use **middle click** to return the the default zoom level

## Backend
CSVGrapher uses a quad tree data structure to boost efficiency. It only renders points that are relevant to the
current zoom level. If the user wants to zoom in to expose more detail, the tree will retrieve more points from
the data set, and if the user wants to zoom out to reduce the amount of detail, the tree will intelligently remove
data points from the graph. Therefore, the amount of points rendered is independent of the data size and provides
consistent speed regardless of input.
