import javax.swing.JPanel;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
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
import java.util.List;
import java.util.function.Consumer;
import javax.swing.JColorChooser;

public class DrawingPanel extends JPanel {

    private final ShapeManager shapeManager;
    private final Consumer<ShapeObject> selectionListener;

    // -------------------------------------------------------------------------
    // Sistem Undo/Redo - Ditambahkan oleh Anggota 4
    // HistoryManager mengelola undoStack dan redoStack via Command Pattern
    // -------------------------------------------------------------------------
    private final HistoryManager historyManager;

    private ToolType currentTool;
    private Color currentFillColor;
    private Color currentStrokeColor;
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
        this.currentStrokeColor = Color.BLACK;
        this.draggingSelectedShape = false;

        // Inisialisasi HistoryManager - Anggota 4
        this.historyManager = new HistoryManager();

        setBackground(Color.WHITE);
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

        g2d.dispose();
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
            g2d.setStroke(new BasicStroke(2.5f));
            g2d.setColor(Color.RED);
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
            g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    10, new float[]{6, 4}, 0));
            g2d.setColor(shapeObject.getStrokeColor());
            g2d.draw(shape);

            if (shapeObject.isSelected()) {
                g2d.setStroke(new BasicStroke(2));
                g2d.setColor(Color.RED);
                g2d.draw(shape.getBounds2D());
            }
        }
        g2d.setTransform(originalTransform);
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
                    Color ic = (custom != null) ? custom : blendColors(a.getFillColor(), b.getFillColor());

                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
                    g2d.setColor(ic);
                    g2d.fill(areaA);
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
                    g2d.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                            10, new float[]{4, 3}, 0));
                    g2d.setColor(ic.darker());
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

        g2d.setColor(new Color(0, 100, 220, 30));
        g2d.fillRect(x, y, w, h);
        g2d.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                10, new float[]{4, 3}, 0));
        g2d.setColor(new Color(0, 100, 220));
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
        previewTx.translate(x, y);
        g2d.setTransform(previewTx);

        if (tempShape.getType() != ToolType.LINE) {
            g2d.setColor(tempShape.getFillColor());
            g2d.fill(shape);
        }

        g2d.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                10.0f, new float[]{5.0f, 5.0f}, 0.0f));
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
                currentDragPoint = e.getPoint();

                if (currentTool == ToolType.SELECT) {
                    if (draggingSelectedShape) {
                        // Gerakkan semua shape yang terpilih sekaligus
                        int deltaX = e.getX() - lastDragPoint.x;
                        int deltaY = e.getY() - lastDragPoint.y;

                        List<ShapeObject> selected = shapeManager.getSelectedShapes();
                        for (ShapeObject s : selected) {
                            s.moveBy(deltaX, deltaY);
                        }
                        lastDragPoint = e.getPoint();
                        notifySelectionChanged();
                    }
                    // Rubber-band: update seleksi real-time
                    if (rubberBanding) {
                        int rx = dragStartPoint.x;
                        int ry = dragStartPoint.y;
                        int rw = e.getX() - rx;
                        int rh = e.getY() - ry;
                        shapeManager.selectShapesInRect(rx, ry, rw, rh);
                        notifySelectionChanged();
                    }
                }
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (currentTool == ToolType.SELECT) {
                    draggingSelectedShape = false;
                    rubberBanding = false;
                    dragStartPoint = null;
                    lastDragPoint = null;
                    currentDragPoint = null;
                    return;
                }

                createShapeFromDrag(e.getPoint());
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
        historyManager.executeCommand(new AddShapeCommand(shapeManager, createdShape));

        selectionListener.accept(createdShape);
        repaint();
    }

    private void notifySelectionChanged() {
        selectionListener.accept(shapeManager.getSelectedShape());
    }
}
