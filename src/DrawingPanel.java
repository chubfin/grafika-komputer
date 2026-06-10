import javax.swing.JPanel;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.JColorChooser;

public class DrawingPanel extends JPanel {

    private static final Color WORKSPACE_BG = new Color(27, 30, 33);
    private static final Color CANVAS_BG = Color.WHITE;
    private static final Color CANVAS_BORDER = new Color(170, 176, 184);
    private static final int CANVAS_MAX_WIDTH = 920;
    private static final int CANVAS_MARGIN = 24;
    private static final int CANVAS_ARC = 18;
    private static final Color SELECTION_BLUE = new Color(55, 142, 219);
    private static final Color SELECTION_FILL = new Color(55, 142, 219, 28);

    private final ShapeManager shapeManager;
    private final Consumer<ShapeObject> selectionListener;

    // -------------------------------------------------------------------------
    // Sistem Undo/Redo - Ditambahkan oleh Anggota 4
    // HistoryManager mengelola undoStack dan redoStack via Command Pattern
    // -------------------------------------------------------------------------
    private final HistoryManager historyManager;

    private ToolType currentTool;
    private Color currentFillColor;
    private Color currentFillSecondaryColor;
    private boolean currentFillEnabled;
    private boolean currentGradientFill;
    private Color currentStrokeColor;
    private float currentStrokeWidth;
    private LineStyle currentLineStyle;
    private Point dragStartPoint;
    private Point lastDragPoint;
    private Point currentDragPoint;
    private boolean draggingSelectedShape;

    // -------------------------------------------------------------------------
    // Multi-Selection support - Anggota 4
    // -------------------------------------------------------------------------
    /** true saat kita sedang menggambar rubber-band selection */
    private boolean rubberBanding = false;

    public DrawingPanel(ShapeManager shapeManager, Consumer<ShapeObject> selectionListener) {
        this.shapeManager = shapeManager;
        this.selectionListener = selectionListener;
        this.currentTool = ToolType.SELECT;
        this.currentFillColor = new Color(115, 166, 255);
        this.currentFillSecondaryColor = new Color(173, 216, 230);
        this.currentFillEnabled = false;
        this.currentGradientFill = false;
        this.currentStrokeColor = Color.BLACK;
        this.currentStrokeWidth = 2.0f;
        this.currentLineStyle = LineStyle.SOLID;
        this.draggingSelectedShape = false;

        // Inisialisasi HistoryManager - Anggota 4
        this.historyManager = new HistoryManager();

        setBackground(WORKSPACE_BG);
        setFocusable(true);
        setupMouseHandlers();
        setupKeyboardHandlers();
    }

