import java.awt.Color;

public class ShapeState {
    private final int x;
    private final int y;
    private final Color fillColor;
    private final Color fillSecondaryColor;
    private final boolean fillEnabled;
    private final boolean gradientFill;
    private final Color strokeColor;
    private final float strokeWidth;
    private final LineStyle lineStyle;
    private final double rotation;
    private final double scaleX;
    private final double scaleY;
    private final double skewX;
    private final double skewY;
    private final boolean reflected;
    private final int reflectDirection;
    private final boolean explicitReflectionAxis;
    private final double reflectionAxisX;
    private final double reflectionAxisY;
    private final double reflectionAxisAngle;
    private final AnimationType animationType;
    private final double animSpeedX;
    private final double animSpeedY;
    private final double pulsePhase;

    public ShapeState(ShapeObject shape) {
        this.x = shape.getX();
        this.y = shape.getY();
        this.fillColor = shape.getFillColor();
        this.fillSecondaryColor = shape.getFillSecondaryColor();
        this.fillEnabled = shape.isFillEnabled();
        this.gradientFill = shape.isGradientFill();
        this.strokeColor = shape.getStrokeColor();
        this.strokeWidth = shape.getStrokeWidth();
        this.lineStyle = shape.getLineStyle();
        this.rotation = shape.getRotation();
        this.scaleX = shape.getScaleX();
        this.scaleY = shape.getScaleY();
        this.skewX = shape.getSkewX();
        this.skewY = shape.getSkewY();
        this.reflected = shape.isReflected();
        this.reflectDirection = shape.getReflectDirection();
        this.explicitReflectionAxis = shape.hasExplicitReflectionAxis();
        this.reflectionAxisX = shape.getReflectionAxisX();
        this.reflectionAxisY = shape.getReflectionAxisY();
        this.reflectionAxisAngle = shape.getReflectionAxisAngle();
        this.animationType = shape.getAnimationType();
        this.animSpeedX = shape.getAnimSpeedX();
        this.animSpeedY = shape.getAnimSpeedY();
        this.pulsePhase = shape.getPulsePhase();
    }

    public void applyTo(ShapeObject shape) {
        shape.setX(x);
        shape.setY(y);
        shape.setFillColor(fillColor);
        shape.setFillSecondaryColor(fillSecondaryColor);
        shape.setFillEnabled(fillEnabled);
        shape.setGradientFill(gradientFill);
        shape.setStrokeColor(strokeColor);
        shape.setStrokeWidth(strokeWidth);
        shape.setLineStyle(lineStyle);
        shape.setRotation(rotation);
        shape.setScaleX(scaleX);
        shape.setScaleY(scaleY);
        shape.setSkewX(skewX);
        shape.setSkewY(skewY);
        shape.setReflected(reflected);
        shape.setReflectDirection(reflectDirection);
        if (explicitReflectionAxis) {
            shape.setReflectionAxisLine(reflectionAxisX, reflectionAxisY, reflectionAxisAngle);
        } else {
            shape.clearReflectionAxisLine();
        }
        shape.setAnimationType(animationType);
        shape.setAnimSpeedX(animSpeedX);
        shape.setAnimSpeedY(animSpeedY);
        shape.setPulsePhase(pulsePhase);
    }
}
