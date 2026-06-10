import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Polygon;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.UIManager;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.filechooser.FileNameExtensionFilter;

public class MainFrame extends JFrame {

    private static final Color APP_BG = new Color(28, 31, 34);
    private static final Color PANEL_BG = new Color(42, 45, 49);
    private static final Color TOOL_BG = new Color(35, 39, 43);
    private static final Color TOOL_SELECTED = new Color(47, 63, 78);
    private static final Color BORDER = new Color(64, 70, 76);
    private static final Color TEXT = new Color(235, 238, 241);
    private static final Color ACCENT = new Color(55, 142, 219);

    private final ShapeManager shapeManager;
    private final DrawingPanel drawingPanel;
    private final PropertyPanel propertyPanel;
    private final StylePanel stylePanel;
    private final JLabel statusLabel;
    private JButton animStatusButton;

    public MainFrame() {
        installDarkTheme();

        shapeManager = new ShapeManager();
        drawingPanel = new DrawingPanel(shapeManager, this::onSelectionChanged);
        propertyPanel = new PropertyPanel(() -> drawingPanel.repaint());
        stylePanel = new StylePanel(shapeManager, drawingPanel,
                () -> {
                    drawingPanel.repaint();
                    onSelectionChanged(shapeManager.getSelectedShape());
                });
        statusLabel = new JLabel("Ready");
        statusLabel.setForeground(TEXT);

        setTitle("VectorFlow - Grafika Komputer");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(APP_BG);

        setJMenuBar(createMenuBar());
        add(createToolBar(), BorderLayout.WEST);

        // Bungkus drawing panel + status bar dalam satu panel tengah
        // agar status bar tidak memanjang ke bawah panel kanan
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(APP_BG);
        centerPanel.add(drawingPanel, BorderLayout.CENTER);
        centerPanel.add(createStatusBar(), BorderLayout.SOUTH);

        add(centerPanel, BorderLayout.CENTER);
        add(createRightPanel(), BorderLayout.EAST);

        setVisible(true);
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(PANEL_BG);
        menuBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(22, 24, 27)));
        menuBar.add(createFileMenu());
        menuBar.add(createEditMenu());
        menuBar.add(createTransformMenu());
        menuBar.add(createStyleMenu());
        menuBar.add(createAnimationMenu());
        return menuBar;
    }

    // =========================================================================
    // File Menu
    // =========================================================================

    private JMenu createFileMenu() {
        JMenu fileMenu = new JMenu("File");

        JMenuItem newItem = new JMenuItem("New");
        newItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(
                java.awt.event.KeyEvent.VK_N,
                java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        newItem.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Buat kanvas baru? Semua shape akan dihapus.",
                    "New Canvas",
                    JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                shapeManager.clearAll();
                drawingPanel.getHistoryManager().clearHistory();
                drawingPanel.repaint();
                onSelectionChanged(null);
                statusLabel.setText("Kanvas baru dibuat");
            }
        });

        JMenuItem saveItem = new JMenuItem("Save as PNG");
        saveItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(
                java.awt.event.KeyEvent.VK_S,
                java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        saveItem.addActionListener(e -> saveCanvasAsPng());

        fileMenu.add(newItem);
        fileMenu.addSeparator();
        fileMenu.add(saveItem);
        return fileMenu;
    }

    private void saveCanvasAsPng() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Simpan sebagai PNG");
        chooser.setFileFilter(new FileNameExtensionFilter("PNG Image", "png"));
        chooser.setSelectedFile(new File("kanvas.png"));

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".png")) {
            file = new File(file.getAbsolutePath() + ".png");
        }

        // Render DrawingPanel ke BufferedImage
        BufferedImage image = new BufferedImage(
                drawingPanel.getWidth(),
                drawingPanel.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        drawingPanel.paint(image.createGraphics());

        try {
            ImageIO.write(image, "png", file);
            statusLabel.setText("Tersimpan: " + file.getName());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Gagal menyimpan: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =========================================================================
    // Edit Menu  (Undo/Redo + Group/Ungroup)
    // Undo/Redo ditambahkan oleh Anggota 4
    // Group/Ungroup ditambahkan oleh Anggota 4
    // =========================================================================

    private JMenu createEditMenu() {
        JMenu editMenu = new JMenu("Edit");

        // --- Undo ---
        JMenuItem undoItem = new JMenuItem("Undo");
        undoItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(
                java.awt.event.KeyEvent.VK_Z,
                java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        undoItem.addActionListener(e -> {
            HistoryManager history = drawingPanel.getHistoryManager();
            if (history.undo()) {
                drawingPanel.repaint();
                onSelectionChanged(shapeManager.getSelectedShape());
                statusLabel.setText("Undo berhasil");
            } else {
                statusLabel.setText("Tidak ada aksi yang bisa di-undo");
            }
        });

        // --- Redo ---
        JMenuItem redoItem = new JMenuItem("Redo");
        redoItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(
                java.awt.event.KeyEvent.VK_Y,
                java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        redoItem.addActionListener(e -> {
            HistoryManager history = drawingPanel.getHistoryManager();
            if (history.redo()) {
                drawingPanel.repaint();
                onSelectionChanged(shapeManager.getSelectedShape());
                statusLabel.setText("Redo berhasil");
            } else {
                statusLabel.setText("Tidak ada aksi yang bisa di-redo");
            }
        });

        editMenu.add(undoItem);
        editMenu.add(redoItem);
        editMenu.addSeparator();

        // --- Select All ---
        JMenuItem selectAllItem = new JMenuItem("Select All");
        selectAllItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(
                java.awt.event.KeyEvent.VK_A,
                java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        selectAllItem.addActionListener(e -> {
            for (ShapeObject s : shapeManager.getShapes()) {
                s.setSelected(true);
            }
            drawingPanel.repaint();
            statusLabel.setText("Semua shape dipilih");
        });

        // --- Group ---
        JMenuItem groupItem = new JMenuItem("Group");
        groupItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(
                java.awt.event.KeyEvent.VK_G,
                java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        groupItem.addActionListener(e -> groupSelected());

        // --- Ungroup ---
        JMenuItem ungroupItem = new JMenuItem("Ungroup");
        ungroupItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(
                java.awt.event.KeyEvent.VK_G,
                java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()
                        | java.awt.event.InputEvent.SHIFT_DOWN_MASK));
        ungroupItem.addActionListener(e -> ungroupSelected());

        // --- Intersection Color ---
        JMenuItem intersectColorItem = new JMenuItem("Set Intersection Color...");
        intersectColorItem.addActionListener(e -> setIntersectionColor());

        editMenu.add(selectAllItem);
        editMenu.addSeparator();
        editMenu.add(groupItem);
        editMenu.add(ungroupItem);
        editMenu.addSeparator();
        editMenu.add(intersectColorItem);

        return editMenu;
    }

    /** Group semua shape yang terpilih menjadi satu GroupObject. */
    private void groupSelected() {
        List<ShapeObject> selected = shapeManager.getSelectedShapes();
        // Keluarkan GroupObject yang sudah ada dari kandidat (tidak boleh group-in-group)
        selected.removeIf(s -> s instanceof GroupObject);

        if (selected.size() < 2) {
            statusLabel.setText("Pilih minimal 2 shape untuk di-group (Ctrl+Click atau rubber-band)");
            return;
        }
        GroupShapeCommand cmd = new GroupShapeCommand(shapeManager, selected);
        drawingPanel.getHistoryManager().executeCommand(cmd);
        drawingPanel.repaint();
        onSelectionChanged(cmd.getGroup());
        statusLabel.setText("Group dibuat (" + selected.size() + " shape)");
    }

    /** Ungroup GroupObject yang terpilih. */
    private void ungroupSelected() {
        ShapeObject sel = shapeManager.getSelectedShape();
        if (!(sel instanceof GroupObject)) {
            statusLabel.setText("Pilih sebuah group untuk di-ungroup");
            return;
        }
        UngroupShapeCommand cmd = new UngroupShapeCommand(shapeManager, (GroupObject) sel);
        drawingPanel.getHistoryManager().executeCommand(cmd);
        drawingPanel.repaint();
        onSelectionChanged(shapeManager.getSelectedShape());
        statusLabel.setText("Ungroup selesai");
    }

    /**
     * Set warna irisan: pilih dua shape (Ctrl+Click), lalu gunakan menu ini.
     * Warna yang dipilih disimpan di fill color shape pertama yang dipilih
     * sebagai sinyal warna irisan — DrawingPanel akan mencampur keduanya.
     *
     * Karena irisan dua shape diwarnai berdasarkan rata-rata fill color keduanya,
     * cara paling natural adalah: ubah fill color salah satu shape melalui dialog
     * warna, hasilnya langsung terlihat di irisan.
     */
    private void setIntersectionColor() {
        List<ShapeObject> selected = shapeManager.getSelectedShapes();
        if (selected.size() < 2) {
            statusLabel.setText("Pilih 2 shape yang beririsan terlebih dahulu (Ctrl+Click)");
            return;
        }

        Color initialColor = getIntersectionPreviewColor(selected.get(0), selected.get(1));
        Color chosen = JColorChooser.showDialog(this, "Warna Area Irisan", initialColor);
        if (chosen == null) return;

        // Distribusikan warna ke kedua shape sehingga rata-ratanya = chosen
        // Cara sederhana: set kedua shape ke warna tersebut
        selected.get(0).setFillColor(chosen);
        selected.get(1).setFillColor(chosen);
        drawingPanel.repaint();
        statusLabel.setText("Warna irisan diperbarui");
    }

    private Color getIntersectionPreviewColor(ShapeObject a, ShapeObject b) {
        Color ca = a.getFillColor();
        Color cb = b.getFillColor();
        return new Color(
                (ca.getRed() + cb.getRed()) / 2,
                (ca.getGreen() + cb.getGreen()) / 2,
                (ca.getBlue() + cb.getBlue()) / 2);
    }

    // =========================================================================
    // Transform Menu — bekerja pada SEMUA shape yang terpilih
    // =========================================================================

    private JMenu createTransformMenu() {
    JMenu transformMenu = new JMenu("Transform");

    JMenuItem rotateItem = new JMenuItem("Rotate +15°");
    rotateItem.addActionListener(e -> applyTransformToSelected(
            shape -> shape.setRotation(shape.getRotation() + 15)));

    JMenuItem rotateCCWItem = new JMenuItem("Rotate -15°");
    rotateCCWItem.addActionListener(e -> applyTransformToSelected(
            shape -> shape.setRotation(shape.getRotation() - 15)));

    JMenuItem scaleUpItem = new JMenuItem("Scale Up (+10%)");
    scaleUpItem.addActionListener(e -> applyTransformToSelected(shape -> {
        shape.setScaleX(shape.getScaleX() * 1.1);
        shape.setScaleY(shape.getScaleY() * 1.1);
    }));

    JMenuItem scaleDownItem = new JMenuItem("Scale Down (-10%)");
    scaleDownItem.addActionListener(e -> applyTransformToSelected(shape -> {
        shape.setScaleX(shape.getScaleX() / 1.1);
        shape.setScaleY(shape.getScaleY() / 1.1);
    }));

    // Reflection Horizontal - bayangan di samping kiri/kanan
    JMenuItem reflectHorizontalItem = new JMenuItem("Reflect Horizontal (Kiri/Kanan)");
    reflectHorizontalItem.addActionListener(e -> applyTransformToSelected(shape -> {
        shape.setReflected(true);
        shape.setReflectDirection(1);
        // TIDAK mengubah line style objek asli
    }));

    // Reflection Vertical - bayangan di bawah/atas
    JMenuItem reflectVerticalItem = new JMenuItem("Reflect Vertical (Atas/Bawah)");
    reflectVerticalItem.addActionListener(e -> applyTransformToSelected(shape -> {
        shape.setReflected(true);
        shape.setReflectDirection(2);
        // TIDAK mengubah line style objek asli
    }));

    // Turn off reflection
    JMenuItem reflectOffItem = new JMenuItem("Turn Off Reflection");
    reflectOffItem.addActionListener(e -> applyTransformToSelected(shape -> {
        shape.setReflected(false);
        shape.setReflectDirection(0);
        // Line style otomatis kembali ke original via setReflected()
    }));

    JMenuItem resetItem = new JMenuItem("Reset Transform");
    resetItem.addActionListener(e -> applyTransformToSelected(shape -> {
        shape.setRotation(0);
        shape.setScaleX(1.0);
        shape.setScaleY(1.0);
        shape.setSkewX(0);
        shape.setSkewY(0);
        shape.setReflected(false);
        shape.setReflectDirection(0);
    }));

    transformMenu.add(rotateItem);
    transformMenu.add(rotateCCWItem);
    transformMenu.addSeparator();
    transformMenu.add(scaleUpItem);
    transformMenu.add(scaleDownItem);
    transformMenu.addSeparator();
    transformMenu.add(reflectHorizontalItem);
    transformMenu.add(reflectVerticalItem);
    transformMenu.add(reflectOffItem);
    transformMenu.addSeparator();
    transformMenu.add(resetItem);
    return transformMenu;
}

    /**
     * Terapkan transformasi ke semua shape yang terpilih.
     * GroupObject: transformasi diterapkan ke setiap member.
     */
    private void applyTransformToSelected(java.util.function.Consumer<ShapeObject> action) {
        List<ShapeObject> selected = shapeManager.getSelectedShapes();
        if (selected.isEmpty()) {
            statusLabel.setText("Pilih shape terlebih dahulu");
            return;
        }
        for (ShapeObject s : selected) {
            if (s instanceof GroupObject) {
                for (ShapeObject member : ((GroupObject) s).getMembers()) {
                    action.accept(member);
                }
            } else {
                action.accept(s);
            }
        }
        drawingPanel.repaint();
        int count = selected.size();
        statusLabel.setText("Transform diterapkan ke " + count + " shape");
    }

    // =========================================================================
    // Style Menu
    // =========================================================================

    private JMenu createStyleMenu() {
        JMenu styleMenu = new JMenu("Style");

        JMenuItem fillColorItem = new JMenuItem("Fill Color");
        fillColorItem.addActionListener(e -> stylePanel.chooseFillColor());

        JMenuItem noFillItem = new JMenuItem("No Fill");
        noFillItem.addActionListener(e -> stylePanel.setFillEnabled(false));

        JMenuItem gradientStartItem = new JMenuItem("Gradient Start Color");
        gradientStartItem.addActionListener(e -> stylePanel.chooseFillColor());

        JMenuItem gradientEndItem = new JMenuItem("Gradient End Color");
        gradientEndItem.addActionListener(e -> stylePanel.chooseFillSecondaryColor());

        JMenuItem gradientOnItem = new JMenuItem("Enable Gradient");
        gradientOnItem.addActionListener(e -> stylePanel.setGradientEnabled(true));

        JMenuItem gradientOffItem = new JMenuItem("Disable Gradient");
        gradientOffItem.addActionListener(e -> stylePanel.setGradientEnabled(false));

        JMenuItem strokeColorItem = new JMenuItem("Stroke Color");
        strokeColorItem.addActionListener(e -> stylePanel.chooseStrokeColor());

        JMenuItem strokeWidthItem = new JMenuItem("Stroke Width...");
        strokeWidthItem.addActionListener(e -> promptStrokeWidth());

        JMenu lineStyleMenu = new JMenu("Line Style");
        JMenuItem solidItem = new JMenuItem("Solid");
        solidItem.addActionListener(e -> stylePanel.setLineStyle(LineStyle.SOLID));
        JMenuItem dashedItem = new JMenuItem("Dashed");
        dashedItem.addActionListener(e -> stylePanel.setLineStyle(LineStyle.DASHED));
        JMenuItem dottedItem = new JMenuItem("Dotted");
        dottedItem.addActionListener(e -> stylePanel.setLineStyle(LineStyle.DOTTED));
        lineStyleMenu.add(solidItem);
        lineStyleMenu.add(dashedItem);
        lineStyleMenu.add(dottedItem);

        styleMenu.add(fillColorItem);
        styleMenu.add(noFillItem);
        styleMenu.addSeparator();
        styleMenu.add(gradientStartItem);
        styleMenu.add(gradientEndItem);
        styleMenu.add(gradientOnItem);
        styleMenu.add(gradientOffItem);
        styleMenu.addSeparator();
        styleMenu.add(strokeColorItem);
        styleMenu.add(strokeWidthItem);
        styleMenu.add(lineStyleMenu);
        return styleMenu;
    }

    // =========================================================================
    // Toolbar
    // =========================================================================

    private javax.swing.JScrollPane createToolBar() {
        JPanel toolBar = new JPanel();
        toolBar.setLayout(new BoxLayout(toolBar, BoxLayout.Y_AXIS));
        toolBar.setBackground(TOOL_BG);
        toolBar.setBorder(BorderFactory.createEmptyBorder(10, 6, 10, 6));

        ButtonGroup toolGroup = new ButtonGroup();
        addToolButton(toolBar, toolGroup, ToolType.SELECT, true);
        addToolButton(toolBar, toolGroup, ToolType.RECTANGLE, false);
        addToolButton(toolBar, toolGroup, ToolType.CIRCLE, false);
        addToolButton(toolBar, toolGroup, ToolType.TRIANGLE, false);
        addToolButton(toolBar, toolGroup, ToolType.PARALLELOGRAM, false);
        addToolButton(toolBar, toolGroup, ToolType.STAR, false);
        addToolButton(toolBar, toolGroup, ToolType.LINE, false);

        javax.swing.JScrollPane sp = new javax.swing.JScrollPane(toolBar);
        sp.setBorder(null);
        sp.setBackground(TOOL_BG);
        sp.getViewport().setBackground(TOOL_BG);
        sp.setHorizontalScrollBarPolicy(javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setVerticalScrollBarPolicy(javax.swing.JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        sp.getVerticalScrollBar().setUnitIncrement(12);
        sp.setPreferredSize(new Dimension(74, 0));

        javax.swing.JScrollBar vsb = sp.getVerticalScrollBar();
        vsb.setPreferredSize(new Dimension(0, 0)); // sembunyikan scrollbar tapi tetap bisa scroll
        vsb.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            private final Color TRACK = TOOL_BG;
            private final Color THUMB = new Color(70, 76, 83);
            private final Color THUMB_HOVER = new Color(95, 102, 110);

            @Override protected void configureScrollBarColors() {
                thumbColor = THUMB;
                trackColor = TRACK;
            }
            @Override protected JButton createDecreaseButton(int orientation) { return zeroBtn(); }
            @Override protected JButton createIncreaseButton(int orientation) { return zeroBtn(); }
            private JButton zeroBtn() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                b.setMinimumSize(new Dimension(0, 0));
                b.setMaximumSize(new Dimension(0, 0));
                return b;
            }
            @Override public void paintTrack(Graphics g, javax.swing.JComponent c, Rectangle trackBounds) {
                g.setColor(TRACK);
                g.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
            }
            @Override public void paintThumb(Graphics g, javax.swing.JComponent c, Rectangle thumbBounds) {
                if (thumbBounds.isEmpty()) return;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isDragging ? THUMB_HOVER : THUMB);
                g2.fillRoundRect(thumbBounds.x + 1, thumbBounds.y + 2,
                        thumbBounds.width - 2, thumbBounds.height - 4, 6, 6);
                g2.dispose();
            }
        });

        return sp;
    }

    private JPanel createRightPanel() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(PANEL_BG);

        propertyPanel.setAlignmentX(LEFT_ALIGNMENT);
        stylePanel.setAlignmentX(LEFT_ALIGNMENT);
        content.add(propertyPanel);
        content.add(stylePanel);

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        scrollPane.setBackground(PANEL_BG);
        scrollPane.getViewport().setBackground(PANEL_BG);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(12);

        // Dark scrollbar styling
        javax.swing.JScrollBar vsb = scrollPane.getVerticalScrollBar();
        vsb.setBackground(new Color(22, 24, 27));
        vsb.setForeground(new Color(22, 24, 27));
        vsb.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            private final Color TRACK = new Color(22, 24, 27);
            private final Color THUMB = new Color(70, 76, 83);
            private final Color THUMB_HOVER = new Color(95, 102, 110);

            @Override protected void configureScrollBarColors() {
                thumbColor = THUMB;
                trackColor = TRACK;
            }
            @Override protected JButton createDecreaseButton(int orientation) {
                return createZeroButton();
            }
            @Override protected JButton createIncreaseButton(int orientation) {
                return createZeroButton();
            }
            private JButton createZeroButton() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                b.setMinimumSize(new Dimension(0, 0));
                b.setMaximumSize(new Dimension(0, 0));
                return b;
            }
            @Override public void paintTrack(Graphics g, javax.swing.JComponent c, Rectangle trackBounds) {
                g.setColor(TRACK);
                g.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
            }
            @Override public void paintThumb(Graphics g, javax.swing.JComponent c, Rectangle thumbBounds) {
                if (thumbBounds.isEmpty()) return;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isDragging ? THUMB_HOVER : THUMB);
                g2.fillRoundRect(thumbBounds.x + 2, thumbBounds.y + 2,
                        thumbBounds.width - 4, thumbBounds.height - 4, 6, 6);
                g2.dispose();
            }
        });

        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(260, 100));
        panel.setMinimumSize(new Dimension(260, 100));
        panel.setBackground(PANEL_BG);
        panel.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(21, 24, 27)));
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private void addToolButton(JPanel toolBar, ButtonGroup toolGroup, ToolType toolType, boolean selected) {
        JToggleButton button = new ToolButton(toolType);
        button.setSelected(selected);
        button.addActionListener(e -> {
            drawingPanel.setCurrentTool(toolType);
            statusLabel.setText("Tool: " + toolType.getDisplayName());
        });

        toolGroup.add(button);
        toolBar.add(button);
        toolBar.add(Box.createRigidArea(new Dimension(0, 8)));
    }

    // =========================================================================
    // Color choosers — bekerja untuk single dan multi-selection
    // =========================================================================

    // =========================================================================
    // Status bar & selection callback
    // =========================================================================

    private JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(PANEL_BG);
        statusBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(24, 27, 30)));
        statusBar.add(statusLabel, BorderLayout.WEST);

        JPanel actions = new JPanel(new GridLayout(1, 4, 8, 0));
        actions.setOpaque(false);
        actions.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 10));
        actions.add(createStatusButton("Undo", "undo", () -> {
            HistoryManager history = drawingPanel.getHistoryManager();
            if (history.undo()) {
                drawingPanel.repaint();
                onSelectionChanged(shapeManager.getSelectedShape());
                statusLabel.setText("Undo berhasil");
            } else {
                statusLabel.setText("Tidak ada aksi yang bisa di-undo");
            }
        }));
        actions.add(createStatusButton("Redo", "redo", () -> {
            HistoryManager history = drawingPanel.getHistoryManager();
            if (history.redo()) {
                drawingPanel.repaint();
                onSelectionChanged(shapeManager.getSelectedShape());
                statusLabel.setText("Redo berhasil");
            } else {
                statusLabel.setText("Tidak ada aksi yang bisa di-redo");
            }
        }));
        actions.add(createStatusButton("Clear Canvas", "clear", () -> {
            shapeManager.clearAll();
            drawingPanel.getHistoryManager().clearHistory();
            drawingPanel.repaint();
            onSelectionChanged(null);
            statusLabel.setText("Kanvas dibersihkan");
        }));

        final JButton[] animBtnHolder = new JButton[1];
        animBtnHolder[0] = createStatusButton("Play", "play", () -> {
            toggleAnimationState(animBtnHolder[0]);
        });
        animStatusButton = animBtnHolder[0];
        actions.add(animStatusButton);

        statusBar.add(actions, BorderLayout.EAST);
        return statusBar;
    }

    private void onSelectionChanged(ShapeObject selectedShape) {
        List<ShapeObject> selected = shapeManager.getSelectedShapes();

        if (selected.isEmpty()) {
            propertyPanel.showShape(null);
            stylePanel.showShape(null);
            statusLabel.setText("No object selected");
            return;
        }

        ShapeObject s = selected.get(0);
        stylePanel.showShape(s);

        if (selected.size() == 1) {
            propertyPanel.showShape(s);
            String label = (s instanceof GroupObject)
                    ? "Group (" + ((GroupObject) s).getMembers().size() + " shapes)"
                    : s.getType().getDisplayName() + " #" + s.getId();
            statusLabel.setText("Selected: " + label);
        } else {
            // Multi-selection: tampilkan info jumlah
            propertyPanel.showShape(null);
            statusLabel.setText("Selected: " + selected.size() + " shapes");
        }
    }

    private void promptStrokeWidth() {
        String input = JOptionPane.showInputDialog(
                this,
                "Masukkan stroke width (1.0 - 30.0):",
                String.valueOf(drawingPanel.getCurrentStrokeWidth()));
        if (input == null) {
            return;
        }

        try {
            float width = Float.parseFloat(input.trim());
            width = Math.max(1.0f, Math.min(30.0f, width));
            stylePanel.setStrokeWidth(width);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Stroke width harus berupa angka.",
                    "Input tidak valid",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    private void installDarkTheme() {
        UIManager.put("Panel.background", PANEL_BG);
        UIManager.put("OptionPane.background", PANEL_BG);
        UIManager.put("OptionPane.messageForeground", TEXT);
        UIManager.put("MenuBar.background", PANEL_BG);
        UIManager.put("Menu.background", PANEL_BG);
        UIManager.put("Menu.foreground", TEXT);
        UIManager.put("MenuItem.background", PANEL_BG);
        UIManager.put("MenuItem.foreground", TEXT);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("TabbedPane.background", PANEL_BG);
        UIManager.put("TabbedPane.foreground", TEXT);
        UIManager.put("TabbedPane.selected", TOOL_BG);
        UIManager.put("Button.background", new Color(52, 58, 64));
        UIManager.put("Button.foreground", TEXT);
        UIManager.put("ToggleButton.background", TOOL_BG);
        UIManager.put("ToggleButton.foreground", TEXT);
        UIManager.put("CheckBox.background", PANEL_BG);
        UIManager.put("CheckBox.foreground", TEXT);
        UIManager.put("ComboBox.background", new Color(31, 34, 38));
        UIManager.put("ComboBox.foreground", TEXT);
        UIManager.put("Spinner.background", new Color(31, 34, 38));
        UIManager.put("Spinner.foreground", TEXT);
        UIManager.put("ScrollPane.background", PANEL_BG);
        UIManager.put("Viewport.background", PANEL_BG);
    }

    private JButton createStatusButton(String text, Runnable action) {
        return createStatusButton(text, null, action);
    }

    private JButton createStatusButton(String text, String iconType, Runnable action) {
        JButton button = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Background
                Color bg = getModel().isPressed()
                        ? new Color(38, 43, 49)
                        : getModel().isRollover()
                        ? new Color(60, 66, 74)
                        : new Color(49, 54, 60);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);

                int iconSize = 14;
                int textGap = 6;
                FontMetrics fm = g2.getFontMetrics(getFont());
                int textW = fm.stringWidth(getText());
                String currentIcon = (String) getClientProperty("iconType");
                int totalW = (currentIcon != null ? iconSize + textGap : 0) + textW;
                int startX = (getWidth() - totalW) / 2;
                int cy = getHeight() / 2;

                // Icon
                if (currentIcon != null) {
                    g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.setColor(isEnabled() ? TEXT : new Color(100, 106, 113));
                    int ix = startX;
                    int iy = cy - iconSize / 2;
                    paintStatusIcon(g2, currentIcon, ix, iy, iconSize);
                    startX += iconSize + textGap;
                }

                // Text
                g2.setFont(getFont());
                g2.setColor(isEnabled() ? TEXT : new Color(100, 106, 113));
                g2.drawString(getText(), startX, cy + fm.getAscent() / 2 - 1);
                g2.dispose();
            }
        };
        button.putClientProperty("iconType", iconType);
        button.setText(text);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setOpaque(false);
        button.setForeground(TEXT);
        button.setBackground(new Color(49, 54, 60));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(5, 12, 5, 12)));
        button.addActionListener(e -> {
            if (action != null) {
                action.run();
            }
        });
        return button;
    }

    private void paintStatusIcon(Graphics2D g2, String type, int x, int y, int size) {
        switch (type) {
            case "undo": {
                // Path: C3 10 C3 6 6 3 10 3 L13 3  (hook bawah, ekor kanan)
                g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                java.awt.geom.GeneralPath path = new java.awt.geom.GeneralPath();
                double s = size / 16.0;
                path.moveTo(x + 3*s, y + 10*s);
                path.curveTo(x + 3*s, y + 6*s, x + 6*s, y + 3*s, x + 10*s, y + 3*s);
                path.lineTo(x + 13*s, y + 3*s);
                g2.draw(path);
                // Kepala panah di (3,6), (3,10), (7,10)
                java.awt.geom.GeneralPath arrow = new java.awt.geom.GeneralPath();
                arrow.moveTo(x + 3*s,  y + 6*s);
                arrow.lineTo(x + 3*s,  y + 10*s);
                arrow.lineTo(x + 7*s,  y + 10*s);
                g2.draw(arrow);
                break;
            }
            case "redo": {
                // Mirror: hook bawah, ekor kiri
                g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                double s = size / 16.0;
                java.awt.geom.GeneralPath path = new java.awt.geom.GeneralPath();
                path.moveTo(x + 13*s, y + 10*s);
                path.curveTo(x + 13*s, y + 6*s, x + 10*s, y + 3*s, x + 6*s, y + 3*s);
                path.lineTo(x + 3*s, y + 3*s);
                g2.draw(path);
                // Kepala panah di (13,6), (13,10), (9,10)
                java.awt.geom.GeneralPath arrow = new java.awt.geom.GeneralPath();
                arrow.moveTo(x + 13*s, y + 6*s);
                arrow.lineTo(x + 13*s, y + 10*s);
                arrow.lineTo(x + 9*s,  y + 10*s);
                g2.draw(arrow);
                break;
            }
            case "clear": {
                // Ikon tempat sampah
                g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int tw = size - 4;
                int tx = x + 2;
                // Badan
                g2.drawRoundRect(tx + 1, y + 4, tw - 2, size - 6, 2, 2);
                // Tutup
                g2.drawLine(tx, y + 4, tx + tw, y + 4);
                g2.drawLine(tx + 3, y + 2, tx + tw - 3, y + 2);
                // Garis dalam
                g2.drawLine(tx + 3, y + 6, tx + 3, y + size - 4);
                g2.drawLine(tx + tw / 2, y + 6, tx + tw / 2, y + size - 4);
                g2.drawLine(tx + tw - 3, y + 6, tx + tw - 3, y + size - 4);
                break;
            }
            case "play": {
                g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                double s = size / 16.0;
                Polygon p = new Polygon();
                p.addPoint((int)(x + 4*s), (int)(y + 2*s));
                p.addPoint((int)(x + 13*s), (int)(y + 8*s));
                p.addPoint((int)(x + 4*s), (int)(y + 14*s));
                g2.drawPolygon(p);
                break;
            }
            case "stop": {
                g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                double s = size / 16.0;
                g2.drawRect((int)(x + 3*s), (int)(y + 3*s), (int)(10*s), (int)(10*s));
                break;
            }
        }
    }

    private final class ToolButton extends JToggleButton {
        private final ToolType toolType;

        private ToolButton(ToolType toolType) {
            this.toolType = toolType;
            setPreferredSize(new Dimension(64, 54));
            setMinimumSize(new Dimension(64, 54));
            setMaximumSize(new Dimension(64, 54));
            setAlignmentX(LEFT_ALIGNMENT);
            setToolTipText(toolType.getDisplayName());
            setText("");
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorder(BorderFactory.createEmptyBorder());
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(isSelected() ? TOOL_SELECTED : new Color(255, 255, 255, 18));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 7, 7);
            if (isSelected()) {
                g2.setColor(ACCENT);
            } else {
                g2.setColor(new Color(255, 255, 255, 45));
            }
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 7, 7);

            g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(isSelected() ? new Color(83, 171, 255) : TEXT);
            paintToolIcon(g2, toolType);
            paintToolLabel(g2);
            g2.dispose();
        }

        private void paintToolIcon(Graphics2D g2, ToolType type) {
            int cx = getWidth() / 2;
            int cy = 21;
            if (type == ToolType.SELECT) {
                Polygon p = new Polygon();
                p.addPoint(cx - 7, cy - 11);
                p.addPoint(cx - 6, cy + 10);
                p.addPoint(cx, cy + 4);
                p.addPoint(cx + 6, cy + 11);
                p.addPoint(cx + 9, cy + 9);
                p.addPoint(cx + 4, cy + 2);
                p.addPoint(cx + 11, cy + 2);
                g2.drawPolygon(p);
            } else if (type == ToolType.RECTANGLE) {
                g2.drawRect(cx - 12, cy - 9, 24, 18);
            } else if (type == ToolType.CIRCLE) {
                g2.drawOval(cx - 12, cy - 12, 24, 24);
            } else if (type == ToolType.TRIANGLE) {
                Polygon p = new Polygon();
                p.addPoint(cx, cy - 13);
                p.addPoint(cx - 12, cy + 11);
                p.addPoint(cx + 12, cy + 11);
                g2.drawPolygon(p);
            } else if (type == ToolType.PARALLELOGRAM) {
                Polygon p = new Polygon();
                p.addPoint(cx - 6, cy - 11);
                p.addPoint(cx + 13, cy - 11);
                p.addPoint(cx + 6, cy + 11);
                p.addPoint(cx - 13, cy + 11);
                g2.drawPolygon(p);
            } else if (type == ToolType.STAR) {
                Polygon p = new Polygon();
                for (int i = 0; i < 10; i++) {
                    double angle = -Math.PI / 2.0 + i * Math.PI / 5.0;
                    double radius = (i % 2 == 0) ? 13 : 6;
                    p.addPoint(
                            (int) Math.round(cx + Math.cos(angle) * radius),
                            (int) Math.round(cy + Math.sin(angle) * radius));
                }
                g2.drawPolygon(p);
            } else if (type == ToolType.LINE) {
                g2.drawLine(cx - 12, cy + 11, cx + 12, cy - 11);
            }
        }

        private void paintToolLabel(Graphics2D g2) {
            String label = toolType.getDisplayName();
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 9f));
            FontMetrics metrics = g2.getFontMetrics();
            int maxWidth = getWidth() - 8;
            while (metrics.stringWidth(label) > maxWidth && g2.getFont().getSize2D() > 7f) {
                g2.setFont(g2.getFont().deriveFont(g2.getFont().getSize2D() - 0.5f));
                metrics = g2.getFontMetrics();
            }

            int x = (getWidth() - metrics.stringWidth(label)) / 2;
            int y = getHeight() - 9;
            g2.setColor(isSelected() ? TEXT : new Color(210, 216, 222));
            g2.drawString(label, x, y);
        }
    }

    private void toggleAnimationState(JButton btn) {
        if (drawingPanel.isAnimating()) {
            drawingPanel.stopAnimation();
            btn.setText("Play");
            btn.putClientProperty("iconType", "play");
            statusLabel.setText("Animasi dihentikan");
        } else {
            drawingPanel.startAnimation();
            btn.setText("Stop");
            btn.putClientProperty("iconType", "stop");
            statusLabel.setText("Animasi dijalankan...");
        }
        btn.repaint();
    }

    private JMenu createAnimationMenu() {
        JMenu animMenu = new JMenu("Animasi");

        JMenuItem playItem = new JMenuItem("Mulai/Hentikan Animasi");
        playItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(
                java.awt.event.KeyEvent.VK_SPACE,
                java.awt.event.InputEvent.CTRL_DOWN_MASK));
        playItem.addActionListener(e -> {
            if (animStatusButton != null) {
                toggleAnimationState(animStatusButton);
            }
        });

        JMenu typeSubMenu = new JMenu("Atur Animasi Shape Terpilih");
        
        JMenuItem noneItem = new JMenuItem("None");
        noneItem.addActionListener(e -> applyAnimationToSelected(AnimationType.NONE));
        
        JMenuItem spinItem = new JMenuItem("Spin (Rotate)");
        spinItem.addActionListener(e -> applyAnimationToSelected(AnimationType.SPIN));
        
        JMenuItem bounceItem = new JMenuItem("Bounce (Move)");
        bounceItem.addActionListener(e -> applyAnimationToSelected(AnimationType.BOUNCE));
        
        JMenuItem pulseItem = new JMenuItem("Pulse (Scale)");
        pulseItem.addActionListener(e -> applyAnimationToSelected(AnimationType.PULSE));

        typeSubMenu.add(noneItem);
        typeSubMenu.add(spinItem);
        typeSubMenu.add(bounceItem);
        typeSubMenu.add(pulseItem);

        animMenu.add(playItem);
        animMenu.addSeparator();
        animMenu.add(typeSubMenu);

        return animMenu;
    }

    private void applyAnimationToSelected(AnimationType type) {
        List<ShapeObject> selected = shapeManager.getSelectedShapes();
        if (selected.isEmpty()) {
            statusLabel.setText("Pilih shape terlebih dahulu");
            return;
        }
        for (ShapeObject s : selected) {
            if (s instanceof GroupObject) {
                for (ShapeObject member : ((GroupObject) s).getMembers()) {
                    member.setAnimationType(type);
                }
            } else {
                s.setAnimationType(type);
            }
        }
        drawingPanel.repaint();
        onSelectionChanged(shapeManager.getSelectedShape());
        statusLabel.setText("Animasi " + type.getDisplayName() + " diterapkan ke " + selected.size() + " shape");
    }
}