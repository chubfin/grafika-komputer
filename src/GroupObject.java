import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * GroupObject - Representasi sekumpulan ShapeObject yang diperlakukan sebagai satu entitas.
 * Dibuat oleh: Anggota 4
 *
 * GroupObject mewarisi ShapeObject dengan posisi/ukuran sebagai bounding box
 * gabungan dari semua member-nya. Saat dipindah, semua member ikut bergerak.
 */
public class GroupObject extends ShapeObject {

    private final List<ShapeObject> members;

    public GroupObject(int id, List<ShapeObject> members) {
        super(id, ToolType.SELECT,
                computeBounds(members).x,
                computeBounds(members).y,
                computeBounds(members).width,
                computeBounds(members).height,
                new Color(0, 0, 0, 0),   // fill transparan
            Color.BLUE,              // stroke biru menandai group
            false,
            false,
            new Color(0, 0, 0, 0),
            2.0f,
            LineStyle.SOLID
        );
        this.members = new ArrayList<>(members);
        // Pastikan semua member tidak "selected" individual
        for (ShapeObject m : this.members) {
            m.setSelected(false);
        }
    }

    public List<ShapeObject> getMembers() {
        return Collections.unmodifiableList(members);
    }

    /** Hitung bounding box dari semua member. */
    private static Rectangle computeBounds(List<ShapeObject> members) {
        if (members.isEmpty()) {
            return new Rectangle(0, 0, 10, 10);
        }
        Rectangle bounds = null;
        for (ShapeObject s : members) {
            Rectangle b = s.getTransformedShape().getBounds();
            bounds = (bounds == null) ? b : bounds.union(b);
        }
        return bounds;
    }

    /**
     * Saat group dipindah, semua member ikut bergerak.
     */
    @Override
    public void moveBy(int deltaX, int deltaY) {
        super.moveBy(deltaX, deltaY);
        for (ShapeObject m : members) {
            m.moveBy(deltaX, deltaY);
        }
    }

    /**
     * Saat group di-reflect, semua member ikut di-reflect.
     */
    @Override
    public void setReflected(boolean reflected) {
        super.setReflected(reflected);
        for (ShapeObject m : members) {
            m.setReflected(reflected);
        }
    }

    @Override
    public void setReflectDirection(int reflectDirection) {
        super.setReflectDirection(reflectDirection);
        for (ShapeObject m : members) {
            m.setReflectDirection(reflectDirection);
        }
    }

    @Override
    public void setReflectionAxisLine(double axisX, double axisY, double angleDegrees) {
        super.setReflectionAxisLine(axisX, axisY, angleDegrees);
        for (ShapeObject m : members) {
            m.setReflectionAxisLine(axisX, axisY, angleDegrees);
        }
    }

    @Override
    public void clearReflectionAxisLine() {
        super.clearReflectionAxisLine();
        for (ShapeObject m : members) {
            m.clearReflectionAxisLine();
        }
    }

    /**
     * Hit-test: cek apakah titik (x,y) ada di dalam salah satu member.
     */
    @Override
    public boolean contains(int pointX, int pointY) {
        for (ShapeObject m : members) {
            if (m.contains(pointX, pointY)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Shape yang digunakan untuk menggambar outline selection group
     * adalah union dari semua bounding box member.
     */
    @Override
    public Shape getTransformedShape() {
        Area area = new Area();
        for (ShapeObject m : members) {
            area.add(new Area(m.getTransformedShape().getBounds2D()));
        }
        return area;
    }

    /**
     * GroupObject tidak punya transform sendiri — identitas.
     */
    @Override
    public AffineTransform getTransform() {
        return new AffineTransform();
    }
}
