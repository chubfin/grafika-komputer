import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;

public class MainFrame extends JFrame {

    private final ShapeManager shapeManager;
    private final DrawingPanel drawingPanel;
    private final PropertyPanel propertyPanel;
    private final JLabel statusLabel;
    private JButton fillColorButton;
    private JButton strokeColorButton;

    public MainFrame() {
        shapeManager = new ShapeManager();
        propertyPanel = new PropertyPanel();
        statusLabel = new JLabel("Ready");
        drawingPanel = new DrawingPanel(shapeManager, this::onSelectionChanged);

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
        menuBar.add(new JMenu("File"));
        menuBar.add(new JMenu("Edit"));
        menuBar.add(new JMenu("Transform"));
        menuBar.add(createStyleMenu());
        return menuBar;
    }

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

    private JPanel createToolBar() {
        JPanel toolBar = new JPanel(new GridLayout(6, 1, 6, 6));
        toolBar.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        toolBar.setPreferredSize(new Dimension(130, 0));

        ButtonGroup toolGroup = new ButtonGroup();
        addToolButton(toolBar, toolGroup, ToolType.RECTANGLE, true);
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

    private void chooseFillColor() {
        Color selectedColor = JColorChooser.showDialog(this, "Choose Fill Color", drawingPanel.getCurrentFillColor());
        if (selectedColor == null) {
            return;
        }

        drawingPanel.setCurrentFillColor(selectedColor);
        fillColorButton.setBackground(selectedColor);
        applyColorToSelectedShape(true, selectedColor);
        statusLabel.setText("Fill color updated");
    }

    private void chooseStrokeColor() {
        Color selectedColor = JColorChooser.showDialog(this, "Choose Stroke Color", drawingPanel.getCurrentStrokeColor());
        if (selectedColor == null) {
            return;
        }

        drawingPanel.setCurrentStrokeColor(selectedColor);
        strokeColorButton.setBackground(selectedColor);
        applyColorToSelectedShape(false, selectedColor);
        statusLabel.setText("Stroke color updated");
    }

    private void applyColorToSelectedShape(boolean fillColor, Color selectedColor) {
        ShapeObject selectedShape = shapeManager.getSelectedShape();
        if (selectedShape == null) {
            return;
        }

        if (fillColor) {
            selectedShape.setFillColor(selectedColor);
        } else {
            selectedShape.setStrokeColor(selectedColor);
        }

        drawingPanel.repaint();
    }

    private JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        statusBar.add(statusLabel, BorderLayout.WEST);
        return statusBar;
    }

    private void onSelectionChanged(ShapeObject selectedShape) {
        propertyPanel.showShape(selectedShape);

        if (selectedShape == null) {
            statusLabel.setText("No object selected");
            return;
        }

        statusLabel.setText("Selected: " + selectedShape.getType().getDisplayName() + " #" + selectedShape.getId());
    }
}
