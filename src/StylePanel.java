import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class StylePanel extends JPanel {

    private static final Color PANEL_BG = new Color(42, 45, 49);
    private static final Color SECTION_BG = new Color(33, 36, 40);
    private static final Color FIELD_BG = new Color(27, 30, 34);
    private static final Color BORDER = new Color(64, 70, 76);
    private static final Color TEXT = new Color(235, 238, 241);
    private static final Color MUTED_TEXT = new Color(178, 184, 191);

    private final ShapeManager shapeManager;
    private final DrawingPanel drawingPanel;
    private final Runnable onChangeCallback;

    private final JCheckBox fillEnabledCheckbox;
    private final JCheckBox gradientCheckbox;
    private final ColorSwatch fillColorSwatch;
    private final ColorSwatch fillSecondarySwatch;
    private final ColorSwatch strokeColorSwatch;
    // Keep JButton references for API compatibility (delegate to swatches)
    private final JButton fillColorButton;
    private final JButton fillSecondaryButton;
    private final JButton strokeColorButton;
    private final JSpinner strokeWidthSpinner;
    private final JComboBox<LineStyle> lineStyleComboBox;

    private ShapeObject currentShape;
    private boolean isUpdating = false;

    public StylePanel(ShapeManager shapeManager, DrawingPanel drawingPanel, Runnable onChangeCallback) {
        this.shapeManager = shapeManager;
        this.drawingPanel = drawingPanel;
        this.onChangeCallback = onChangeCallback;

        setLayout(new BorderLayout());
        setBackground(PANEL_BG);
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        setMaximumSize(new Dimension(250, Integer.MAX_VALUE));

        JLabel titleLabel = new JLabel("Style");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        titleLabel.setForeground(TEXT);
        titleLabel.setOpaque(true);
        titleLabel.setBackground(SECTION_BG);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(9, 12, 9, 12));
        add(titleLabel, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setBackground(PANEL_BG);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(8, 12, 12, 12));
        add(contentPanel, BorderLayout.CENTER);

        fillEnabledCheckbox = new JCheckBox();
        gradientCheckbox = new JCheckBox();
        fillEnabledCheckbox.setOpaque(false);
        fillEnabledCheckbox.setForeground(TEXT);
        gradientCheckbox.setOpaque(false);
        gradientCheckbox.setForeground(TEXT);

        fillColorSwatch = new ColorSwatch(new Color(115, 166, 255));
        fillSecondarySwatch = new ColorSwatch(new Color(255, 200, 100));
        strokeColorSwatch = new ColorSwatch(Color.BLACK);
        // Delegate JButton references to swatch's hidden button for legacy API
        fillColorButton = fillColorSwatch.getProxyButton();
        fillSecondaryButton = fillSecondarySwatch.getProxyButton();
        strokeColorButton = strokeColorSwatch.getProxyButton();

        strokeWidthSpinner = new JSpinner(new SpinnerNumberModel(2.0, 1.0, 30.0, 0.5));
        lineStyleComboBox = new JComboBox<>(LineStyle.values());
        styleSpinner(strokeWidthSpinner);
        lineStyleComboBox.setBackground(FIELD_BG);
        lineStyleComboBox.setForeground(TEXT);
        lineStyleComboBox.setBorder(BorderFactory.createLineBorder(BORDER));

        int row = 0;

        addSectionLabel(contentPanel, row++, "Fill");
        addCheckboxRow(contentPanel, row++, "Enable Fill", fillEnabledCheckbox);
        addSwatchRow(contentPanel, row++, "Fill Color", fillColorSwatch);
        addCheckboxRow(contentPanel, row++, "Gradient Fill", gradientCheckbox);
        addSwatchRow(contentPanel, row++, "Gradient End", fillSecondarySwatch);

        addSectionLabel(contentPanel, row++, "Stroke");
        addSwatchRow(contentPanel, row++, "Stroke Color", strokeColorSwatch);
        addSpinnerRow(contentPanel, row++, "Stroke Width", strokeWidthSpinner);
        addComboRow(contentPanel, row++, "Line Style", lineStyleComboBox);

        setupListeners();
        showShape(null);
    }

    public void showShape(ShapeObject shape) {
        isUpdating = true;
        currentShape = shape;

        ShapeObject source = resolveDisplayShape(shape);
        if (source == null) {
            fillEnabledCheckbox.setSelected(drawingPanel.isCurrentFillEnabled());
            fillColorSwatch.setColor(drawingPanel.getCurrentFillColor());
            fillSecondarySwatch.setColor(drawingPanel.getCurrentFillSecondaryColor());
            gradientCheckbox.setSelected(drawingPanel.isCurrentGradientFill());
            strokeColorSwatch.setColor(drawingPanel.getCurrentStrokeColor());
            strokeWidthSpinner.setValue((double) drawingPanel.getCurrentStrokeWidth());
            lineStyleComboBox.setSelectedItem(drawingPanel.getCurrentLineStyle());
        } else {
            fillEnabledCheckbox.setSelected(source.isFillEnabled());
            fillColorSwatch.setColor(source.getFillColor());
            fillSecondarySwatch.setColor(source.getFillSecondaryColor());
            gradientCheckbox.setSelected(source.isGradientFill());
            strokeColorSwatch.setColor(source.getStrokeColor());
            strokeWidthSpinner.setValue((double) source.getStrokeWidth());
            lineStyleComboBox.setSelectedItem(source.getLineStyle());
        }

        fillColorSwatch.setEnabled(fillEnabledCheckbox.isSelected());
        fillSecondarySwatch.setEnabled(fillEnabledCheckbox.isSelected() && gradientCheckbox.isSelected());
        gradientCheckbox.setEnabled(fillEnabledCheckbox.isSelected());

        isUpdating = false;
    }

    public void chooseFillColor() {
        Color initialColor = fillColorSwatch.getColor();
        Color selectedColor = JColorChooser.showDialog(this, "Fill Color", initialColor);
        if (selectedColor == null) {
            return;
        }
        fillColorSwatch.setColor(selectedColor);
        fillEnabledCheckbox.setSelected(true);
        fillColorSwatch.setEnabled(true);
        gradientCheckbox.setEnabled(true);
        applyCurrentStyle();
    }

    public void chooseFillSecondaryColor() {
        Color initialColor = fillSecondarySwatch.getColor();
        Color selectedColor = JColorChooser.showDialog(this, "Gradient End Color", initialColor);
        if (selectedColor == null) {
            return;
        }
        fillSecondarySwatch.setColor(selectedColor);
        fillEnabledCheckbox.setSelected(true);
        gradientCheckbox.setSelected(true);
        fillSecondarySwatch.setEnabled(true);
        applyCurrentStyle();
    }

    public void chooseStrokeColor() {
        Color initialColor = strokeColorSwatch.getColor();
        Color selectedColor = JColorChooser.showDialog(this, "Stroke Color", initialColor);
        if (selectedColor == null) {
            return;
        }
        strokeColorSwatch.setColor(selectedColor);
        applyCurrentStyle();
    }

    public void setFillEnabled(boolean enabled) {
        fillEnabledCheckbox.setSelected(enabled);
        fillColorSwatch.setEnabled(enabled);
        gradientCheckbox.setEnabled(enabled);
        fillSecondarySwatch.setEnabled(enabled && gradientCheckbox.isSelected());
        applyCurrentStyle();
    }

    public void setGradientEnabled(boolean enabled) {
        gradientCheckbox.setSelected(enabled);
        fillSecondarySwatch.setEnabled(fillEnabledCheckbox.isSelected() && enabled);
        applyCurrentStyle();
    }

    public void setFillColor(Color color) {
        fillColorSwatch.setColor(color);
        applyCurrentStyle();
    }

    public void setGradientEndColor(Color color) {
        fillSecondarySwatch.setColor(color);
        applyCurrentStyle();
    }

    public void setStrokeColor(Color color) {
        strokeColorSwatch.setColor(color);
        applyCurrentStyle();
    }

    private void setupListeners() {
        fillEnabledCheckbox.addActionListener(e -> {
            if (isUpdating) return;
            fillColorSwatch.setEnabled(fillEnabledCheckbox.isSelected());
            gradientCheckbox.setEnabled(fillEnabledCheckbox.isSelected());
            fillSecondarySwatch.setEnabled(fillEnabledCheckbox.isSelected() && gradientCheckbox.isSelected());
            applyCurrentStyle();
        });

        gradientCheckbox.addActionListener(e -> {
            if (isUpdating) return;
            fillSecondarySwatch.setEnabled(fillEnabledCheckbox.isSelected() && gradientCheckbox.isSelected());
            applyCurrentStyle();
        });

        fillColorSwatch.addClickListener(() -> chooseFillColor());
        fillSecondarySwatch.addClickListener(() -> chooseFillSecondaryColor());
        strokeColorSwatch.addClickListener(() -> chooseStrokeColor());

        strokeWidthSpinner.addChangeListener(e -> {
            if (isUpdating) return;
            applyCurrentStyle();
        });

        lineStyleComboBox.addActionListener(e -> {
            if (isUpdating) return;
            applyCurrentStyle();
        });
    }

    private void applyCurrentStyle() {
        if (isUpdating) {
            return;
        }

        List<ShapeObject> selected = shapeManager.getSelectedShapes();
        if (selected.isEmpty()) {
            drawingPanel.setCurrentFillEnabled(fillEnabledCheckbox.isSelected());
            drawingPanel.setCurrentFillColor(fillColorSwatch.getColor());
            drawingPanel.setCurrentFillSecondaryColor(fillSecondarySwatch.getColor());
            drawingPanel.setCurrentGradientFill(gradientCheckbox.isSelected());
            drawingPanel.setCurrentStrokeColor(strokeColorSwatch.getColor());
            drawingPanel.setCurrentStrokeWidth(((Number) strokeWidthSpinner.getValue()).floatValue());
            drawingPanel.setCurrentLineStyle((LineStyle) lineStyleComboBox.getSelectedItem());
        } else {
            for (ShapeObject shape : selected) {
                applyStyleToShape(shape);
            }
        }

        onChangeCallback.run();
    }

    private void applyStyleToShape(ShapeObject shape) {
        if (shape instanceof GroupObject) {
            for (ShapeObject member : ((GroupObject) shape).getMembers()) {
                applyStyleToShape(member);
            }
            return;
        }

        shape.setFillEnabled(fillEnabledCheckbox.isSelected());
        shape.setFillColor(fillColorSwatch.getColor());
        shape.setFillSecondaryColor(fillSecondarySwatch.getColor());
        shape.setGradientFill(gradientCheckbox.isSelected());
        shape.setStrokeColor(strokeColorSwatch.getColor());
        shape.setStrokeWidth(((Number) strokeWidthSpinner.getValue()).floatValue());
        shape.setLineStyle((LineStyle) lineStyleComboBox.getSelectedItem());
    }

    private ShapeObject resolveDisplayShape(ShapeObject shape) {
        if (shape instanceof GroupObject) {
            List<ShapeObject> members = ((GroupObject) shape).getMembers();
            if (!members.isEmpty()) {
                return members.get(0);
            }
        }
        return shape;
    }

    public void setStrokeWidth(float width) {
        strokeWidthSpinner.setValue((double) width);
        applyCurrentStyle();
    }

    public void setLineStyle(LineStyle lineStyle) {
        lineStyleComboBox.setSelectedItem(lineStyle);
        applyCurrentStyle();
    }

    // -------------------------------------------------------------------------
    // ColorSwatch — blok warna interaktif dengan label hex
    // -------------------------------------------------------------------------

    /**
     * Widget yang menampilkan kotak warna berwarna + teks hex di sebelahnya.
     * Klik pada widget akan membuka JColorChooser via clickListener.
     * Proxy JButton disediakan agar kode lama yang memanggil .getBackground()
     * tetap dapat bekerja.
     */
    public class ColorSwatch extends JPanel {
        private Color color;
        private final JLabel hexLabel;
        private final JButton proxyButton;
        private Runnable clickListener;

        public ColorSwatch(Color initialColor) {
            setLayout(new BorderLayout(6, 0));
            setOpaque(false);
            this.color = initialColor != null ? initialColor : Color.BLACK;

            // Kotak warna
            JPanel box = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    if (isEnabled()) {
                        g2.setColor(color);
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 5, 5);
                    } else {
                        // Pola kotak-kotak (disabled / no-fill)
                        g2.setColor(new Color(50, 54, 59));
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 5, 5);
                        g2.setColor(new Color(70, 76, 83));
                        int s = 4;
                        for (int r = 0; r < getHeight(); r += s) {
                            for (int c2 = 0; c2 < getWidth(); c2 += s) {
                                if ((r / s + c2 / s) % 2 == 0) g2.fillRect(c2, r, s, s);
                            }
                        }
                        g2.setColor(new Color(80, 87, 95));
                        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 5, 5);
                    }
                    if (isEnabled()) {
                        g2.setColor(BORDER);
                        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 5, 5);
                    }
                    g2.dispose();
                }
            };
            box.setOpaque(false);
            box.setPreferredSize(new Dimension(28, 22));
            box.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            box.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    if (isEnabled() && clickListener != null) clickListener.run();
                }
            });

            hexLabel = new JLabel(toHex(color));
            hexLabel.setForeground(new Color(178, 184, 191));
            hexLabel.setFont(hexLabel.getFont().deriveFont(Font.PLAIN, 11f));
            hexLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            hexLabel.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    if (isEnabled() && clickListener != null) clickListener.run();
                }
            });

            add(box, BorderLayout.WEST);
            add(hexLabel, BorderLayout.CENTER);

            proxyButton = new JButton();
            proxyButton.setBackground(this.color);
        }

        public void setColor(Color c) {
            if (c == null) return;
            this.color = c;
            hexLabel.setText(toHex(c));
            proxyButton.setBackground(c);
            repaint();
        }

        public Color getColor() { return color; }

        public JButton getProxyButton() { return proxyButton; }

        public void addClickListener(Runnable r) { this.clickListener = r; }

        @Override public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            hexLabel.setForeground(enabled ? new Color(178, 184, 191) : new Color(80, 87, 95));
            repaint();
        }

        private String toHex(Color c) {
            return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
        }
    }

    private void addSwatchRow(JPanel panel, int row, String label, ColorSwatch swatch) {
        GridBagConstraints labelGbc = new GridBagConstraints();
        labelGbc.gridx = 0;
        labelGbc.gridy = row;
        labelGbc.anchor = GridBagConstraints.WEST;
        labelGbc.insets = new Insets(4, 0, 4, 12);
        panel.add(createFieldLabel(label), labelGbc);

        GridBagConstraints swatchGbc = new GridBagConstraints();
        swatchGbc.gridx = 1;
        swatchGbc.gridy = row;
        swatchGbc.weightx = 1.0;
        swatchGbc.fill = GridBagConstraints.HORIZONTAL;
        swatchGbc.insets = new Insets(4, 0, 4, 0);
        panel.add(swatch, swatchGbc);
    }

    private void addSectionLabel(JPanel panel, int row, String text) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 0, 4, 0);
        JLabel label = new JLabel(text);
        label.setForeground(TEXT);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 13f));
        label.setOpaque(true);
        label.setBackground(SECTION_BG);
        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 1, 0, new Color(28, 31, 35)),
                BorderFactory.createEmptyBorder(7, 0, 7, 0)));
        panel.add(label, gbc);
    }

    private void addCheckboxRow(JPanel panel, int row, String label, JCheckBox checkbox) {
        GridBagConstraints labelGbc = new GridBagConstraints();
        labelGbc.gridx = 0;
        labelGbc.gridy = row;
        labelGbc.anchor = GridBagConstraints.WEST;
        labelGbc.insets = new Insets(4, 0, 4, 12);
        panel.add(createFieldLabel(label), labelGbc);

        GridBagConstraints checkGbc = new GridBagConstraints();
        checkGbc.gridx = 1;
        checkGbc.gridy = row;
        checkGbc.anchor = GridBagConstraints.WEST;
        checkGbc.insets = new Insets(4, 0, 4, 0);
        panel.add(checkbox, checkGbc);
    }

    private void addButtonRow(JPanel panel, int row, String label, JButton button) {
        GridBagConstraints labelGbc = new GridBagConstraints();
        labelGbc.gridx = 0;
        labelGbc.gridy = row;
        labelGbc.anchor = GridBagConstraints.WEST;
        labelGbc.insets = new Insets(4, 0, 4, 12);
        panel.add(createFieldLabel(label), labelGbc);

        GridBagConstraints buttonGbc = new GridBagConstraints();
        buttonGbc.gridx = 1;
        buttonGbc.gridy = row;
        buttonGbc.weightx = 1.0;
        buttonGbc.fill = GridBagConstraints.HORIZONTAL;
        buttonGbc.insets = new Insets(4, 0, 4, 0);
        panel.add(button, buttonGbc);
    }

    private void addSpinnerRow(JPanel panel, int row, String label, JSpinner spinner) {
        GridBagConstraints labelGbc = new GridBagConstraints();
        labelGbc.gridx = 0;
        labelGbc.gridy = row;
        labelGbc.anchor = GridBagConstraints.WEST;
        labelGbc.insets = new Insets(4, 0, 4, 12);
        panel.add(createFieldLabel(label), labelGbc);

        GridBagConstraints spinnerGbc = new GridBagConstraints();
        spinnerGbc.gridx = 1;
        spinnerGbc.gridy = row;
        spinnerGbc.weightx = 1.0;
        spinnerGbc.fill = GridBagConstraints.HORIZONTAL;
        spinnerGbc.insets = new Insets(4, 0, 4, 0);
        panel.add(spinner, spinnerGbc);
    }

    private void addComboRow(JPanel panel, int row, String label, JComboBox<LineStyle> comboBox) {
        GridBagConstraints labelGbc = new GridBagConstraints();
        labelGbc.gridx = 0;
        labelGbc.gridy = row;
        labelGbc.anchor = GridBagConstraints.WEST;
        labelGbc.insets = new Insets(4, 0, 4, 12);
        panel.add(createFieldLabel(label), labelGbc);

        GridBagConstraints comboGbc = new GridBagConstraints();
        comboGbc.gridx = 1;
        comboGbc.gridy = row;
        comboGbc.weightx = 1.0;
        comboGbc.fill = GridBagConstraints.HORIZONTAL;
        comboGbc.insets = new Insets(4, 0, 4, 0);
        panel.add(comboBox, comboGbc);
    }

    private JLabel createFieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(MUTED_TEXT);
        return label;
    }

    private void styleSpinner(JSpinner spinner) {
        spinner.setBorder(BorderFactory.createLineBorder(BORDER));
        JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) spinner.getEditor();
        editor.getTextField().setBackground(FIELD_BG);
        editor.getTextField().setForeground(TEXT);
        editor.getTextField().setCaretColor(TEXT);
        editor.getTextField().setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
    }
}