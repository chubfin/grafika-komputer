import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
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

public class PropertyPanel extends JPanel {

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
    private final JCheckBox reflectedCheckbox;

    private ShapeObject currentShape;
    private Runnable onChangeCallback;

    private boolean isUpdating = false;

    public PropertyPanel(Runnable onChangeCallback) {
        this.onChangeCallback = onChangeCallback;

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        setPreferredSize(new Dimension(220, 0));

        JLabel titleLabel = new JLabel("Properties");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        add(titleLabel, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new GridBagLayout());
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);

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

        reflectedCheckbox = new JCheckBox();

        int row = 0;

        addSectionLabel(contentPanel, row++, "── Info ──");
        addLabelRow(contentPanel, row++, "Type", typeValue);
        addLabelRow(contentPanel, row++, "Width", widthValue);
        addLabelRow(contentPanel, row++, "Height", heightValue);

        addSectionLabel(contentPanel, row++, "── Translasi ──");
        addSpinnerRow(contentPanel, row++, "X", xSpinner);
        addSpinnerRow(contentPanel, row++, "Y", ySpinner);

        addSectionLabel(contentPanel, row++, "── Transformasi ──");
        addSpinnerRow(contentPanel, row++, "Rotation (°)", rotationSpinner);
        addSpinnerRow(contentPanel, row++, "Scale X", scaleXSpinner);
        addSpinnerRow(contentPanel, row++, "Scale Y", scaleYSpinner);
        addSpinnerRow(contentPanel, row++, "Skew X", skewXSpinner);
        addSpinnerRow(contentPanel, row++, "Skew Y", skewYSpinner);
        addCheckboxRow(contentPanel, row++, "Reflection", reflectedCheckbox);

        setupListeners();
    }

    private void setupListeners() {
        ChangeListener listener = e -> {
            if (currentShape == null || isUpdating) return;
            currentShape.setX((Integer) xSpinner.getValue());
            currentShape.setY((Integer) ySpinner.getValue());
            currentShape.setRotation((Double) rotationSpinner.getValue());
            currentShape.setScaleX((Double) scaleXSpinner.getValue());
            currentShape.setScaleY((Double) scaleYSpinner.getValue());
            currentShape.setSkewX((Double) skewXSpinner.getValue());
            currentShape.setSkewY((Double) skewYSpinner.getValue());
            onChangeCallback.run();
         };

        xSpinner.addChangeListener(listener);
        ySpinner.addChangeListener(listener);
        rotationSpinner.addChangeListener(listener);
        scaleXSpinner.addChangeListener(listener);
        scaleYSpinner.addChangeListener(listener);
        skewXSpinner.addChangeListener(listener);
        skewYSpinner.addChangeListener(listener);

        reflectedCheckbox.addActionListener(e -> {
            if (isUpdating || currentShape == null) return;
            currentShape.setReflected(reflectedCheckbox.isSelected());
            onChangeCallback.run();
        });
    }

    public void showShape(ShapeObject shape) {
        isUpdating = true;

        currentShape = shape;

        if (shape == null) {
            typeValue.setText("-");
            widthValue.setText("-");
            heightValue.setText("-");
            xSpinner.setValue(0);
            ySpinner.setValue(0);
            rotationSpinner.setValue(0.0);
            scaleXSpinner.setValue(1.0);
            scaleYSpinner.setValue(1.0);
            skewXSpinner.setValue(0.0);
            skewYSpinner.setValue(0.0);
            reflectedCheckbox.setSelected(false);
        } else {
            typeValue.setText(shape.getType().getDisplayName());
            widthValue.setText(String.valueOf(shape.getWidth()));
            heightValue.setText(String.valueOf(shape.getHeight()));
            xSpinner.setValue(shape.getX());
            ySpinner.setValue(shape.getY());
            rotationSpinner.setValue(shape.getRotation());
            scaleXSpinner.setValue(shape.getScaleX());
            scaleYSpinner.setValue(shape.getScaleY());
            skewXSpinner.setValue(shape.getSkewX());
            skewYSpinner.setValue(shape.getSkewY());
            reflectedCheckbox.setSelected(shape.isReflected());
        }

        isUpdating = false;
    }

    private void addSectionLabel(JPanel panel, int row, String text) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 0, 4, 0);
        JLabel label = new JLabel(text);
        label.setForeground(Color.GRAY);
        panel.add(label, gbc);
    }

    private void addLabelRow(JPanel panel, int row, String label, JLabel valueLabel) {
        GridBagConstraints labelGbc = new GridBagConstraints();
        labelGbc.gridx = 0;
        labelGbc.gridy = row;
        labelGbc.anchor = GridBagConstraints.WEST;
        labelGbc.insets = new Insets(4, 0, 4, 12);
        panel.add(new JLabel(label), labelGbc);

        GridBagConstraints valueGbc = new GridBagConstraints();
        valueGbc.gridx = 1;
        valueGbc.gridy = row;
        valueGbc.weightx = 1.0;
        valueGbc.anchor = GridBagConstraints.WEST;
        valueGbc.insets = new Insets(4, 0, 4, 0);
        panel.add(valueLabel, valueGbc);
    }

    private void addSpinnerRow(JPanel panel, int row, String label, JSpinner spinner) {
        GridBagConstraints labelGbc = new GridBagConstraints();
        labelGbc.gridx = 0;
        labelGbc.gridy = row;
        labelGbc.anchor = GridBagConstraints.WEST;
        labelGbc.insets = new Insets(4, 0, 4, 12);
        panel.add(new JLabel(label), labelGbc);

        GridBagConstraints spinnerGbc = new GridBagConstraints();
        spinnerGbc.gridx = 1;
        spinnerGbc.gridy = row;
        spinnerGbc.weightx = 1.0;
        spinnerGbc.fill = GridBagConstraints.HORIZONTAL;
        spinnerGbc.insets = new Insets(4, 0, 4, 0);
        panel.add(spinner, spinnerGbc);
    }

    private void addCheckboxRow(JPanel panel, int row, String label, JCheckBox checkbox) {
        GridBagConstraints labelGbc = new GridBagConstraints();
        labelGbc.gridx = 0;
        labelGbc.gridy = row;
        labelGbc.anchor = GridBagConstraints.WEST;
        labelGbc.insets = new Insets(4, 0, 4, 12);
        panel.add(new JLabel(label), labelGbc);

        GridBagConstraints checkGbc = new GridBagConstraints();
        checkGbc.gridx = 1;
        checkGbc.gridy = row;
        checkGbc.anchor = GridBagConstraints.WEST;
        checkGbc.insets = new Insets(4, 0, 4, 0);
        panel.add(checkbox, checkGbc);
    }
}
