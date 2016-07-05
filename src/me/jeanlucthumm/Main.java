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
import org.w3c.dom.css.Rect;

import java.io.FileNotFoundException;
import java.io.IOError;
import java.util.Stack;

/**
 * Demonstration of quad tree insert and find, with visual feedback on subdivisions and
 * bound selections.
 *
 * @author Jean-Luc Thumm
 */
public class Main extends Application {

    // Constants
    public static final int POINT_RAD = 2;          // radius of points graphed
    public static final String PATH = "data.csv";
    public static final double ZOOM_INC = 5;        // amount of zoom per scroll in percent

    private Group root;             // holds all other nodes
    private Canvas canvas;          // where graphing will occur
    private GraphicsContext gc;     // gc of canvas
    private QuadTree tree;          // contains points for logn access times
    private Point2D selecAnchor;    // stores anchor of each selection rectangle
    private Rectangle selecRec;     // actual selection rectangle
    private ZoomLevel initZoom;     // furthest away zoom level
    private Stack<ZoomLevel> zoomLog;   // keeps track of all zooms

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
        canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, this::canvasClicked);
        canvas.heightProperty().bind(scene.heightProperty());
        canvas.widthProperty().bind(scene.widthProperty());
        canvas.getTransforms().add(new Translate(0, scene.getHeight())); // need origin in bottom left
        canvas.getTransforms().add(new Scale(1, -1));
        root.getChildren().add(canvas);

        zoomLog = new Stack<>();

        // Set up root event handlers
        root.setOnMousePressed(this::captureSelectionAnchor);
        root.setOnMouseDragged(this::dragSelection);
        root.setOnMouseReleased(this::endSelection);
        canvas.setOnScroll(this::zoom);

        // Generate and populate tree
        CSVReader reader = new CSVReader(PATH);
        try {
            // Create initial zoom level
            Pair<Point2D, Point2D> corners = reader.getMinMax();
            Point2D min = corners.getKey();
            Point2D max = corners.getValue();
            double initWidth = max.getX() - min.getX();
            double initHeight = max.getY() - min.getY();
            initZoom = new ZoomLevel(min, scene.getWidth() / initWidth, scene.getHeight() / initHeight);

            // Create tree
            Rectangle2D bounds = new Rectangle2D(min.getX(), min.getY(), initWidth, initHeight);
            tree = new QuadTree(bounds);
            reader.readData(tree);
        } catch (FileNotFoundException e) {
            System.err.println(CSVReader.NOFIND_MSG + PATH);
            return;
        } catch (IOError e) {
            System.err.println(CSVReader.IOERROR_MSG + PATH);
            return;
        }
        gc.setFill(Color.BLUE);
        tree.graphPointsAndBoundaries(gc, initZoom);
//        tree.graphPoints(gc, initZoom);

        zoomLog.push(initZoom);

        // Display to user
        primaryStage.show();
    }

    private void zoom(ScrollEvent event) {
        // TODO this pops the initial zoom. NO!
        if (event.getDeltaY() > 0) {
            ZoomLevel level = zoomLog.peek().zoom(tree.getBounds(), 0.05);
            zoomLog.push(level);
            gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
            tree.graphPointsAndBoundaries(gc, level);
        } else if (event.getDeltaY() < 0){
            if (zoomLog.size() == 1) return;
            ZoomLevel level = zoomLog.pop();
            gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
            tree.graphPointsAndBoundaries(gc, level);
        }
    }

    /** Handle clicks on canvas **/
    private void canvasClicked(MouseEvent event) {
        if (event.getButton() == MouseButton.SECONDARY) {
        }
    }

    /** Captures root point for selection rectangle */
    private void captureSelectionAnchor(MouseEvent event) {
        // Create selection rectangle and prep for dragging
        if (event.getButton() != MouseButton.PRIMARY) return;
        selecAnchor = new Point2D(event.getX(), event.getY());
        selecRec = new Rectangle(selecAnchor.getX(), selecAnchor.getY(), 0, 0);
        selecRec.setStroke(Color.BLACK);
        selecRec.setFill(Color.TRANSPARENT);
        root.getChildren().add(selecRec);
    }

    /** Resizes selection rectangle */
    private void dragSelection(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY) return;
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
        if (event.getButton() != MouseButton.PRIMARY) return;
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
























