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
    private Color fillSecondaryColor;
    private boolean fillEnabled;
    private boolean gradientFill;
    private Color strokeColor;
    private float strokeWidth;
    private LineStyle lineStyle;
    private boolean selected;

    private double rotation;
    private double scaleX;
    private double scaleY;
    private double skewX;
    private double skewY;
    private boolean reflected;
    private int reflectDirection;  // 0=none, 1=horizontal (Kiri/Kanan), 2=vertical (Atas/Bawah)
    private LineStyle originalLineStyle;
    
    // Animasi
    private AnimationType animationType;
    private double animSpeedX;
    private double animSpeedY;
    private double pulsePhase;
    
    // Batas canvas (di-set dari DrawingPanel)
    private static Rectangle canvasBounds;

    public ShapeObject(int id, ToolType type, int x, int y, int width, int height) {
        this(id, type, x, y, width, height, new Color(115, 166, 255), Color.BLACK);
        this.fillEnabled = false;
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
        this(id, type, x, y, width, height, fillColor, strokeColor, true, false,
                fillColor, 2.0f, LineStyle.SOLID);
    }

    public ShapeObject(
            int id,
            ToolType type,
            int x,
            int y,
            int width,
            int height,
            Color fillColor,
            Color strokeColor,
            boolean fillEnabled,
            boolean gradientFill,
            Color fillSecondaryColor,
            float strokeWidth,
            LineStyle lineStyle
    ) {
        this.id = id;
        this.type = type;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.fillColor = fillColor;
        this.fillSecondaryColor = fillSecondaryColor != null ? fillSecondaryColor : fillColor;
        this.fillEnabled = fillEnabled;
        this.gradientFill = gradientFill;
        this.strokeColor = strokeColor;
        this.strokeWidth = strokeWidth;
        this.lineStyle = lineStyle != null ? lineStyle : LineStyle.SOLID;
        this.selected = false;
        this.rotation = 0.0;
        this.scaleX = 1.0;
        this.scaleY = 1.0;
        this.skewX = 0.0;
        this.skewY = 0.0;
        this.reflected = false;
        this.reflectDirection = 0;
        this.originalLineStyle = this.lineStyle;
        this.animationType = AnimationType.NONE;
        this.animSpeedX = 3.0;
        this.animSpeedY = 3.0;
        this.pulsePhase = 0.0;
    }
    
    public static void setCanvasBounds(Rectangle bounds) {
        canvasBounds = bounds;
    }

    public Shape getTransformedShape() {
        return getTransform().createTransformedShape(createBaseShape());
    }

    public AffineTransform getTransform() {
        AffineTransform transform = new AffineTransform();

        transform.translate(x, y);

        double centerX = width / 2.0;
        double centerY = height / 2.0;

        transform.rotate(Math.toRadians(rotation), centerX, centerY);

        transform.translate(centerX, centerY);
        transform.scale(scaleX, scaleY);
        transform.translate(-centerX, -centerY);

        transform.translate(centerX, centerY);
        transform.shear(skewX, skewY);
        transform.translate(-centerX, -centerY);

        return transform;
    }

    /**
     * Mendapatkan shape bayangan refleksi yang memperhatikan batas kanvas.
     */
    public Shape getReflectionShape() {
        if (!reflected || reflectDirection == 0 || canvasBounds == null) return null;
        return getReflectionTransform().createTransformedShape(createBaseShape());
    }

    /**
     * Mendapatkan transformasi refleksi dengan deteksi batas kanvas yang akurat
     * untuk kebutuhan rendering maupun hit-testing objek bayangan.
     */
    public AffineTransform getReflectionTransform() {
        if (!reflected || reflectDirection == 0) {
            return getTransform();
        }
        
        Shape transformedShape = getTransform().createTransformedShape(createBaseShape());
        Rectangle objBounds = transformedShape.getBounds();
        AffineTransform reflectTx = new AffineTransform();
        
        if (reflectDirection == 1) { 
            // HORIZONTAL: Refleksi terhadap sumbu Y -> Mengubah posisi X (Kiri/Kanan)
            double L; // Garis cermin vertikal X = L
            if (canvasBounds != null) {
                // Cek apakah muat jika ditaruh di sebelah kanan objek asli
                if (objBounds.x + 2 * objBounds.width <= canvasBounds.x + canvasBounds.width) {
                    L = objBounds.x + objBounds.width; // Cermin di sisi kanan objek
                } else if (objBounds.x - objBounds.width >= canvasBounds.x) {
                    L = objBounds.x; // Cermin di sisi kiri objek
                } else {
                    // Jika terpaksa tidak muat di kedua sisi, pilih area yang spacenya lebih luas
                    int spaceRight = (canvasBounds.x + canvasBounds.width) - (objBounds.x + objBounds.width);
                    int spaceLeft = objBounds.x - canvasBounds.x;
                    L = (spaceRight >= spaceLeft) ? (objBounds.x + objBounds.width) : objBounds.x;
                }
            } else {
                L = objBounds.x + objBounds.width;
            }
            // Rumus pencerminan koordinat X: X' = -X + 2L
            reflectTx.translate(2 * L, 0);
            reflectTx.scale(-1, 1);
            
        } else if (reflectDirection == 2) { 
            // VERTICAL: Refleksi terhadap sumbu X -> Mengubah posisi Y (Atas/Bawah)
            double L; // Garis cermin horizontal Y = L
            if (canvasBounds != null) {
                // Cek apakah muat jika ditaruh di sebelah bawah objek asli
                if (objBounds.y + 2 * objBounds.height <= canvasBounds.y + canvasBounds.height) {
                    L = objBounds.y + objBounds.height; // Cermin di sisi bawah objek
                } else if (objBounds.y - objBounds.height >= canvasBounds.y) {
                    L = objBounds.y; // Cermin di sisi atas objek
                } else {
                    // Jika terpaksa tidak muat di kedua sisi, pilih area yang spacenya lebih luas
                    int spaceBottom = (canvasBounds.y + canvasBounds.height) - (objBounds.y + objBounds.height);
                    int spaceTop = objBounds.y - canvasBounds.y;
                    L = (spaceBottom >= spaceTop) ? (objBounds.y + objBounds.height) : objBounds.y;
                }
            } else {
                L = objBounds.y + objBounds.height;
            }
            // Rumus pencerminan koordinat Y: Y' = -Y + 2L
            reflectTx.translate(0, 2 * L);
            reflectTx.scale(1, -1);
        }
        
        // Gabungkan transform refleksi dunia dengan transform lokal bawaan objek asli
        reflectTx.concatenate(getTransform());
        return reflectTx;
    }

    private Shape createBaseShape() {
        int width = this.width;
        int height = this.height;

        switch (this.type) {
            case RECTANGLE:
                return new Rectangle(0, 0, width, height);
            case CIRCLE:
                return new Ellipse2D.Double(0, 0, width, height);
            case TRIANGLE:
                Polygon triangle = new Polygon();
                triangle.addPoint(width / 2, 0);
                triangle.addPoint(0, height);
                triangle.addPoint(width, height);
                return triangle;
            case PARALLELOGRAM:
                Polygon parallelogram = new Polygon();
                int offset = Math.max(1, Math.abs(width) / 4);
                parallelogram.addPoint(offset, 0);
                parallelogram.addPoint(width, 0);
                parallelogram.addPoint(width - offset, height);
                parallelogram.addPoint(0, height);
                return parallelogram;
            case STAR:
                return createStar(width, height);
            case LINE:
                return new Line2D.Double(0, 0, width, height);
            default:
                return new Rectangle(0, 0, width, height);
        }
    }

    private Shape createStar(int width, int height) {
        Polygon star = new Polygon();
        double centerX = width / 2.0;
        double centerY = height / 2.0;
        double outerRadius = Math.min(Math.abs(width), Math.abs(height)) / 2.0;
        double innerRadius = outerRadius * 0.45;

        for (int i = 0; i < 10; i++) {
            double angle = -Math.PI / 2.0 + i * Math.PI / 5.0;
            double radius = (i % 2 == 0) ? outerRadius : innerRadius;
            int px = (int) Math.round(centerX + Math.cos(angle) * radius);
            int py = (int) Math.round(centerY + Math.sin(angle) * radius);
            star.addPoint(px, py);
        }
        return star;
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
            AffineTransform tx = checkReflected ? getReflectionTransform() : getTransform();
            Point2D p1 = new Point2D.Double(0, 0);
            Point2D p2 = new Point2D.Double(width, height);
            Point2D tp1 = tx.transform(p1, null);
            Point2D tp2 = tx.transform(p2, null);
            return distanceToLine(pointX, pointY, tp1.getX(), tp1.getY(), tp2.getX(), tp2.getY()) <= 6.0;
        }

        Shape s = checkReflected ? 
            getReflectionTransform().createTransformedShape(createBaseShape()) : 
            getTransformedShape();
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

    // Getter dan Setter
    public int getId() { return id; }
    public ToolType getType() { return type; }
    public int getX() { return x; }
    public void setX(int x) { this.x = x; }
    public int getY() { return y; }
    public void setY(int y) { this.y = y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public Color getFillColor() { return fillColor; }
    public void setFillColor(Color fillColor) { this.fillColor = fillColor; }
    public Color getFillSecondaryColor() { return fillSecondaryColor; }
    public void setFillSecondaryColor(Color fillSecondaryColor) { this.fillSecondaryColor = fillSecondaryColor; }
    public boolean isFillEnabled() { return fillEnabled; }
    public void setFillEnabled(boolean fillEnabled) { this.fillEnabled = fillEnabled; }
    public boolean isGradientFill() { return gradientFill; }
    public void setGradientFill(boolean gradientFill) { this.gradientFill = gradientFill; }
    public Color getStrokeColor() { return strokeColor; }
    public void setStrokeColor(Color strokeColor) { this.strokeColor = strokeColor; }
    public float getStrokeWidth() { return strokeWidth; }
    public void setStrokeWidth(float strokeWidth) { this.strokeWidth = strokeWidth; }
    public LineStyle getLineStyle() { return lineStyle; }
    
    public void setLineStyle(LineStyle lineStyle) {
        this.lineStyle = lineStyle != null ? lineStyle : LineStyle.SOLID;
    }
    
    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }
    public double getRotation() { return rotation; }
    public void setRotation(double rotation) { this.rotation = rotation; }
    public double getScaleX() { return scaleX; }
    public void setScaleX(double scaleX) { this.scaleX = scaleX; }
    public double getScaleY() { return scaleY; }
    public void setScaleY(double scaleY) { this.scaleY = scaleY; }
    public double getSkewX() { return skewX; }
    public void setSkewX(double skewX) { this.skewX = skewX; }
    public double getSkewY() { return skewY; }
    public void setSkewY(double skewY) { this.skewY = skewY; }
    public boolean isReflected() { return reflected; }
    public int getReflectDirection() { return reflectDirection; }
    public void setReflectDirection(int reflectDirection) { this.reflectDirection = reflectDirection; }
    
    public void setReflected(boolean reflected) {
        if (reflected && !this.reflected) {
            this.originalLineStyle = this.lineStyle;
        } else if (!reflected && this.reflected) {
            this.lineStyle = this.originalLineStyle;
        }
        this.reflected = reflected;
    }

    public AnimationType getAnimationType() {
        return animationType;
    }

    public void setAnimationType(AnimationType animationType) {
        this.animationType = animationType != null ? animationType : AnimationType.NONE;
    }

    public double getAnimSpeedX() {
        return animSpeedX;
    }

    public void setAnimSpeedX(double animSpeedX) {
        this.animSpeedX = animSpeedX;
    }

    public double getAnimSpeedY() {
        return animSpeedY;
    }

    public void setAnimSpeedY(double animSpeedY) {
        this.animSpeedY = animSpeedY;
    }

    public double getPulsePhase() {
        return pulsePhase;
    }

    public void setPulsePhase(double pulsePhase) {
        this.pulsePhase = pulsePhase;
    }
}