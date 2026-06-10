import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeListener;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.Font;
import java.awt.FlowLayout;

public class PropertyPanel extends JPanel {

    private static final Color PANEL_BG = new Color(42, 45, 49);
    private static final Color SECTION_BG = new Color(33, 36, 40);
    private static final Color FIELD_BG = new Color(27, 30, 34);
    private static final Color BORDER = new Color(64, 70, 76);
    private static final Color TEXT = new Color(235, 238, 241);
    private static final Color MUTED_TEXT = new Color(178, 184, 191);

    private final JLabel typeValue;
    private final JLabel widthValue;
    private final JLabel heightValue;

    private final JSpinner xSpinner;
    private final JSpinner ySpinner;

    private final JSpinner rotationSpinner;
    private final JSpinner scaleXSpinner;
    private final JSpinner scaleYSpinner;
    private final JSpinner skewXSpinner;
    private final JSpinner skewYSpinner;

    private final JRadioButton reflectNoneRadio;
    private final JRadioButton reflectHorizontalRadio;
    private final JRadioButton reflectVerticalRadio;
    private final ButtonGroup reflectionGroup;

    private final JComboBox<AnimationType> animTypeComboBox;

    private ShapeObject currentShape;
    private Runnable onChangeCallback;

    private boolean isUpdating = false;

    public PropertyPanel(Runnable onChangeCallback) {
        this.onChangeCallback = onChangeCallback;

        setLayout(new BorderLayout());
        setBackground(PANEL_BG);
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        setMaximumSize(new Dimension(250, Integer.MAX_VALUE));

        JLabel titleLabel = new JLabel("Transform");
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

        typeValue = new JLabel("-");
        widthValue = new JLabel("-");
        heightValue = new JLabel("-");

        xSpinner = new JSpinner(new SpinnerNumberModel(0, -9999, 9999, 1));
        ySpinner = new JSpinner(new SpinnerNumberModel(0, -9999, 9999, 1));

        rotationSpinner = new JSpinner(new SpinnerNumberModel(0.0, -360.0, 360.0, 1.0));

        scaleXSpinner = new JSpinner(new SpinnerNumberModel(1.0, 0.1, 10.0, 0.1));
        scaleYSpinner = new JSpinner(new SpinnerNumberModel(1.0, 0.1, 10.0, 0.1));

        skewXSpinner = new JSpinner(new SpinnerNumberModel(0.0, -5.0, 5.0, 0.1));
        skewYSpinner = new JSpinner(new SpinnerNumberModel(0.0, -5.0, 5.0, 0.1));

        // Radio buttons for reflection
        reflectNoneRadio = new JRadioButton("None");
        reflectHorizontalRadio = new JRadioButton("Horizontal");
        reflectVerticalRadio = new JRadioButton("Vertical");
        reflectionGroup = new ButtonGroup();
        reflectionGroup.add(reflectNoneRadio);
        reflectionGroup.add(reflectHorizontalRadio);
        reflectionGroup.add(reflectVerticalRadio);
        reflectNoneRadio.setSelected(true);

        animTypeComboBox = new JComboBox<>(AnimationType.values());
        animTypeComboBox.setBackground(FIELD_BG);
        animTypeComboBox.setForeground(TEXT);
        animTypeComboBox.setBorder(BorderFactory.createLineBorder(BORDER));

        styleRadioButton(reflectNoneRadio);
        styleRadioButton(reflectHorizontalRadio);
        styleRadioButton(reflectVerticalRadio);

        styleSpinner(xSpinner);
        styleSpinner(ySpinner);
        styleSpinner(rotationSpinner);
        styleSpinner(scaleXSpinner);
        styleSpinner(scaleYSpinner);
        styleSpinner(skewXSpinner);
        styleSpinner(skewYSpinner);
        styleValueLabel(typeValue);
        styleValueLabel(widthValue);
        styleValueLabel(heightValue);

        int row = 0;

        // ── Info ──
        addSectionLabel(contentPanel, row++, "Info");
        addLabelRow(contentPanel, row++, "Type", typeValue);

        // Width & Height sejajar dalam satu baris
        addWidthHeightRow(contentPanel, row++);

        // ── Translasi ──
        addSectionLabel(contentPanel, row++, "Translasi");
        // Label "Position" kemudian X Y dalam satu baris di bawahnya
        addXYLabeledRow(contentPanel, row++, "Position", xSpinner, ySpinner);

        // ── Transformasi ──
        addSectionLabel(contentPanel, row++, "Transformasi");

        // Rotation: label di atas, spinner di bawah
        addLabeledSpinnerBlock(contentPanel, row++, "Rotation (°)", rotationSpinner);

        // Scale: label di atas, X Y spinner sejajar
        addLabeledXYSpinnerRow(contentPanel, row++, "Scale", scaleXSpinner, scaleYSpinner);

        // Skew: label di atas, X Y spinner sejajar
        addLabeledXYSpinnerRow(contentPanel, row++, "Skew", skewXSpinner, skewYSpinner);

        // Reflection: label di atas, radio buttons di bawah
        addReflectionBlock(contentPanel, row++);

        // ── Animasi ──
        addSectionLabel(contentPanel, row++, "Animasi");
        addAnimationBlock(contentPanel, row++);

        setupListeners();
    }

