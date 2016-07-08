package me.jeanlucthumm;

import javafx.application.Application;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;
import javafx.util.Pair;

import java.io.FileNotFoundException;
import java.io.IOError;

/**
 * Demonstration of quad tree insert and find, with visual feedback on subdivisions and
 * bound selections.
 *
 * @author Jean-Luc Thumm
 */
public class Main extends Application {

    // Constants
    public static final int POINT_RAD = 2;          // radius of points graphed
    public static final String PATH = "data.csv";   // path to data file (csv)
    public static final double ZOOM_INC = 0.1;        // amount of zoomLevel per scroll in percent

    // Controls
    public static final MouseButton PAN_BUTTON = MouseButton.PRIMARY;
    public static final MouseButton SELECT_BUTTON = MouseButton.SECONDARY;
    public static final MouseButton RESET_ZOOM_BUTTON = MouseButton.MIDDLE;

    private Group root;             // holds all other nodes
    private Canvas canvas;          // where graphing will occur
    private GraphicsContext gc;     // gc of canvas
    private QuadTree tree;          // contains points for logn access times
    private Point2D selecAnchor;    // stores anchor of each selection rectangle
    private Point2D panAnchor;      // stores anchor of each pan
    private Rectangle selecRec;     // actual selection rectangle
    private ZoomLevel initZoom;     // standard zoomLevel level
    private ZoomLevel zoomLevel;    // current zoomLevel level

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Set up hierarchy
        primaryStage.setTitle("Graph");
        root = new Group();
        Scene scene = new Scene(root, 600, 600);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false); // TODO make this resizable

        // Set up canvas
        canvas = new Canvas();
        gc = canvas.getGraphicsContext2D();
        canvas.heightProperty().bind(scene.heightProperty());
        canvas.widthProperty().bind(scene.widthProperty());
        canvas.getTransforms().add(new Translate(0, scene.getHeight())); // need origin in bottom left
        canvas.getTransforms().add(new Scale(1, -1));
        root.getChildren().add(canvas);

        // Set up event handlers
        root.setOnMousePressed(this::captureSelectionAnchor);
        root.setOnMouseDragged(this::dragSelection);
        root.setOnMouseReleased(this::endSelection);
        canvas.setOnScroll(this::zoom);
        canvas.setOnMousePressed(this::handleCanvasClick);
        canvas.setOnMouseDragged(this::dragPan);

        // Generate and populate tree
        CSVReader reader = new CSVReader(PATH);
        try {
            // Create initial zoomLevel level
            System.out.println("Getting bounds...");
            Pair<Point2D, Point2D> corners = reader.getMinMax();
            Point2D min = corners.getKey();
            Point2D max = corners.getValue();
            double initWidth = max.getX() - min.getX();
            double initHeight = max.getY() - min.getY();
            initZoom = new ZoomLevel(min, scene.getWidth() / initWidth, scene.getHeight() / initHeight);

            // Create tree
            System.out.println("Initializing tree...");
            Rectangle2D bounds = new Rectangle2D(min.getX(), min.getY(), initWidth, initHeight);
            tree = new QuadTree(bounds);
            reader.readData(tree);

            // Zoom out a little for padding on the edges
            Point2D source = new Point2D(canvas.getWidth() / 2, canvas.getHeight() / 2); // zoom out evenly
            initZoom.setZoom(source, tree.getBounds(), -0.05);
            zoomLevel = new ZoomLevel(initZoom);

        } catch (FileNotFoundException e) {
            System.err.println(CSVReader.NOFIND_MSG + PATH);
            return;
        } catch (IOError e) {
            System.err.println(CSVReader.IOERROR_MSG + PATH);
            return;
        }

        gc.setFill(Color.BLUE);
        clearAndGraph();

        // Display to user
        primaryStage.show();
    }

    private void clearAndGraph() {
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        tree.graphPointsAndBoundaries(gc, zoomLevel);
    }

    private void zoom(ScrollEvent event) {
        if (event.getDeltaY() > 0) { // Zoom in
            Point2D source = new Point2D(event.getX(), event.getY());
            zoomLevel.setZoom(source, tree.getBounds(), ZOOM_INC);
            clearAndGraph();
        } else if (event.getDeltaY() < 0){ // Zoom out
            Point2D source = new Point2D(event.getX(), event.getY());
            zoomLevel.setZoom(source, tree.getBounds(), -ZOOM_INC);
            clearAndGraph();
        }
    }

    /** Handle clicks on canvas **/
    private void handleCanvasClick(MouseEvent event) {
        if (event.getButton() == PAN_BUTTON) {
            // Capture panAnchor
            panAnchor = new Point2D(event.getX(), event.getY());

        } else if (event.getButton() == RESET_ZOOM_BUTTON) {
            // Reset zoom
            zoomLevel = new ZoomLevel(initZoom);
            clearAndGraph();
        }
    }

    private void dragPan(MouseEvent event) {
        if (event.getButton() != PAN_BUTTON) return;

        // Convert delta to original coordinates
        Point2D delta = panAnchor.subtract(new Point2D(event.getX(), event.getY()));
        delta = zoomLevel.convertDeltaToOriginal(delta);

        zoomLevel.setPanDelta(delta);
        clearAndGraph();
        panAnchor = new Point2D(event.getX(), event.getY());
    }

    /** Captures root point for selection rectangle */
    private void captureSelectionAnchor(MouseEvent event) {
        // Create selection rectangle and prep for dragging
        if (event.getButton() != SELECT_BUTTON) return;
        selecAnchor = new Point2D(event.getX(), event.getY());
        selecRec = new Rectangle(selecAnchor.getX(), selecAnchor.getY(), 0, 0);
        selecRec.setStroke(Color.BLACK);
        selecRec.setFill(Color.TRANSPARENT);
        root.getChildren().add(selecRec);
    }

    /** Resizes selection rectangle */
    private void dragSelection(MouseEvent event) {
        if (event.getButton() != SELECT_BUTTON) return;
        double newWidth = event.getX() - selecAnchor.getX();
        double newHeight = event.getY() - selecAnchor.getY();

        // Handle rectangles growing in all direction from their anchor point
        if (newWidth >= 0) {
            selecRec.setWidth(newWidth);
            selecRec.setX(selecAnchor.getX());
        } else {
            selecRec.setWidth(-newWidth);
            selecRec.setX(selecAnchor.getX() + newWidth);
        }
        if (newHeight >= 0) {
            selecRec.setHeight(newHeight);
            selecRec.setY(selecAnchor.getY());
        } else {
            selecRec.setHeight(-newHeight);
            selecRec.setY(selecAnchor.getY() + newHeight);
        }
    }

    /** Removes selection triangle and delegates to tree to find points */
    private void endSelection(MouseEvent event) {
        // Convert to canvas coordinates
        if (event.getButton() != SELECT_BUTTON) return;
        Point2D canvasPt = canvas.parentToLocal(selecRec.getX(), selecRec.getY());
        Rectangle2D selection = new Rectangle2D(canvasPt.getX(), canvasPt.getY(),
                selecRec.getWidth(), selecRec.getHeight());

        // Get rid of selection rectangle
        root.getChildren().remove(selecRec);
        selecRec = null;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
