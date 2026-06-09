import java.util.ArrayList;
import java.util.List;

/**
 * GroupShapeCommand - Perintah untuk menggabungkan beberapa shape menjadi group.
 * Dibuat oleh: Anggota 4
 *
 * - execute(): hapus shapes individual, tambahkan GroupObject
 * - undo()   : hapus GroupObject, kembalikan shapes individual
 */
public class GroupShapeCommand implements Command {

    private final ShapeManager shapeManager;
    private final List<ShapeObject> members;
    private final List<Integer> originalIndices;
    private final GroupObject group;

    public GroupShapeCommand(ShapeManager shapeManager, List<ShapeObject> members) {
        this.shapeManager = shapeManager;
        this.members = new ArrayList<>(members);
        this.originalIndices = new ArrayList<>();
        // Simpan indeks asli agar urutan terjaga saat undo
        for (ShapeObject s : members) {
            originalIndices.add(shapeManager.indexOf(s));
        }
        this.group = new GroupObject(shapeManager.buildGroupId(), members);
    }

    @Override
    public void execute() {
        // Hapus members dari kanvas
        for (ShapeObject s : members) {
            shapeManager.removeShape(s);
        }
        // Tambahkan group sebagai satu entitas
        shapeManager.addShape(group);
        shapeManager.selectShape(group);
    }

    @Override
    public void undo() {
        shapeManager.removeShape(group);
        // Kembalikan members pada posisi semula
        for (int i = 0; i < members.size(); i++) {
            int idx = Math.min(originalIndices.get(i), shapeManager.getShapes().size());
            shapeManager.addShapeAt(members.get(i), idx);
        }
        // Pilih kembali semua member
        for (ShapeObject s : members) {
            s.setSelected(true);
        }
    }

    public GroupObject getGroup() {
        return group;
    }
}
