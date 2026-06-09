import java.util.List;

/**
 * UngroupShapeCommand - Perintah untuk memecah GroupObject menjadi shapes individual.
 * Dibuat oleh: Anggota 4
 *
 * - execute(): hapus group, tambahkan kembali members
 * - undo()   : hapus members, kembalikan group
 */
public class UngroupShapeCommand implements Command {

    private final ShapeManager shapeManager;
    private final GroupObject group;
    private final List<ShapeObject> members;
    private int groupIndex;

    public UngroupShapeCommand(ShapeManager shapeManager, GroupObject group) {
        this.shapeManager = shapeManager;
        this.group = group;
        this.members = group.getMembers();
    }

    @Override
    public void execute() {
        groupIndex = shapeManager.indexOf(group);
        shapeManager.removeShape(group);
        for (int i = 0; i < members.size(); i++) {
            shapeManager.addShapeAt(members.get(i), groupIndex + i);
            members.get(i).setSelected(true);
        }
    }

    @Override
    public void undo() {
        for (ShapeObject s : members) {
            shapeManager.removeShape(s);
            s.setSelected(false);
        }
        shapeManager.addShapeAt(group, groupIndex);
        shapeManager.selectShape(group);
    }
}
