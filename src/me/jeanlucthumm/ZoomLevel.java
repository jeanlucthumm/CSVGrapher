package me.jeanlucthumm;

import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.shape.Rectangle;

class ZoomLevel {
    private Point2D anchor;
    private double widthRatio;
    private double heightRatio;

    ZoomLevel(Point2D anchor, double widthRatio, double heightRatio) {
        this.anchor = anchor;
        this.widthRatio = widthRatio;
        this.heightRatio = heightRatio;
    }

    ZoomLevel(Rectangle2D unzoomed, Rectangle2D zoomed) {
        anchor = new Point2D(zoomed.getMinX(), zoomed.getMinY());
        widthRatio = zoomed.getWidth() / unzoomed.getWidth();
        heightRatio = zoomed.getHeight() / unzoomed.getHeight();
    }

    Point2D convertToOriginal(Point2D canvasPt) {
        double x = anchor.getX() + canvasPt.getX() / widthRatio;
        double y = anchor.getY() + canvasPt.getY() / heightRatio;
        return new Point2D(x, y);
    }

    Rectangle2D convertToOriginal(Rectangle2D canvasRec) {
        Point2D min = new Point2D(canvasRec.getMinX(), canvasRec.getMinY());
        Point2D originalMin = convertToOriginal(min);
        double width = canvasRec.getWidth() / widthRatio;
        double height = canvasRec.getHeight() / heightRatio;
        return new Rectangle2D(originalMin.getX(), originalMin.getY(), width, height);
    }

    Point2D convertToLocal(Point2D treePt) {
        double x = (treePt.getX() - anchor.getX()) * widthRatio;
        double y = (treePt.getY() - anchor.getY()) * heightRatio;
        return new Point2D(x, y);
    }

    Rectangle2D convertToLocal(Rectangle2D treeRec) {
        Point2D min = new Point2D(treeRec.getMinX(), treeRec.getMinY());
        Point2D localMin = convertToLocal(min);
        double width = treeRec.getWidth() * widthRatio;
        double height = treeRec.getHeight() * heightRatio;
        return new Rectangle2D(localMin.getX(), localMin.getY(), width, height);
    }

    ZoomLevel zoom(Rectangle2D original, double percent) {
        double adjPercent = 1 + percent;
        double widthOffset = percent * original.getWidth();
        double heightOffset = percent * original.getHeight();
        Point2D min = new Point2D(anchor.getX() + widthOffset, anchor.getY() + heightOffset);
        return new ZoomLevel(min, adjPercent * widthRatio, adjPercent * heightRatio);
    }
}
