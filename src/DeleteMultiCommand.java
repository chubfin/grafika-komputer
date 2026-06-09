import java.util.ArrayList;
import java.util.List;

/**
 * DeleteMultiCommand - Menghapus beberapa shape sekaligus dalam satu Command.
 * Dibuat oleh: Anggota 4
 *
 * Digunakan saat Delete ditekan dengan multi-selection aktif.
 * - execute(): hapus semua shape yang terpilih
 * - undo()   : kembalikan semua shape yang dihapus ke posisi semula
 */
public class DeleteMultiCommand implements Command {

    private final ShapeManager shapeManager;
    private final List<ShapeObject> shapes;
    private final List<Integer> originalIndices;

    public DeleteMultiCommand(ShapeManager shapeManager, List<ShapeObject> shapes) {
        this.shapeManager = shapeManager;
        this.shapes = new ArrayList<>(shapes);
        this.originalIndices = new ArrayList<>();
        for (ShapeObject s : this.shapes) {
            originalIndices.add(shapeManager.indexOf(s));
        }
    }

    @Override
    public void execute() {
        for (ShapeObject s : shapes) {
            shapeManager.removeShape(s);
        }
    }

    @Override
    public void undo() {
        for (int i = 0; i < shapes.size(); i++) {
            int idx = Math.min(originalIndices.get(i), shapeManager.getShapes().size());
            shapeManager.addShapeAt(shapes.get(i), idx);
            shapes.get(i).setSelected(true);
        }
    }
}