    private void setupListeners() {
    ChangeListener listener = e -> {
        if (currentShape == null || isUpdating) return;
        currentShape.setX(((Number) xSpinner.getValue()).intValue());
        currentShape.setY(((Number) ySpinner.getValue()).intValue());
        currentShape.setRotation(((Number) rotationSpinner.getValue()).doubleValue());
        currentShape.setScaleX(((Number) scaleXSpinner.getValue()).doubleValue());
        currentShape.setScaleY(((Number) scaleYSpinner.getValue()).doubleValue());
        currentShape.setSkewX(((Number) skewXSpinner.getValue()).doubleValue());
        currentShape.setSkewY(((Number) skewYSpinner.getValue()).doubleValue());
        onChangeCallback.run();
    };

    xSpinner.addChangeListener(listener);
    ySpinner.addChangeListener(listener);
    rotationSpinner.addChangeListener(listener);
    scaleXSpinner.addChangeListener(listener);
    scaleYSpinner.addChangeListener(listener);
    skewXSpinner.addChangeListener(listener);
    skewYSpinner.addChangeListener(listener);

    // Reflection listener - TIDAK mengubah line style objek asli
    java.awt.event.ActionListener reflectionListener = e -> {
        if (isUpdating || currentShape == null) return;
        
        if (reflectNoneRadio.isSelected()) {
            currentShape.setReflected(false);
            currentShape.setReflectDirection(0);
        } else if (reflectHorizontalRadio.isSelected()) {
            currentShape.setReflected(true);
            currentShape.setReflectDirection(1);
        } else if (reflectVerticalRadio.isSelected()) {
            currentShape.setReflected(true);
            currentShape.setReflectDirection(2);
        }
        onChangeCallback.run();
    };
    reflectNoneRadio.addActionListener(reflectionListener);
    reflectHorizontalRadio.addActionListener(reflectionListener);
    reflectVerticalRadio.addActionListener(reflectionListener);

    animTypeComboBox.addActionListener(e -> {
        if (isUpdating || currentShape == null) return;
        currentShape.setAnimationType((AnimationType) animTypeComboBox.getSelectedItem());
        onChangeCallback.run();
    });
}

public void showShape(ShapeObject shape) {
    isUpdating = true;
    currentShape = shape;

    if (shape == null) {
        xSpinner.setValue(0);
        ySpinner.setValue(0);
        rotationSpinner.setValue(0.0);
        scaleXSpinner.setValue(1.0);
        scaleYSpinner.setValue(1.0);
        skewXSpinner.setValue(0.0);
        skewYSpinner.setValue(0.0);
        
        typeValue.setText("-");
        widthValue.setText("-");
        heightValue.setText("-");
        
        reflectNoneRadio.setSelected(true);
        animTypeComboBox.setSelectedItem(AnimationType.NONE);
        
        xSpinner.setEnabled(false);
        ySpinner.setEnabled(false);
        rotationSpinner.setEnabled(false);
        scaleXSpinner.setEnabled(false);
        scaleYSpinner.setEnabled(false);
        skewXSpinner.setEnabled(false);
        skewYSpinner.setEnabled(false);
        reflectNoneRadio.setEnabled(false);
        reflectHorizontalRadio.setEnabled(false);
        reflectVerticalRadio.setEnabled(false);
        animTypeComboBox.setEnabled(false);
    } else {
        xSpinner.setValue(shape.getX());
        ySpinner.setValue(shape.getY());
        rotationSpinner.setValue(shape.getRotation());
        scaleXSpinner.setValue(shape.getScaleX());
        scaleYSpinner.setValue(shape.getScaleY());
        skewXSpinner.setValue(shape.getSkewX());
        skewYSpinner.setValue(shape.getSkewY());
        
        typeValue.setText(shape.getType().getDisplayName());
        widthValue.setText(String.valueOf(shape.getWidth()));
        heightValue.setText(String.valueOf(shape.getHeight()));
        
        animTypeComboBox.setSelectedItem(shape.getAnimationType());
        
        xSpinner.setEnabled(true);
        ySpinner.setEnabled(true);
        rotationSpinner.setEnabled(true);
        scaleXSpinner.setEnabled(true);
        scaleYSpinner.setEnabled(true);
        skewXSpinner.setEnabled(true);
        skewYSpinner.setEnabled(true);
        reflectNoneRadio.setEnabled(true);
        reflectHorizontalRadio.setEnabled(true);
        reflectVerticalRadio.setEnabled(true);
        animTypeComboBox.setEnabled(true);
        
        // Reflection handling
        if (!shape.isReflected()) {
            reflectNoneRadio.setSelected(true);
        } else {
            if (shape.getReflectDirection() == 1) {
                reflectHorizontalRadio.setSelected(true);
            } else if (shape.getReflectDirection() == 2) {
                reflectVerticalRadio.setSelected(true);
            } else {
                reflectNoneRadio.setSelected(true);
            }
        }
    }
    isUpdating = false;
}

