import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
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
        setPreferredSize(new Dimension(250, 0));

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
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(PANEL_BG);
        add(scrollPane, BorderLayout.CENTER);

        fillEnabledCheckbox = new JCheckBox();
        gradientCheckbox = new JCheckBox();
        fillEnabledCheckbox.setOpaque(false);
        fillEnabledCheckbox.setForeground(TEXT);
        gradientCheckbox.setOpaque(false);
        gradientCheckbox.setForeground(TEXT);

        fillColorButton = createColorButton();
        fillSecondaryButton = createColorButton();
        strokeColorButton = createColorButton();

        strokeWidthSpinner = new JSpinner(new SpinnerNumberModel(2.0, 1.0, 30.0, 0.5));
        lineStyleComboBox = new JComboBox<>(LineStyle.values());
        styleSpinner(strokeWidthSpinner);
        lineStyleComboBox.setBackground(FIELD_BG);
        lineStyleComboBox.setForeground(TEXT);
        lineStyleComboBox.setBorder(BorderFactory.createLineBorder(BORDER));

        int row = 0;

        addSectionLabel(contentPanel, row++, "Fill");
        addCheckboxRow(contentPanel, row++, "Enable Fill", fillEnabledCheckbox);
        addButtonRow(contentPanel, row++, "Fill Color", fillColorButton);
        addCheckboxRow(contentPanel, row++, "Gradient Fill", gradientCheckbox);
        addButtonRow(contentPanel, row++, "Gradient End", fillSecondaryButton);

        addSectionLabel(contentPanel, row++, "Stroke");
        addButtonRow(contentPanel, row++, "Stroke Color", strokeColorButton);
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
            fillColorButton.setBackground(drawingPanel.getCurrentFillColor());
            fillSecondaryButton.setBackground(drawingPanel.getCurrentFillSecondaryColor());
            gradientCheckbox.setSelected(drawingPanel.isCurrentGradientFill());
            strokeColorButton.setBackground(drawingPanel.getCurrentStrokeColor());
            strokeWidthSpinner.setValue((double) drawingPanel.getCurrentStrokeWidth());
            lineStyleComboBox.setSelectedItem(drawingPanel.getCurrentLineStyle());
        } else {
            fillEnabledCheckbox.setSelected(source.isFillEnabled());
            fillColorButton.setBackground(source.getFillColor());
            fillSecondaryButton.setBackground(source.getFillSecondaryColor());
            gradientCheckbox.setSelected(source.isGradientFill());
            strokeColorButton.setBackground(source.getStrokeColor());
            strokeWidthSpinner.setValue((double) source.getStrokeWidth());
            lineStyleComboBox.setSelectedItem(source.getLineStyle());
        }

        fillColorButton.setEnabled(fillEnabledCheckbox.isSelected());
        fillSecondaryButton.setEnabled(fillEnabledCheckbox.isSelected() && gradientCheckbox.isSelected());
        gradientCheckbox.setEnabled(fillEnabledCheckbox.isSelected());

        isUpdating = false;
    }

    public void chooseFillColor() {
        Color initialColor = fillColorButton.getBackground();
        Color selectedColor = JColorChooser.showDialog(this, "Fill Color", initialColor);
        if (selectedColor == null) {
            return;
        }
        fillColorButton.setBackground(selectedColor);
        fillEnabledCheckbox.setSelected(true);
        fillColorButton.setEnabled(true);
        gradientCheckbox.setEnabled(true);
        applyCurrentStyle();
    }

    public void chooseFillSecondaryColor() {
        Color initialColor = fillSecondaryButton.getBackground();
        Color selectedColor = JColorChooser.showDialog(this, "Gradient End Color", initialColor);
        if (selectedColor == null) {
            return;
        }
        fillSecondaryButton.setBackground(selectedColor);
        fillEnabledCheckbox.setSelected(true);
        gradientCheckbox.setSelected(true);
        fillSecondaryButton.setEnabled(true);
        applyCurrentStyle();
    }

    public void chooseStrokeColor() {
        Color initialColor = strokeColorButton.getBackground();
        Color selectedColor = JColorChooser.showDialog(this, "Stroke Color", initialColor);
        if (selectedColor == null) {
            return;
        }
        strokeColorButton.setBackground(selectedColor);
        applyCurrentStyle();
    }

    public void setFillEnabled(boolean enabled) {
        fillEnabledCheckbox.setSelected(enabled);
        fillColorButton.setEnabled(enabled);
        gradientCheckbox.setEnabled(enabled);
        fillSecondaryButton.setEnabled(enabled && gradientCheckbox.isSelected());
        applyCurrentStyle();
    }

    public void setGradientEnabled(boolean enabled) {
        gradientCheckbox.setSelected(enabled);
        fillSecondaryButton.setEnabled(fillEnabledCheckbox.isSelected() && enabled);
        applyCurrentStyle();
    }

    public void setStrokeWidth(float width) {
        strokeWidthSpinner.setValue((double) width);
        applyCurrentStyle();
    }

    public void setLineStyle(LineStyle lineStyle) {
        lineStyleComboBox.setSelectedItem(lineStyle);
        applyCurrentStyle();
    }

    public void setFillColor(Color color) {
        fillColorButton.setBackground(color);
        applyCurrentStyle();
    }

    public void setGradientEndColor(Color color) {
        fillSecondaryButton.setBackground(color);
        applyCurrentStyle();
    }

    public void setStrokeColor(Color color) {
        strokeColorButton.setBackground(color);
        applyCurrentStyle();
    }

    private void setupListeners() {
        fillEnabledCheckbox.addActionListener(e -> {
            if (isUpdating) return;
            fillColorButton.setEnabled(fillEnabledCheckbox.isSelected());
            gradientCheckbox.setEnabled(fillEnabledCheckbox.isSelected());
            fillSecondaryButton.setEnabled(fillEnabledCheckbox.isSelected() && gradientCheckbox.isSelected());
            applyCurrentStyle();
        });

        gradientCheckbox.addActionListener(e -> {
            if (isUpdating) return;
            fillSecondaryButton.setEnabled(fillEnabledCheckbox.isSelected() && gradientCheckbox.isSelected());
            applyCurrentStyle();
        });

        fillColorButton.addActionListener(e -> chooseFillColor());
        fillSecondaryButton.addActionListener(e -> chooseFillSecondaryColor());
        strokeColorButton.addActionListener(e -> chooseStrokeColor());

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
            drawingPanel.setCurrentFillColor(fillColorButton.getBackground());
            drawingPanel.setCurrentFillSecondaryColor(fillSecondaryButton.getBackground());
            drawingPanel.setCurrentGradientFill(gradientCheckbox.isSelected());
            drawingPanel.setCurrentStrokeColor(strokeColorButton.getBackground());
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
        shape.setFillColor(fillColorButton.getBackground());
        shape.setFillSecondaryColor(fillSecondaryButton.getBackground());
        shape.setGradientFill(gradientCheckbox.isSelected());
        shape.setStrokeColor(strokeColorButton.getBackground());
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

    private JButton createColorButton() {
        JButton button = new JButton();
        button.setPreferredSize(new Dimension(40, 26));
        button.setOpaque(true);
        button.setBorderPainted(true);
        button.setFocusPainted(false);
        button.setText("");
        button.setBorder(BorderFactory.createLineBorder(BORDER));
        return button;
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
