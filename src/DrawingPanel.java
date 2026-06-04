import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.function.Consumer;

public class DrawingPanel extends JPanel {

    private final ShapeManager shapeManager;
    private final Consumer<ShapeObject> selectionListener;

    private ToolType currentTool;
    private Color currentFillColor;
    private Color currentStrokeColor;
    private Point dragStartPoint;
    private Point lastDragPoint;
    private boolean draggingSelectedShape;

    public DrawingPanel(ShapeManager shapeManager, Consumer<ShapeObject> selectionListener) {
        this.shapeManager = shapeManager;
        this.selectionListener = selectionListener;
        this.currentTool = ToolType.RECTANGLE;
        this.currentFillColor = new Color(115, 166, 255);
        this.currentStrokeColor = Color.BLACK;
        this.draggingSelectedShape = false;

        setBackground(Color.WHITE);
        setFocusable(true);
        setupMouseHandlers();
        setupKeyboardHandlers();
    }

    public void setCurrentTool(ToolType tool) {
        currentTool = tool;
    }

    public Color getCurrentFillColor() {
        return currentFillColor;
    }

    public void setCurrentFillColor(Color currentFillColor) {
        this.currentFillColor = currentFillColor;
    }

    public Color getCurrentStrokeColor() {
        return currentStrokeColor;
    }

    public void setCurrentStrokeColor(Color currentStrokeColor) {
        this.currentStrokeColor = currentStrokeColor;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (ShapeObject shapeObject : shapeManager.getShapes()) {
            drawShape(g2d, shapeObject);
        }

        g2d.dispose();
    }

    private void drawShape(Graphics2D g2d, ShapeObject shapeObject) {
        Shape shape = createDrawableShape(shapeObject);

        if (shapeObject.getType() != ToolType.LINE) {
            g2d.setColor(shapeObject.getFillColor());
            g2d.fill(shape);
        }

        g2d.setStroke(new BasicStroke(2));
        g2d.setColor(shapeObject.getStrokeColor());
        g2d.draw(shape);

        if (shapeObject.isSelected()) {
            g2d.setStroke(new BasicStroke(2));
            g2d.setColor(Color.RED);
            g2d.draw(shape.getBounds2D());
        }
    }

    private Shape createDrawableShape(ShapeObject shapeObject) {
        int x = shapeObject.getX();
        int y = shapeObject.getY();
        int width = shapeObject.getWidth();
        int height = shapeObject.getHeight();

        switch (shapeObject.getType()) {
            case RECTANGLE:
                return new Rectangle(x, y, width, height);
            case CIRCLE:
                return new Ellipse2D.Double(x, y, width, height);
            case TRIANGLE:
                Polygon triangle = new Polygon();
                triangle.addPoint(x + width / 2, y);
                triangle.addPoint(x, y + height);
                triangle.addPoint(x + width, y + height);
                return triangle;
            case LINE:
                return new Line2D.Double(x, y, x + width, y + height);
            default:
                return new Rectangle(x, y, width, height);
        }
    }

    private void setupMouseHandlers() {
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                dragStartPoint = e.getPoint();
                lastDragPoint = e.getPoint();

                ShapeObject clickedShape = shapeManager.findShapeAt(e.getX(), e.getY());
                draggingSelectedShape = clickedShape != null;

                if (clickedShape != null) {
                    shapeManager.selectShape(clickedShape);
                    notifySelectionChanged();
                    repaint();
                    return;
                }

                shapeManager.clearSelection();
                notifySelectionChanged();
                repaint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (!draggingSelectedShape) {
                    return;
                }

                ShapeObject selectedShape = shapeManager.getSelectedShape();
                if (selectedShape == null) {
                    return;
                }

                int deltaX = e.getX() - lastDragPoint.x;
                int deltaY = e.getY() - lastDragPoint.y;
                selectedShape.moveBy(deltaX, deltaY);
                lastDragPoint = e.getPoint();
                notifySelectionChanged();
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (draggingSelectedShape) {
                    draggingSelectedShape = false;
                    return;
                }

                createShapeFromDrag(e.getPoint());
                dragStartPoint = null;
                lastDragPoint = null;
            }
        };

        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
    }

    private void setupKeyboardHandlers() {
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    shapeManager.deleteSelectedShape();
                    notifySelectionChanged();
                    repaint();
                }
            }
        });
    }

    private void createShapeFromDrag(Point endPoint) {
        if (dragStartPoint == null || dragStartPoint.distance(endPoint) < 4) {
            return;
        }

        int x = Math.min(dragStartPoint.x, endPoint.x);
        int y = Math.min(dragStartPoint.y, endPoint.y);
        int width = Math.abs(endPoint.x - dragStartPoint.x);
        int height = Math.abs(endPoint.y - dragStartPoint.y);

        if (currentTool == ToolType.LINE) {
            x = dragStartPoint.x;
            y = dragStartPoint.y;
            width = endPoint.x - dragStartPoint.x;
            height = endPoint.y - dragStartPoint.y;
        }

        ShapeObject createdShape = shapeManager.createShape(
                currentTool,
                x,
                y,
                width,
                height,
                currentFillColor,
                currentStrokeColor
        );
        selectionListener.accept(createdShape);
        repaint();
    }

    private void notifySelectionChanged() {
        selectionListener.accept(shapeManager.getSelectedShape());
    }
}
