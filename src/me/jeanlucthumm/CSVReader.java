package me.jeanlucthumm;

import javafx.geometry.Point2D;
import javafx.util.Pair;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

class CSVReader {

    static final String IOERROR_MSG = "I/O Error ";
    static final String NOFIND_MSG = "Could not find file ";
    private static final String EMPTY_MSG = "File empty ";

    private String path;

    CSVReader(String path) {
        this.path = path;
    }

    boolean readData(QuadTree tree) throws IOException {
        // Try to set up reader
        FileReader file = new FileReader(path);
        BufferedReader in = new BufferedReader(file);

        // Skip header
        if (in.readLine() == null) {
            System.err.println(EMPTY_MSG + path);
            in.close();
            file.close();
            return false;
        }

        // Read every point and add to tree
        String line;
        while ((line = in.readLine()) != null) {
            String[] data = line.trim().split(",");
            double x = Double.parseDouble(data[0]);
            double y = Double.parseDouble(data[1]);
            Point2D point = new Point2D(x, y);
            if (!tree.add(point));
//                System.err.println("Could not add point: " + point); // TODO uncomment
        }
        in.close();
        file.close();
        return true;
    }

    Pair<Point2D, Point2D> getMinMax() throws IOException {
        // Try to set up reader
        FileReader file = new FileReader(path);
        BufferedReader in = new BufferedReader(file);

        // Skip header
        if (in.readLine() == null) {
            System.err.println(EMPTY_MSG + path);
            in.close();
            file.close();
            return null;
        }

        // Get the min values for each axis
        double minX = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;

        String line;
        while ((line = in.readLine()) != null) {
            String[] data = line.trim().split(",");
            double x = Double.parseDouble(data[0]);
            double y = Double.parseDouble(data[1]);
            // Check bounds
            if (x < minX) minX = x;
            if (x > maxX) maxX = x;
            if (y < minY) minY = y;
            if (y > maxY) maxY = y;
        }
        in.close();
        file.close();
        return new Pair<>(new Point2D(minX, minY), new Point2D(maxX, maxY));
    }
}
