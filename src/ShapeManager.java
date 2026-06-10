import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ShapeManager {

    private final ArrayList<ShapeObject> shapes;
    private int nextId;

    /**
     * Menyimpan warna irisan untuk setiap pasangan shape.
     * Key: "id1_id2" dengan id1 < id2 agar konsisten.
     */
    private final Map<String, Color> intersectionColors = new HashMap<>();

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
        shape.setFillEnabled(false);
        shapes.add(shape);
        selectShape(shape);
        return shape;
    }

    // -------------------------------------------------------------------------
    // Method pendukung sistem Undo/Redo - Ditambahkan oleh Anggota 4
    // -------------------------------------------------------------------------

    /**
     * Membuat ShapeObject baru TANPA langsung menambahkannya ke daftar.
     * Digunakan oleh DrawingPanel agar penambahan shape bisa dibungkus
     * dalam AddShapeCommand sebelum dieksekusi via HistoryManager.
     */
    public ShapeObject buildShape(
            ToolType type,
            int x,
            int y,
            int width,
            int height,
            Color fillColor,
            Color strokeColor
    ) {
        ShapeObject shape = new ShapeObject(nextId++, type, x, y, width, height, fillColor, strokeColor);
        shape.setFillEnabled(false);
        return shape;
    }

    public List<ShapeObject> getShapes() {
        return Collections.unmodifiableList(shapes);
    }

    /** Mengembalikan shape pertama yang terpilih (kompatibilitas lama). */
    public ShapeObject getSelectedShape() {
        for (ShapeObject shape : shapes) {
            if (shape.isSelected()) {
                return shape;
            }
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Multi-Selection - Ditambahkan oleh Anggota 4 (fitur baru)
    // -------------------------------------------------------------------------

    /** Mengembalikan semua shape yang sedang terpilih. */
    public List<ShapeObject> getSelectedShapes() {
        return shapes.stream()
                .filter(ShapeObject::isSelected)
                .collect(Collectors.toList());
    }

    /**
     * Menambah/menghapus shape dari seleksi (untuk Ctrl+Click).
     * Shape lain yang sudah terpilih tidak diubah.
     */
    public void toggleShapeSelection(ShapeObject shape) {
        shape.setSelected(!shape.isSelected());
    }

    /**
     * Menyeleksi semua shape yang bounding box-nya beririsan dengan
     * area rubber-band (seleksi seret).
     */
    public void selectShapesInRect(int rx, int ry, int rw, int rh) {
        java.awt.Rectangle selRect = new java.awt.Rectangle(
                Math.min(rx, rx + rw), Math.min(ry, ry + rh),
                Math.abs(rw), Math.abs(rh));
        for (ShapeObject shape : shapes) {
            java.awt.Rectangle bounds = shape.getTransformedShape().getBounds();
            shape.setSelected(selRect.intersects(bounds));
        }
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
        for (ShapeObject shape : shapes) {
            shape.setSelected(false);
        }
    }

    public void deleteSelectedShape() {
        shapes.removeIf(ShapeObject::isSelected);
    }

    // -------------------------------------------------------------------------
    // Method pendukung sistem Undo/Redo - Ditambahkan oleh Anggota 4
    // -------------------------------------------------------------------------

    /**
     * Menambahkan shape langsung ke daftar tanpa membuat objek baru.
     * Digunakan oleh AddShapeCommand.execute() dan DeleteShapeCommand.undo().
     */
    public void addShape(ShapeObject shape) {
        if (!shapes.contains(shape)) {
            shapes.add(shape);
        }
    }

    /**
     * Menyisipkan shape pada indeks tertentu (untuk undo group agar urutan terjaga).
     */
    public void addShapeAt(ShapeObject shape, int index) {
        if (!shapes.contains(shape)) {
            int clampedIndex = Math.max(0, Math.min(index, shapes.size()));
            shapes.add(clampedIndex, shape);
        }
    }

    /**
     * Menghapus shape tertentu dari daftar berdasarkan referensi objek.
     */
    public void removeShape(ShapeObject shape) {
        shapes.remove(shape);
    }

    /** Mengembalikan indeks shape dalam daftar (untuk undo yang perlu urutan). */
    public int indexOf(ShapeObject shape) {
        return shapes.indexOf(shape);
    }

    // -------------------------------------------------------------------------
    // Intersection color per pasangan shape - Anggota 4
    // -------------------------------------------------------------------------

    private String intersectionKey(ShapeObject a, ShapeObject b) {
        int idA = Math.min(a.getId(), b.getId());
        int idB = Math.max(a.getId(), b.getId());
        return idA + "_" + idB;
    }

    public void setIntersectionColor(ShapeObject a, ShapeObject b, Color color) {
        intersectionColors.put(intersectionKey(a, b), color);
    }

    /** Kembalikan warna irisan, atau null kalau belum di-set. */
    public Color getIntersectionColor(ShapeObject a, ShapeObject b) {
        return intersectionColors.get(intersectionKey(a, b));
    }

    public void clearIntersectionColors() {
        intersectionColors.clear();
    }

    /**
     * Menghasilkan ID unik untuk GroupObject (ID baru, tidak menubruk shape biasa).
     */
    public int buildGroupId() {
        return nextId++;
    }

    public void clearAll() {
        shapes.clear();
        intersectionColors.clear();
    }
}