    // -------------------------------------------------------------------------
    // Layout helper methods
    // -------------------------------------------------------------------------

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

    /** Width dan Height sejajar: "W: 120   H: 80" */
    private void addWidthHeightRow(JPanel panel, int row) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 0, 4, 0);

        JPanel wh = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        wh.setOpaque(false);

        JLabel wLabel = createFieldLabel("W:");
        wLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 4));
        wh.add(wLabel);
        wh.add(widthValue);

        JLabel spacer = new JLabel("   ");
        spacer.setForeground(MUTED_TEXT);
        wh.add(spacer);

        JLabel hLabel = createFieldLabel("H:");
        hLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 4));
        wh.add(hLabel);
        wh.add(heightValue);

        panel.add(wh, gbc);
    }

    /** Untuk Info: label kiri, value kanan */
    private void addLabelRow(JPanel panel, int row, String label, JLabel valueLabel) {
        GridBagConstraints labelGbc = new GridBagConstraints();
        labelGbc.gridx = 0;
        labelGbc.gridy = row;
        labelGbc.anchor = GridBagConstraints.WEST;
        labelGbc.insets = new Insets(4, 0, 4, 12);
        panel.add(createFieldLabel(label), labelGbc);

        GridBagConstraints valueGbc = new GridBagConstraints();
        valueGbc.gridx = 1;
        valueGbc.gridy = row;
        valueGbc.weightx = 1.0;
        valueGbc.anchor = GridBagConstraints.WEST;
        valueGbc.insets = new Insets(4, 0, 4, 0);
        panel.add(valueLabel, valueGbc);
    }

    /**
     * Label di atas ("Position"), lalu X [ spinner ] Y [ spinner ] di bawahnya.
     */
    private void addXYLabeledRow(JPanel panel, int row, String labelText,
                                  JSpinner spinnerX, JSpinner spinnerY) {
        // Label atas
        GridBagConstraints labelGbc = new GridBagConstraints();
        labelGbc.gridx = 0;
        labelGbc.gridy = row;
        labelGbc.gridwidth = 2;
        labelGbc.anchor = GridBagConstraints.WEST;
        labelGbc.insets = new Insets(6, 0, 2, 0);
        JLabel lbl = createFieldLabel(labelText);
        panel.add(lbl, labelGbc);

        // X Y row
        GridBagConstraints xyGbc = new GridBagConstraints();
        xyGbc.gridx = 0;
        xyGbc.gridy = row + 1;  // will be handled by caller incrementing row
        // We reuse row; caller passes row for label, we paint both label and xy in sequence
        // Actually easier: put them in a sub-panel
        // Let's just override: label row = row, xy inline = row+0 but after adding label
        // We'll use a sub-panel approach for cleanliness
        panel.remove(lbl); // remove what we just added, use sub-panel instead

        JPanel block = new JPanel(new GridBagLayout());
        block.setOpaque(false);

        GridBagConstraints bGbc = new GridBagConstraints();
        bGbc.gridx = 0; bGbc.gridy = 0;
        bGbc.gridwidth = 2; bGbc.anchor = GridBagConstraints.WEST;
        bGbc.insets = new Insets(0, 0, 4, 0);
        block.add(createFieldLabel(labelText), bGbc);

        // X label + spinner
        GridBagConstraints xLblGbc = new GridBagConstraints();
        xLblGbc.gridx = 0; xLblGbc.gridy = 1;
        xLblGbc.anchor = GridBagConstraints.WEST;
        xLblGbc.insets = new Insets(0, 0, 0, 4);
        block.add(createFieldLabel("X:"), xLblGbc);

        GridBagConstraints xSpGbc = new GridBagConstraints();
        xSpGbc.gridx = 1; xSpGbc.gridy = 1;
        xSpGbc.weightx = 0.5; xSpGbc.fill = GridBagConstraints.HORIZONTAL;
        xSpGbc.insets = new Insets(0, 0, 0, 8);
        block.add(spinnerX, xSpGbc);

        // Y label + spinner
        GridBagConstraints yLblGbc = new GridBagConstraints();
        yLblGbc.gridx = 2; yLblGbc.gridy = 1;
        yLblGbc.anchor = GridBagConstraints.WEST;
        yLblGbc.insets = new Insets(0, 0, 0, 4);
        block.add(createFieldLabel("Y:"), yLblGbc);

        GridBagConstraints ySpGbc = new GridBagConstraints();
        ySpGbc.gridx = 3; ySpGbc.gridy = 1;
        ySpGbc.weightx = 0.5; ySpGbc.fill = GridBagConstraints.HORIZONTAL;
        block.add(spinnerY, ySpGbc);

        GridBagConstraints blockGbc = new GridBagConstraints();
        blockGbc.gridx = 0; blockGbc.gridy = row;
        blockGbc.gridwidth = 2;
        blockGbc.fill = GridBagConstraints.HORIZONTAL;
        blockGbc.insets = new Insets(4, 0, 4, 0);
        panel.add(block, blockGbc);
    }

    /**
     * Label di atas (mis. "Rotation (°)"), spinner full-width di bawahnya.
     */
    private void addLabeledSpinnerBlock(JPanel panel, int row, String labelText, JSpinner spinner) {
        JPanel block = new JPanel(new GridBagLayout());
        block.setOpaque(false);

        GridBagConstraints lblGbc = new GridBagConstraints();
        lblGbc.gridx = 0; lblGbc.gridy = 0;
        lblGbc.anchor = GridBagConstraints.WEST;
        lblGbc.insets = new Insets(0, 0, 4, 0);
        block.add(createFieldLabel(labelText), lblGbc);

        GridBagConstraints spGbc = new GridBagConstraints();
        spGbc.gridx = 0; spGbc.gridy = 1;
        spGbc.weightx = 1.0; spGbc.fill = GridBagConstraints.HORIZONTAL;
        block.add(spinner, spGbc);

        GridBagConstraints blockGbc = new GridBagConstraints();
        blockGbc.gridx = 0; blockGbc.gridy = row;
        blockGbc.gridwidth = 2;
        blockGbc.fill = GridBagConstraints.HORIZONTAL;
        blockGbc.insets = new Insets(4, 0, 4, 0);
        panel.add(block, blockGbc);
    }

    /**
     * Label di atas (mis. "Scale"), lalu X [ spinner ] Y [ spinner ] di bawahnya.
     */
    private void addLabeledXYSpinnerRow(JPanel panel, int row, String labelText,
                                         JSpinner spinnerX, JSpinner spinnerY) {
        JPanel block = new JPanel(new GridBagLayout());
        block.setOpaque(false);

        // Label atas
        GridBagConstraints lblGbc = new GridBagConstraints();
        lblGbc.gridx = 0; lblGbc.gridy = 0;
        lblGbc.gridwidth = 4; lblGbc.anchor = GridBagConstraints.WEST;
        lblGbc.insets = new Insets(0, 0, 4, 0);
        block.add(createFieldLabel(labelText), lblGbc);

        // X label
        GridBagConstraints xLblGbc = new GridBagConstraints();
        xLblGbc.gridx = 0; xLblGbc.gridy = 1;
        xLblGbc.anchor = GridBagConstraints.WEST;
        xLblGbc.insets = new Insets(0, 0, 0, 4);
        block.add(createFieldLabel("X:"), xLblGbc);

        // X spinner
        GridBagConstraints xSpGbc = new GridBagConstraints();
        xSpGbc.gridx = 1; xSpGbc.gridy = 1;
        xSpGbc.weightx = 0.5; xSpGbc.fill = GridBagConstraints.HORIZONTAL;
        xSpGbc.insets = new Insets(0, 0, 0, 8);
        block.add(spinnerX, xSpGbc);

        // Y label
        GridBagConstraints yLblGbc = new GridBagConstraints();
        yLblGbc.gridx = 2; yLblGbc.gridy = 1;
        yLblGbc.anchor = GridBagConstraints.WEST;
        yLblGbc.insets = new Insets(0, 0, 0, 4);
        block.add(createFieldLabel("Y:"), yLblGbc);

        // Y spinner
        GridBagConstraints ySpGbc = new GridBagConstraints();
        ySpGbc.gridx = 3; ySpGbc.gridy = 1;
        ySpGbc.weightx = 0.5; ySpGbc.fill = GridBagConstraints.HORIZONTAL;
        block.add(spinnerY, ySpGbc);

        GridBagConstraints blockGbc = new GridBagConstraints();
        blockGbc.gridx = 0; blockGbc.gridy = row;
        blockGbc.gridwidth = 2;
        blockGbc.fill = GridBagConstraints.HORIZONTAL;
        blockGbc.insets = new Insets(4, 0, 4, 0);
        panel.add(block, blockGbc);
    }

    /**
     * Reflection block: label "Reflection" di atas,
     * radio buttons Horizontal dan Vertical di bawah sejajar.
     */
    private void addReflectionBlock(JPanel panel, int row) {
        JPanel block = new JPanel(new GridBagLayout());
        block.setOpaque(false);

        // Label "Reflection"
        GridBagConstraints lblGbc = new GridBagConstraints();
        lblGbc.gridx = 0; lblGbc.gridy = 0;
        lblGbc.gridwidth = 3; lblGbc.anchor = GridBagConstraints.WEST;
        lblGbc.insets = new Insets(0, 0, 4, 0);
        block.add(createFieldLabel("Reflection"), lblGbc);

        // Radio: None
        GridBagConstraints noneGbc = new GridBagConstraints();
        noneGbc.gridx = 0; noneGbc.gridy = 1;
        noneGbc.anchor = GridBagConstraints.WEST;
        noneGbc.insets = new Insets(0, 0, 0, 4);
        block.add(reflectNoneRadio, noneGbc);

        // Radio: Horizontal
        GridBagConstraints hGbc = new GridBagConstraints();
        hGbc.gridx = 1; hGbc.gridy = 1;
        hGbc.anchor = GridBagConstraints.WEST;
        hGbc.insets = new Insets(0, 0, 0, 4);
        block.add(reflectHorizontalRadio, hGbc);

        // Radio: Vertical
        GridBagConstraints vGbc = new GridBagConstraints();
        vGbc.gridx = 2; vGbc.gridy = 1;
        vGbc.weightx = 1.0;
        vGbc.anchor = GridBagConstraints.WEST;
        block.add(reflectVerticalRadio, vGbc);

        GridBagConstraints blockGbc = new GridBagConstraints();
        blockGbc.gridx = 0; blockGbc.gridy = row;
        blockGbc.gridwidth = 2;
        blockGbc.fill = GridBagConstraints.HORIZONTAL;
        blockGbc.insets = new Insets(4, 0, 4, 0);
        panel.add(block, blockGbc);
    }

    private JLabel createFieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(MUTED_TEXT);
        return label;
    }

    private void styleValueLabel(JLabel label) {
        label.setForeground(TEXT);
    }

    private void styleSpinner(JSpinner spinner) {
        spinner.setBorder(BorderFactory.createLineBorder(BORDER));
        JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) spinner.getEditor();
        editor.getTextField().setBackground(FIELD_BG);
        editor.getTextField().setForeground(TEXT);
        editor.getTextField().setCaretColor(TEXT);
        editor.getTextField().setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
    }

    private void styleRadioButton(JRadioButton radio) {
        radio.setOpaque(false);
        radio.setForeground(TEXT);
        radio.setFont(radio.getFont().deriveFont(12f));
    }

    private void addAnimationBlock(JPanel panel, int row) {
        JPanel block = new JPanel(new GridBagLayout());
        block.setOpaque(false);

        // Label "Type"
        GridBagConstraints lblGbc = new GridBagConstraints();
        lblGbc.gridx = 0; lblGbc.gridy = 0;
        lblGbc.anchor = GridBagConstraints.WEST;
        lblGbc.insets = new Insets(0, 0, 4, 0);
        block.add(createFieldLabel("Tipe Animasi"), lblGbc);

        // Combo Box
        GridBagConstraints cbGbc = new GridBagConstraints();
        cbGbc.gridx = 0; cbGbc.gridy = 1;
        cbGbc.weightx = 1.0; cbGbc.fill = GridBagConstraints.HORIZONTAL;
        block.add(animTypeComboBox, cbGbc);

        GridBagConstraints blockGbc = new GridBagConstraints();
        blockGbc.gridx = 0; blockGbc.gridy = row;
        blockGbc.gridwidth = 2;
        blockGbc.fill = GridBagConstraints.HORIZONTAL;
        blockGbc.insets = new Insets(4, 0, 4, 0);
        panel.add(block, blockGbc);
    }
}