    /**
     * Mengekspos HistoryManager ke MainFrame agar tombol/menu Undo-Redo
     * di toolbar dapat memanggil historyManager.undo() / redo().
     * Ditambahkan oleh Anggota 4.
     */
    public HistoryManager getHistoryManager() {
        return historyManager;
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

    public Color getCurrentFillSecondaryColor() {
        return currentFillSecondaryColor;
    }

    public void setCurrentFillSecondaryColor(Color currentFillSecondaryColor) {
        this.currentFillSecondaryColor = currentFillSecondaryColor;
    }

    public boolean isCurrentFillEnabled() {
        return currentFillEnabled;
    }

    public void setCurrentFillEnabled(boolean currentFillEnabled) {
        this.currentFillEnabled = currentFillEnabled;
    }

    public boolean isCurrentGradientFill() {
        return currentGradientFill;
    }

    public void setCurrentGradientFill(boolean currentGradientFill) {
        this.currentGradientFill = currentGradientFill;
    }

    public Color getCurrentStrokeColor() {
        return currentStrokeColor;
    }

    public void setCurrentStrokeColor(Color currentStrokeColor) {
        this.currentStrokeColor = currentStrokeColor;
    }

    public float getCurrentStrokeWidth() {
        return currentStrokeWidth;
    }

    public void setCurrentStrokeWidth(float currentStrokeWidth) {
        this.currentStrokeWidth = currentStrokeWidth;
    }

    public LineStyle getCurrentLineStyle() {
        return currentLineStyle;
    }

    public void setCurrentLineStyle(LineStyle currentLineStyle) {
        this.currentLineStyle = currentLineStyle != null ? currentLineStyle : LineStyle.SOLID;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Rectangle canvasBounds = getCanvasBounds();
        Shape canvasShape = getCanvasShape(canvasBounds);
        drawCanvasSurface(g2d, canvasBounds);
        Shape oldClip = g2d.getClip();
        g2d.setClip(canvasShape);

        // Gambar semua shape (GroupObject sudah digambar via member-nya)
        for (ShapeObject shapeObject : shapeManager.getShapes()) {
            if (shapeObject instanceof GroupObject) {
                drawGroup(g2d, (GroupObject) shapeObject);
            } else {
                drawShape(g2d, shapeObject);
            }
        }

        // Gambar area irisan dua shape yang keduanya terpilih
        drawIntersections(g2d);

        // Preview shape saat drag
        if (dragStartPoint != null && currentDragPoint != null && !draggingSelectedShape && !rubberBanding) {
            drawPreview(g2d);
        }

        // Rubber-band selection rectangle
        if (rubberBanding && dragStartPoint != null && currentDragPoint != null) {
            drawRubberBand(g2d);
        }

        g2d.setClip(oldClip);
        g2d.dispose();
    }

    private Rectangle getCanvasBounds() {
        int width = Math.min(CANVAS_MAX_WIDTH, Math.max(1, getWidth() - CANVAS_MARGIN * 2));
        int height = Math.max(1, getHeight() - CANVAS_MARGIN * 2);
        int x = Math.max(CANVAS_MARGIN, (getWidth() - width) / 2);
        return new Rectangle(x, CANVAS_MARGIN, width, height);
    }

    private void drawCanvasSurface(Graphics2D g2d, Rectangle canvasBounds) {
        Shape canvasShape = getCanvasShape(canvasBounds);
        g2d.setColor(CANVAS_BG);
        g2d.fill(canvasShape);
        g2d.setColor(CANVAS_BORDER);
        g2d.draw(canvasShape);
    }

    private Shape getCanvasShape(Rectangle bounds) {
        return new RoundRectangle2D.Double(
                bounds.x, bounds.y,
                bounds.width, bounds.height,
                CANVAS_ARC, CANVAS_ARC);
    }

    private boolean isInCanvas(Point point) {
        return getCanvasShape(getCanvasBounds()).contains(point);
    }

    private Point clampToCanvas(Point point) {
        Rectangle bounds = getCanvasBounds();
        int x = Math.max(bounds.x, Math.min(bounds.x + bounds.width, point.x));
        int y = Math.max(bounds.y, Math.min(bounds.y + bounds.height, point.y));
        return new Point(x, y);
    }

    /** Gambar group: gambar semua member, lalu outline bounding box group. */
    private void drawGroup(Graphics2D g2d, GroupObject group) {
        for (ShapeObject member : group.getMembers()) {
            drawShape(g2d, member);
        }
        // Outline group (bounding box biru putus-putus)
        Rectangle bounds = group.getTransformedShape().getBounds();
        g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                10, new float[]{6, 4}, 0));
        g2d.setColor(new Color(0, 100, 220));
        g2d.draw(bounds);

