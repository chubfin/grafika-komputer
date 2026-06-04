import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public class PropertyPanel extends JPanel {

    private final JLabel typeValue;
    private final JLabel xValue;
    private final JLabel yValue;
    private final JLabel widthValue;
    private final JLabel heightValue;

    public PropertyPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel titleLabel = new JLabel("Properties");
        add(titleLabel, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new GridBagLayout());
        add(contentPanel, BorderLayout.CENTER);

        typeValue = new JLabel("-");
        xValue = new JLabel("-");
        yValue = new JLabel("-");
        widthValue = new JLabel("-");
        heightValue = new JLabel("-");

        addPropertyRow(contentPanel, 0, "Object Type", typeValue);
        addPropertyRow(contentPanel, 1, "X", xValue);
        addPropertyRow(contentPanel, 2, "Y", yValue);
        addPropertyRow(contentPanel, 3, "Width", widthValue);
        addPropertyRow(contentPanel, 4, "Height", heightValue);
    }

    public void showShape(ShapeObject shape) {
        if (shape == null) {
            typeValue.setText("-");
            xValue.setText("-");
            yValue.setText("-");
            widthValue.setText("-");
            heightValue.setText("-");
            return;
        }

        typeValue.setText(shape.getType().getDisplayName());
        xValue.setText(String.valueOf(shape.getX()));
        yValue.setText(String.valueOf(shape.getY()));
        widthValue.setText(String.valueOf(shape.getWidth()));
        heightValue.setText(String.valueOf(shape.getHeight()));
    }

    private void addPropertyRow(JPanel panel, int row, String label, JLabel valueLabel) {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.anchor = GridBagConstraints.WEST;
        labelConstraints.insets = new Insets(8, 0, 8, 12);
        panel.add(new JLabel(label), labelConstraints);

        GridBagConstraints valueConstraints = new GridBagConstraints();
        valueConstraints.gridx = 1;
        valueConstraints.gridy = row;
        valueConstraints.weightx = 1.0;
        valueConstraints.anchor = GridBagConstraints.WEST;
        valueConstraints.insets = new Insets(8, 0, 8, 0);
        panel.add(valueLabel, valueConstraints);
    }
}
