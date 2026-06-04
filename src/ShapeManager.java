import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ShapeManager {

    private final ArrayList<ShapeObject> shapes;
    private int nextId;

    public ShapeManager() {
        this.shapes = new ArrayList<>();
        this.nextId = 1;
    }

    public ShapeObject createShape(ToolType type, int x, int y, int width, int height) {
        return createShape(type, x, y, width, height, new Color(115, 166, 255), Color.BLACK);
    }

    public ShapeObject createShape(
            ToolType type,
            int x,
            int y,
            int width,
            int height,
            Color fillColor,
            Color strokeColor
    ) {
        ShapeObject shape = new ShapeObject(nextId++, type, x, y, width, height, fillColor, strokeColor);
        shapes.add(shape);
        selectShape(shape);
        return shape;
    }

    public List<ShapeObject> getShapes() {
        return Collections.unmodifiableList(shapes);
    }

    public ShapeObject getSelectedShape() {
        for (ShapeObject shape : shapes) {
            if (shape.isSelected()) {
                return shape;
            }
        }

        return null;
    }

    public ShapeObject findShapeAt(int x, int y) {
        for (int i = shapes.size() - 1; i >= 0; i--) {
            ShapeObject shape = shapes.get(i);
            if (shape.contains(x, y)) {
                return shape;
            }
        }

        return null;
    }

    public void selectShape(ShapeObject selectedShape) {
        for (ShapeObject shape : shapes) {
            shape.setSelected(shape == selectedShape);
        }
    }

    public void clearSelection() {
        selectShape(null);
    }

    public void deleteSelectedShape() {
        ShapeObject selectedShape = getSelectedShape();
        if (selectedShape != null) {
            shapes.remove(selectedShape);
        }
    }
}
