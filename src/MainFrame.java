import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
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
import javax.swing.JToggleButton;
import javax.swing.filechooser.FileNameExtensionFilter;

public class MainFrame extends JFrame {

    private final ShapeManager shapeManager;
    private final DrawingPanel drawingPanel;
    private final PropertyPanel propertyPanel;
    private final JLabel statusLabel;
    private JButton fillColorButton;
    private JButton strokeColorButton;

    public MainFrame() {
        shapeManager = new ShapeManager();
        drawingPanel = new DrawingPanel(shapeManager, this::onSelectionChanged);
        propertyPanel = new PropertyPanel(() -> drawingPanel.repaint());
        statusLabel = new JLabel("Ready");

        setTitle("Simple Paint - Grafika Komputer");
        setSize(1200, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        setJMenuBar(createMenuBar());
        add(createToolBar(), BorderLayout.WEST);
        add(drawingPanel, BorderLayout.CENTER);
        add(propertyPanel, BorderLayout.EAST);
        add(createStatusBar(), BorderLayout.SOUTH);

        setVisible(true);
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(createFileMenu());
        menuBar.add(createEditMenu());
        menuBar.add(createTransformMenu());
        menuBar.add(createStyleMenu());
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

        JMenuItem reflectItem = new JMenuItem("Reflect Horizontal");
        reflectItem.addActionListener(e -> applyTransformToSelected(
                shape -> shape.setReflected(!shape.isReflected())));

        JMenuItem resetItem = new JMenuItem("Reset Transform");
        resetItem.addActionListener(e -> applyTransformToSelected(shape -> {
            shape.setRotation(0);
            shape.setScaleX(1.0);
            shape.setScaleY(1.0);
            shape.setSkewX(0);
            shape.setSkewY(0);
            shape.setReflected(false);
        }));

        transformMenu.add(rotateItem);
        transformMenu.add(rotateCCWItem);
        transformMenu.addSeparator();
        transformMenu.add(scaleUpItem);
        transformMenu.add(scaleDownItem);
        transformMenu.addSeparator();
        transformMenu.add(reflectItem);
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
        fillColorItem.addActionListener(e -> chooseFillColor());

        JMenuItem strokeColorItem = new JMenuItem("Stroke Color");
        strokeColorItem.addActionListener(e -> chooseStrokeColor());

        styleMenu.add(fillColorItem);
        styleMenu.add(strokeColorItem);
        return styleMenu;
    }

    // =========================================================================
    // Toolbar
    // =========================================================================

    private JPanel createToolBar() {
        JPanel toolBar = new JPanel(new GridLayout(7, 1, 6, 6));
        toolBar.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        toolBar.setPreferredSize(new Dimension(130, 0));

        ButtonGroup toolGroup = new ButtonGroup();
        addToolButton(toolBar, toolGroup, ToolType.SELECT, true);
        addToolButton(toolBar, toolGroup, ToolType.RECTANGLE, false);
        addToolButton(toolBar, toolGroup, ToolType.CIRCLE, false);
        addToolButton(toolBar, toolGroup, ToolType.TRIANGLE, false);
        addToolButton(toolBar, toolGroup, ToolType.LINE, false);

        fillColorButton = createColorButton("Fill", drawingPanel.getCurrentFillColor());
        fillColorButton.addActionListener(e -> chooseFillColor());
        toolBar.add(fillColorButton);

        strokeColorButton = createColorButton("Stroke", drawingPanel.getCurrentStrokeColor());
        strokeColorButton.addActionListener(e -> chooseStrokeColor());
        toolBar.add(strokeColorButton);

        return toolBar;
    }

    private void addToolButton(JPanel toolBar, ButtonGroup toolGroup, ToolType toolType, boolean selected) {
        JToggleButton button = new JToggleButton(toolType.getDisplayName());
        button.setSelected(selected);
        button.addActionListener(e -> {
            drawingPanel.setCurrentTool(toolType);
            statusLabel.setText("Tool: " + toolType.getDisplayName());
        });

        toolGroup.add(button);
        toolBar.add(button);
    }

    private JButton createColorButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setBackground(color);
        button.setOpaque(true);
        button.setBorderPainted(true);
        return button;
    }

    // =========================================================================
    // Color choosers — bekerja untuk single dan multi-selection
    // =========================================================================

    private void chooseFillColor() {
        List<ShapeObject> selected = shapeManager.getSelectedShapes();
        Color initialColor = selected.isEmpty()
                ? drawingPanel.getCurrentFillColor()
                : selected.get(0).getFillColor();

        Color selectedColor = JColorChooser.showDialog(this, "Choose Fill Color", initialColor);
        if (selectedColor == null) return;

        if (!selected.isEmpty()) {
            for (ShapeObject s : selected) {
                if (s instanceof GroupObject) {
                    for (ShapeObject m : ((GroupObject) s).getMembers()) {
                        m.setFillColor(selectedColor);
                    }
                } else {
                    s.setFillColor(selectedColor);
                }
            }
            drawingPanel.repaint();
        } else {
            drawingPanel.setCurrentFillColor(selectedColor);
        }
        fillColorButton.setBackground(selectedColor);
        statusLabel.setText("Fill color updated");
    }

    private void chooseStrokeColor() {
        List<ShapeObject> selected = shapeManager.getSelectedShapes();
        Color initialColor = selected.isEmpty()
                ? drawingPanel.getCurrentStrokeColor()
                : selected.get(0).getStrokeColor();

        Color selectedColor = JColorChooser.showDialog(this, "Choose Stroke Color", initialColor);
        if (selectedColor == null) return;

        if (!selected.isEmpty()) {
            for (ShapeObject s : selected) {
                if (s instanceof GroupObject) {
                    for (ShapeObject m : ((GroupObject) s).getMembers()) {
                        m.setStrokeColor(selectedColor);
                    }
                } else {
                    s.setStrokeColor(selectedColor);
                }
            }
            drawingPanel.repaint();
        } else {
            drawingPanel.setCurrentStrokeColor(selectedColor);
        }
        strokeColorButton.setBackground(selectedColor);
        statusLabel.setText("Stroke color updated");
    }

    // =========================================================================
    // Status bar & selection callback
    // =========================================================================

    private JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        statusBar.add(statusLabel, BorderLayout.WEST);
        return statusBar;
    }

    private void onSelectionChanged(ShapeObject selectedShape) {
        List<ShapeObject> selected = shapeManager.getSelectedShapes();

        if (selected.isEmpty()) {
            propertyPanel.showShape(null);
            statusLabel.setText("No object selected");
            fillColorButton.setBackground(drawingPanel.getCurrentFillColor());
            strokeColorButton.setBackground(drawingPanel.getCurrentStrokeColor());
            return;
        }

        if (selected.size() == 1) {
            ShapeObject s = selected.get(0);
            propertyPanel.showShape(s);
            String label = (s instanceof GroupObject)
                    ? "Group (" + ((GroupObject) s).getMembers().size() + " shapes)"
                    : s.getType().getDisplayName() + " #" + s.getId();
            statusLabel.setText("Selected: " + label);
            fillColorButton.setBackground(s.getFillColor());
            strokeColorButton.setBackground(s.getStrokeColor());
        } else {
            // Multi-selection: tampilkan info jumlah
            propertyPanel.showShape(null);
            statusLabel.setText("Selected: " + selected.size() + " shapes");
            fillColorButton.setBackground(selected.get(0).getFillColor());
            strokeColorButton.setBackground(selected.get(0).getStrokeColor());
        }
    }
}
