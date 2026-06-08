import java.awt.Color;
import java.awt.Shape;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

public class ShapeObject {

    private final int id;
    private final ToolType type;

    private int x;
    private int y;
    private int width;
    private int height;

    private Color fillColor;
    private Color strokeColor;
    private boolean selected;

    private double rotation;
    private double scaleX;
    private double scaleY;
    private double skewX;
    private double skewY;
    private boolean reflected;

    public ShapeObject(int id, ToolType type, int x, int y, int width, int height) {
        this(id, type, x, y, width, height, new Color(115, 166, 255), Color.BLACK);
    }

    public ShapeObject(
            int id,
            ToolType type,
            int x,
            int y,
            int width,
            int height,
            Color fillColor,
            Color strokeColor
    ) {
        this.id = id;
        this.type = type;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.fillColor = fillColor;
        this.strokeColor = strokeColor;
        this.selected = false;
        this.rotation = 0.0;
        this.scaleX = 1.0;
        this.scaleY = 1.0;
        this.skewX = 0.0;
        this.skewY = 0.0;
        this.reflected = false;
    }

    public Shape getTransformedShape() {
        Shape baseShape = createBaseShape();
        double centerX = x + width / 2.0;
        double centerY = y + height / 2.0;

        AffineTransform transform = new AffineTransform();

        // rotation
        transform.rotate(Math.toRadians(rotation), centerX, centerY);

        // scale
        transform.translate(centerX, centerY);
        transform.scale(scaleX, scaleY);
        transform.translate(-centerX, -centerY);

        // skew/shear
        transform.translate(centerX, centerY);
        transform.shear(skewX, skewY);
        transform.translate(-centerX, -centerY);

        return transform.createTransformedShape(baseShape);
    }

    public AffineTransform getReflectionTransform() {
        double centerX = x + width / 2.0;
        double centerY = y + height / 2.0;
        double axisX = x + width; // axis of reflection is the right edge of the shape

        AffineTransform reflectTransform = new AffineTransform();

        // Base transform (rotation, scale, shear)
        AffineTransform transform = new AffineTransform();
        transform.rotate(Math.toRadians(rotation), centerX, centerY);
        transform.translate(centerX, centerY);
        transform.scale(scaleX, scaleY);
        transform.translate(-centerX, -centerY);
        transform.translate(centerX, centerY);
        transform.shear(skewX, skewY);
        transform.translate(-centerX, -centerY);

        reflectTransform.concatenate(transform);

        // Apply horizontal reflection across axisX
        reflectTransform.translate(axisX, centerY);
        reflectTransform.scale(-1, 1);
        reflectTransform.translate(-axisX, -centerY);

        return reflectTransform;
    }

    private Shape createBaseShape() {
        int x = this.x;
        int y = this.y;
        int width = this.width;
        int height = this.height;

        switch (this.type) {
            case RECTANGLE:
                return new Rectangle(x, y, width, height);
            case CIRCLE:
                return new Ellipse2D.Double(x, y, width, height);
            case TRIANGLE:
                Polygon triangle = new Polygon();
                triangle.addPoint(x + width / 2, y);
                triangle.addPoint(x, y + height);
                triangle.addPoint(x + width, y + height);
                return triangle;
            case LINE:
                return new Line2D.Double(x, y, x + width, y + height);
            default:
                return new Rectangle(x, y, width, height);
        }
    }

    public boolean contains(int pointX, int pointY) {
        if (containsInShape(pointX, pointY, false)) {
            return true;
        }
        if (reflected && containsInShape(pointX, pointY, true)) {
            return true;
        }
        return false;
    }

    private boolean containsInShape(int pointX, int pointY, boolean checkReflected) {
        if (type == ToolType.LINE) {
            AffineTransform tx;
            if (checkReflected) {
                tx = getReflectionTransform();
            } else {
                double centerX = x + width / 2.0;
                double centerY = y + height / 2.0;
                tx = new AffineTransform();
                tx.rotate(Math.toRadians(rotation), centerX, centerY);
                tx.translate(centerX, centerY);
                tx.scale(scaleX, scaleY);
                tx.translate(-centerX, -centerY);
                tx.translate(centerX, centerY);
                tx.shear(skewX, skewY);
                tx.translate(-centerX, -centerY);
            }
            Point2D p1 = new Point2D.Double(x, y);
            Point2D p2 = new Point2D.Double(x + width, y + height);
            Point2D tp1 = tx.transform(p1, null);
            Point2D tp2 = tx.transform(p2, null);
            return distanceToLine(pointX, pointY, tp1.getX(), tp1.getY(), tp2.getX(), tp2.getY()) <= 6.0;
        }

        Shape s;
        if (checkReflected) {
            s = getReflectionTransform().createTransformedShape(createBaseShape());
        } else {
            s = getTransformedShape();
        }
        return s.contains(pointX, pointY);
    }

    public void moveBy(int deltaX, int deltaY) {
        x += deltaX;
        y += deltaY;
    }

    private double distanceToLine(double px, double py, double sx, double sy, double ex, double ey) {
        double dx = ex - sx;
        double dy = ey - sy;
        double lineLengthSquared = dx * dx + dy * dy;

        if (lineLengthSquared == 0) {
            return Math.hypot(px - sx, py - sy);
        }

        double position = ((px - sx) * dx + (py - sy) * dy) / lineLengthSquared;
        position = Math.max(0, Math.min(1, position));

        double projX = sx + position * dx;
        double projY = sy + position * dy;

        return Math.hypot(px - projX, py - projY);
    }

    public int getId() {
        return id;
    }

    public ToolType getType() {
        return type;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Color getFillColor() {
        return fillColor;
    }

    public void setFillColor(Color fillColor) {
        this.fillColor = fillColor;
    }

    public Color getStrokeColor() {
        return strokeColor;
    }

    public void setStrokeColor(Color strokeColor) {
        this.strokeColor = strokeColor;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public double getRotation() {
        return rotation;
    }

    public void setRotation(double rotation) {
        this.rotation = rotation;
    }

    public double getScaleX() {
        return scaleX;
    }

    public void setScaleX(double scaleX) {
        this.scaleX = scaleX;
    }

    public double getScaleY() {
        return scaleY;
    }

    public void setScaleY(double scaleY) {
        this.scaleY = scaleY;
    }

    public double getSkewX() {
        return skewX;
    }

    public void setSkewX(double skewX) {
        this.skewX = skewX;
    }

    public double getSkewY() {
        return skewY;
    }

    public void setSkewY(double skewY) {
        this.skewY = skewY;
    }

    public boolean isReflected() {
        return reflected;
    }

    public void setReflected(boolean reflected) {
        this.reflected = reflected;
    }
}
