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
import java.awt.geom.AffineTransform;
import java.util.function.Consumer;

public class DrawingPanel extends JPanel {

    private final ShapeManager shapeManager;
    private final Consumer<ShapeObject> selectionListener;

    private ToolType currentTool;
    private Color currentFillColor;
    private Color currentStrokeColor;
    private Point dragStartPoint;
    private Point lastDragPoint;
    private Point currentDragPoint;
    private boolean draggingSelectedShape;

    public DrawingPanel(ShapeManager shapeManager, Consumer<ShapeObject> selectionListener) {
        this.shapeManager = shapeManager;
        this.selectionListener = selectionListener;
        this.currentTool = ToolType.SELECT;
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

        if (dragStartPoint != null && currentDragPoint != null && !draggingSelectedShape) {
            drawPreview(g2d);
        }

        g2d.dispose();
    }

    private void drawShape(Graphics2D g2d, ShapeObject shapeObject) {
        Shape shape = createDrawableShape(shapeObject);

        AffineTransform originalTransform = g2d.getTransform();

        // High-DPI Scaling Fix: Keep the original transform and concatenate
        AffineTransform transform = new AffineTransform(originalTransform);
        transform.concatenate(shapeObject.getTransform());

        g2d.setTransform(transform);

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

        // refleksi horizontal
        if (shapeObject.isReflected()) {
            AffineTransform reflectTransform = new AffineTransform(originalTransform);
            reflectTransform.concatenate(shapeObject.getReflectionTransform());

            g2d.setTransform(reflectTransform);

            if (shapeObject.getType() != ToolType.LINE) {
                Color fillColor = shapeObject.getFillColor();
                g2d.setColor(new Color(fillColor.getRed(), fillColor.getGreen(), fillColor.getBlue(), 120));
                g2d.fill(shape);
            }
            g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{6, 4}, 0));
            g2d.setColor(shapeObject.getStrokeColor());
            g2d.draw(shape);

            // Also draw selection border for reflected shape
            if (shapeObject.isSelected()) {
                g2d.setStroke(new BasicStroke(2));
                g2d.setColor(Color.RED);
                g2d.draw(shape.getBounds2D());
            }
        }
        g2d.setTransform(originalTransform);
    }

    private void drawPreview(Graphics2D g2d) {
        if (dragStartPoint.distance(currentDragPoint) < 4) {
            return;
        }

        int x = Math.min(dragStartPoint.x, currentDragPoint.x);
        int y = Math.min(dragStartPoint.y, currentDragPoint.y);
        int width = Math.abs(currentDragPoint.x - dragStartPoint.x);
        int height = Math.abs(currentDragPoint.y - dragStartPoint.y);

        if (currentTool == ToolType.LINE) {
            x = dragStartPoint.x;
            y = dragStartPoint.y;
            width = currentDragPoint.x - dragStartPoint.x;
            height = currentDragPoint.y - dragStartPoint.y;
        }

        ShapeObject tempShape = new ShapeObject(-1, currentTool, x, y, width, height);
        tempShape.setFillColor(new Color(
                currentFillColor.getRed(),
                currentFillColor.getGreen(),
                currentFillColor.getBlue(),
                100
        ));
        tempShape.setStrokeColor(currentStrokeColor);

        Shape shape = createDrawableShape(tempShape);

        AffineTransform originalTransform = g2d.getTransform();
        AffineTransform previewTx = new AffineTransform(originalTransform);
        // Posisikan bentuk preview menggunakan translasi ke (x, y)
        previewTx.translate(x, y);
        g2d.setTransform(previewTx);

        if (tempShape.getType() != ToolType.LINE) {
            g2d.setColor(tempShape.getFillColor());
            g2d.fill(shape);
        }

        g2d.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{5.0f, 5.0f}, 0.0f));
        g2d.setColor(tempShape.getStrokeColor());
        g2d.draw(shape);

        g2d.setTransform(originalTransform);
    }

    private Shape createDrawableShape(ShapeObject shapeObject) {
        int width = shapeObject.getWidth();
        int height = shapeObject.getHeight();

        switch (shapeObject.getType()) {
            case RECTANGLE:
                return new Rectangle(0, 0, width, height);
            case CIRCLE:
                return new Ellipse2D.Double(0, 0, width, height);
            case TRIANGLE:
                Polygon triangle = new Polygon();
                triangle.addPoint(width / 2, 0);
                triangle.addPoint(0, height);
                triangle.addPoint(width, height);
                return triangle;
            case LINE:
                return new Line2D.Double(0, 0, width, height);
            default:
                return new Rectangle(0, 0, width, height);
        }
    }

    private void setupMouseHandlers() {
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                dragStartPoint = e.getPoint();
                lastDragPoint = e.getPoint();
                currentDragPoint = e.getPoint();

                if (currentTool == ToolType.SELECT) {
                    ShapeObject clickedShape = shapeManager.findShapeAt(e.getX(), e.getY());
                    draggingSelectedShape = clickedShape != null;

                    if (clickedShape != null) {
                        shapeManager.selectShape(clickedShape);
                        notifySelectionChanged();
                        repaint();
                    } else {
                        shapeManager.clearSelection();
                        notifySelectionChanged();
                        repaint();
                    }
                } else {
                    draggingSelectedShape = false;
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                currentDragPoint = e.getPoint();
                if (currentTool == ToolType.SELECT) {
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
                } else {
                    // Update preview
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (currentTool == ToolType.SELECT) {
                    draggingSelectedShape = false;
                    dragStartPoint = null;
                    lastDragPoint = null;
                    currentDragPoint = null;
                    return;
                }

                createShapeFromDrag(e.getPoint());
                dragStartPoint = null;
                lastDragPoint = null;
                currentDragPoint = null;
                repaint();
            }
        };

        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
    }

    private void setupKeyboardHandlers() {
        // Use Key Bindings instead of KeyListener for VK_DELETE so it works window-wide
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "deleteShape");
        getActionMap().put("deleteShape", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                shapeManager.deleteSelectedShape();
                notifySelectionChanged();
                repaint();
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