        // Highlight selection
        if (group.isSelected()) {
            g2d.setStroke(new BasicStroke(2.2f));
            g2d.setColor(SELECTION_BLUE);
            g2d.draw(new Rectangle(bounds.x - 3, bounds.y - 3,
                    bounds.width + 6, bounds.height + 6));
        }
    }

    private void drawShape(Graphics2D g2d, ShapeObject shapeObject) {
        Shape shape = createDrawableShape(shapeObject);

        AffineTransform originalTransform = g2d.getTransform();

        AffineTransform transform = new AffineTransform(originalTransform);
        transform.concatenate(shapeObject.getTransform());
        g2d.setTransform(transform);

        if (shapeObject.getType() != ToolType.LINE && shapeObject.isFillEnabled()) {
            Paint paint = shapeObject.isGradientFill()
                    ? new GradientPaint(0, 0, shapeObject.getFillColor(),
                    Math.max(1, shapeObject.getWidth()), Math.max(1, shapeObject.getHeight()),
                    shapeObject.getFillSecondaryColor())
                    : shapeObject.getFillColor();
            g2d.setPaint(paint);
            g2d.fill(shape);
        }

        g2d.setStroke(createStroke(shapeObject.getStrokeWidth(), shapeObject.getLineStyle()));
        g2d.setColor(shapeObject.getStrokeColor());
        g2d.draw(shape);

        if (shapeObject.isSelected()) {
            Rectangle bounds = shape.getBounds();
            g2d.setColor(SELECTION_FILL);
            g2d.fill(bounds);
            g2d.setStroke(new BasicStroke(2));
            g2d.setColor(SELECTION_BLUE);
            g2d.draw(bounds);
            drawSelectionHandles(g2d, bounds);
        }

        // refleksi horizontal
        if (shapeObject.isReflected()) {
            AffineTransform reflectTransform = new AffineTransform(originalTransform);
            reflectTransform.concatenate(shapeObject.getReflectionTransform());
            g2d.setTransform(reflectTransform);

            if (shapeObject.getType() != ToolType.LINE && shapeObject.isFillEnabled()) {
                Color fillColor = shapeObject.getFillColor();
                if (shapeObject.isGradientFill()) {
                    Paint paint = new GradientPaint(0, 0,
                            new Color(fillColor.getRed(), fillColor.getGreen(), fillColor.getBlue(), 120),
                            Math.max(1, shapeObject.getWidth()), Math.max(1, shapeObject.getHeight()),
                            new Color(shapeObject.getFillSecondaryColor().getRed(),
                                    shapeObject.getFillSecondaryColor().getGreen(),
                                    shapeObject.getFillSecondaryColor().getBlue(), 120));
                    g2d.setPaint(paint);
                } else {
                    g2d.setColor(new Color(fillColor.getRed(), fillColor.getGreen(), fillColor.getBlue(), 120));
                }
                g2d.fill(shape);
            }
            g2d.setStroke(createStroke(shapeObject.getStrokeWidth(), shapeObject.getLineStyle()));
            g2d.setColor(shapeObject.getStrokeColor());
            g2d.draw(shape);

            if (shapeObject.isSelected()) {
                g2d.setStroke(new BasicStroke(2));
                g2d.setColor(SELECTION_BLUE);
                g2d.draw(shape.getBounds2D());
            }
        }
        g2d.setTransform(originalTransform);
    }

    private void drawSelectionHandles(Graphics2D g2d, Rectangle bounds) {
        int size = 8;
        int half = size / 2;
        int[] xs = {bounds.x, bounds.x + bounds.width / 2, bounds.x + bounds.width};
        int[] ys = {bounds.y, bounds.y + bounds.height / 2, bounds.y + bounds.height};

        g2d.setColor(SELECTION_BLUE);
        for (int x : xs) {
            for (int y : ys) {
                if (x == bounds.x + bounds.width / 2 && y == bounds.y + bounds.height / 2) {
                    continue;
                }
                g2d.fillOval(x - half, y - half, size, size);
            }
        }
    }

    /**
     * Gambar area irisan untuk setiap pasangan shape non-LINE non-GROUP.
     * Warna irisan diambil dari ShapeManager (custom) atau di-blend otomatis.
     */
    private void drawIntersections(Graphics2D g2d) {
        List<ShapeObject> all = shapeManager.getShapes();
        for (int i = 0; i < all.size(); i++) {
            for (int j = i + 1; j < all.size(); j++) {
                ShapeObject a = all.get(i);
                ShapeObject b = all.get(j);
                if (a.getType() == ToolType.LINE || b.getType() == ToolType.LINE) continue;
                if (a instanceof GroupObject || b instanceof GroupObject) continue;

                Area areaA = new Area(a.getTransformedShape());
                Area areaB = new Area(b.getTransformedShape());
                areaA.intersect(areaB);

                if (!areaA.isEmpty()) {
                    // Gunakan warna custom kalau sudah di-set, kalau tidak blend otomatis
                    Color custom = shapeManager.getIntersectionColor(a, b);
                    if (custom == null) {
                        continue;
                    }

                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
                    g2d.setColor(custom);
                    g2d.fill(areaA);
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
                    g2d.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                            10, new float[]{4, 3}, 0));
                    g2d.setColor(custom.darker());
                    g2d.draw(areaA);
                }
            }
        }
    }

    private Color blendColors(Color a, Color b) {
        return new Color(
                (a.getRed()   + b.getRed())   / 2,
                (a.getGreen() + b.getGreen()) / 2,
                (a.getBlue()  + b.getBlue())  / 2
        );
    }

    /**
     * Cari pasangan shape yang irisannya mengandung titik (px, py).
     * Kembalikan array [shapeA, shapeB] atau null kalau tidak ada.
     */
    private ShapeObject[] findIntersectionAt(int px, int py) {
        List<ShapeObject> all = shapeManager.getShapes();
        for (int i = 0; i < all.size(); i++) {
            for (int j = i + 1; j < all.size(); j++) {
                ShapeObject a = all.get(i);
                ShapeObject b = all.get(j);
                if (a.getType() == ToolType.LINE || b.getType() == ToolType.LINE) continue;
                if (a instanceof GroupObject || b instanceof GroupObject) continue;

                Area areaA = new Area(a.getTransformedShape());
                Area areaB = new Area(b.getTransformedShape());
                areaA.intersect(areaB);

                if (!areaA.isEmpty() && areaA.contains(px, py)) {
                    return new ShapeObject[]{a, b};
                }
            }
        }
        return null;
    }

    private void drawRubberBand(Graphics2D g2d) {
        int x = Math.min(dragStartPoint.x, currentDragPoint.x);
        int y = Math.min(dragStartPoint.y, currentDragPoint.y);
        int w = Math.abs(currentDragPoint.x - dragStartPoint.x);
        int h = Math.abs(currentDragPoint.y - dragStartPoint.y);

        g2d.setColor(new Color(55, 142, 219, 35));
        g2d.fillRect(x, y, w, h);
        g2d.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                10, new float[]{4, 3}, 0));
        g2d.setColor(SELECTION_BLUE);
        g2d.drawRect(x, y, w, h);
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
        tempShape.setFillColor(currentFillColor);
        tempShape.setFillSecondaryColor(currentFillSecondaryColor);
        tempShape.setFillEnabled(currentFillEnabled);
        tempShape.setGradientFill(currentGradientFill);
        tempShape.setStrokeColor(currentStrokeColor);
        tempShape.setStrokeWidth(currentStrokeWidth);
        tempShape.setLineStyle(currentLineStyle);

        Shape shape = createDrawableShape(tempShape);

        AffineTransform originalTransform = g2d.getTransform();
        AffineTransform previewTx = new AffineTransform(originalTransform);
        previewTx.translate(x, y);
        g2d.setTransform(previewTx);

        if (tempShape.getType() != ToolType.LINE && tempShape.isFillEnabled()) {
            if (tempShape.isGradientFill()) {
                g2d.setPaint(new GradientPaint(0, 0, tempShape.getFillColor(),
                        Math.max(1, tempShape.getWidth()), Math.max(1, tempShape.getHeight()),
                        tempShape.getFillSecondaryColor()));
            } else {
                g2d.setColor(tempShape.getFillColor());
            }
            g2d.fill(shape);
        }

        g2d.setStroke(createStroke(tempShape.getStrokeWidth(), tempShape.getLineStyle()));
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
            case PARALLELOGRAM:
                Polygon parallelogram = new Polygon();
                int offset = Math.max(1, Math.abs(width) / 4);
                parallelogram.addPoint(offset, 0);
                parallelogram.addPoint(width, 0);
                parallelogram.addPoint(width - offset, height);
                parallelogram.addPoint(0, height);
                return parallelogram;
            case STAR:
                return createStar(width, height);
            case LINE:
                return new Line2D.Double(0, 0, width, height);
            default:
                return new Rectangle(0, 0, width, height);
        }
    }

    private Shape createStar(int width, int height) {
        Polygon star = new Polygon();
        double centerX = width / 2.0;
        double centerY = height / 2.0;
        double outerRadius = Math.min(Math.abs(width), Math.abs(height)) / 2.0;
        double innerRadius = outerRadius * 0.45;

        for (int i = 0; i < 10; i++) {
            double angle = -Math.PI / 2.0 + i * Math.PI / 5.0;
            double radius = (i % 2 == 0) ? outerRadius : innerRadius;
            int px = (int) Math.round(centerX + Math.cos(angle) * radius);
            int py = (int) Math.round(centerY + Math.sin(angle) * radius);
            star.addPoint(px, py);
        }
        return star;
    }

    private void setupMouseHandlers() {
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                Point mousePoint = e.getPoint();
                if (!isInCanvas(mousePoint)) {
                    if (currentTool == ToolType.SELECT) {
                        shapeManager.clearSelection();
                        notifySelectionChanged();
                        repaint();
                    }
                    dragStartPoint = null;
                    lastDragPoint = null;
                    currentDragPoint = null;
                    return;
                }

                dragStartPoint = mousePoint;
                lastDragPoint = mousePoint;
                currentDragPoint = mousePoint;

                if (currentTool == ToolType.SELECT) {
                    boolean ctrl = (e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0;
                    ShapeObject clickedShape = shapeManager.findShapeAt(e.getX(), e.getY());

                    if (clickedShape != null) {
                        draggingSelectedShape = true;
                        rubberBanding = false;

                        if (ctrl) {
                            shapeManager.toggleShapeSelection(clickedShape);
                        } else {
                            if (!clickedShape.isSelected()) {
                                shapeManager.selectShape(clickedShape);
                            }
                        }
                        notifySelectionChanged();
                        repaint();
                    } else {
                        // Cek apakah klik tepat di area irisan
                        ShapeObject[] pair = findIntersectionAt(e.getX(), e.getY());
                        if (pair != null && !ctrl) {
                            // Klik di irisan: munculkan color chooser
                            Color current = shapeManager.getIntersectionColor(pair[0], pair[1]);
                            if (current == null) current = blendColors(pair[0].getFillColor(), pair[1].getFillColor());
                            Color chosen = JColorChooser.showDialog(
                                    DrawingPanel.this, "Warna Area Irisan", current);
                            if (chosen != null) {
                                shapeManager.setIntersectionColor(pair[0], pair[1], chosen);
                                repaint();
                            }
                            dragStartPoint = null;
                            return;
                        }
                        // Klik di area kosong
                        draggingSelectedShape = false;
                        if (!ctrl) {
                            shapeManager.clearSelection();
                            notifySelectionChanged();
                        }
                        rubberBanding = true;
                        repaint();
                    }
                } else {
                    draggingSelectedShape = false;
                    rubberBanding = false;
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragStartPoint == null) {
                    return;
                }
                currentDragPoint = clampToCanvas(e.getPoint());

                if (currentTool == ToolType.SELECT) {
                    if (draggingSelectedShape) {
                        // Gerakkan semua shape yang terpilih sekaligus
                        int deltaX = currentDragPoint.x - lastDragPoint.x;
                        int deltaY = currentDragPoint.y - lastDragPoint.y;

                        List<ShapeObject> selected = shapeManager.getSelectedShapes();
                        for (ShapeObject s : selected) {
                            s.moveBy(deltaX, deltaY);
                        }
                        lastDragPoint = currentDragPoint;
                        notifySelectionChanged();
                    }
                    // Rubber-band: update seleksi real-time
                    if (rubberBanding) {
                        int rx = dragStartPoint.x;
                        int ry = dragStartPoint.y;
                        int rw = currentDragPoint.x - rx;
                        int rh = currentDragPoint.y - ry;
                        shapeManager.selectShapesInRect(rx, ry, rw, rh);
                        notifySelectionChanged();
                    }
                }
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                Point releasePoint = clampToCanvas(e.getPoint());
                if (currentTool == ToolType.SELECT) {
                    draggingSelectedShape = false;
                    rubberBanding = false;
                    dragStartPoint = null;
                    lastDragPoint = null;
                    currentDragPoint = null;
                    return;
                }

                createShapeFromDrag(releasePoint);
                dragStartPoint = null;
                lastDragPoint = null;
                currentDragPoint = null;
                rubberBanding = false;
                repaint();
            }
        };

        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
    }

    private void setupKeyboardHandlers() {
        // Delete: hapus semua shape yang terpilih
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(
                javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "deleteShape");
        getActionMap().put("deleteShape", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                List<ShapeObject> selected = shapeManager.getSelectedShapes();
                if (!selected.isEmpty()) {
                    // Bungkus semua delete dalam satu CompoundCommand agar bisa di-undo sekaligus
                    historyManager.executeCommand(new DeleteMultiCommand(shapeManager, selected));
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

        // Bungkus dalam AddShapeCommand agar bisa di-undo
        ShapeObject createdShape = shapeManager.buildShape(
                currentTool, x, y, width, height,
                currentFillColor, currentStrokeColor
        );
        createdShape.setFillEnabled(currentFillEnabled);
        createdShape.setGradientFill(currentGradientFill);
        createdShape.setFillSecondaryColor(currentFillSecondaryColor);
        createdShape.setStrokeWidth(currentStrokeWidth);
        createdShape.setLineStyle(currentLineStyle);
        historyManager.executeCommand(new AddShapeCommand(shapeManager, createdShape));

        selectionListener.accept(createdShape);
        repaint();
    }

    private void notifySelectionChanged() {
        selectionListener.accept(shapeManager.getSelectedShape());
    }

    private BasicStroke createStroke(float width, LineStyle style) {
        float strokeWidth = Math.max(1.0f, width);
        float[] dash = null;
        if (style == LineStyle.DASHED) {
            dash = new float[]{12.0f, 8.0f};
        } else if (style == LineStyle.DOTTED) {
            dash = new float[]{2.0f, 6.0f};
        }
        if (dash == null) {
            return new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        }
        return new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                10.0f, dash, 0.0f);
    }
}
