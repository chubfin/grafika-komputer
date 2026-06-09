/**
 * AddShapeCommand - Perintah untuk menambahkan shape ke kanvas
 * Dibuat oleh: Anggota 4
 *
 * Implementasi Command Pattern untuk aksi "tambah shape".
 * - execute(): menambahkan shape ke ShapeManager
 * - undo()   : menghapus shape yang tadi ditambahkan
 */
public class AddShapeCommand implements Command {

    private final ShapeManager shapeManager;
    private final ShapeObject shape;

    /**
     * @param shapeManager Manager yang mengelola daftar shapes
     * @param shape        Shape yang akan ditambahkan
     */
    public AddShapeCommand(ShapeManager shapeManager, ShapeObject shape) {
        this.shapeManager = shapeManager;
        this.shape = shape;
    }

    /**
     * Menambahkan shape ke daftar (juga memilihnya agar UI langsung update).
     */
    @Override
    public void execute() {
        shapeManager.addShape(shape);
        shapeManager.selectShape(shape);
    }

    /**
     * Menghapus shape yang baru saja ditambahkan (kebalikan execute).
     */
    @Override
    public void undo() {
        shapeManager.removeShape(shape);
    }
}
