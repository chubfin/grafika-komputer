import java.awt.Color;

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

    public boolean contains(int pointX, int pointY) {
        if (type == ToolType.LINE) {
            return distanceToLine(pointX, pointY) <= 6.0;
        }

        int left = Math.min(x, x + width);
        int right = Math.max(x, x + width);
        int top = Math.min(y, y + height);
        int bottom = Math.max(y, y + height);

        return pointX >= left && pointX <= right && pointY >= top && pointY <= bottom;
    }

    public void moveBy(int deltaX, int deltaY) {
        x += deltaX;
        y += deltaY;
    }

    private double distanceToLine(int pointX, int pointY) {
        double startX = x;
        double startY = y;
        double endX = x + width;
        double endY = y + height;
        double lineLengthSquared = Math.pow(endX - startX, 2) + Math.pow(endY - startY, 2);

        if (lineLengthSquared == 0) {
            return Math.hypot(pointX - startX, pointY - startY);
        }

        double position = ((pointX - startX) * (endX - startX) + (pointY - startY) * (endY - startY))
                / lineLengthSquared;
        position = Math.max(0, Math.min(1, position));

        double projectionX = startX + position * (endX - startX);
        double projectionY = startY + position * (endY - startY);

        return Math.hypot(pointX - projectionX, pointY - projectionY);
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

    public int getY() {
        return y;
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
