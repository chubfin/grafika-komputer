/**
 * DeleteShapeCommand - Perintah untuk menghapus shape dari kanvas
 * Dibuat oleh: Anggota 4
 *
 * Implementasi Command Pattern untuk aksi "hapus shape".
 * - execute(): menghapus shape dari ShapeManager
 * - undo()   : mengembalikan shape yang tadi dihapus
 *
 * Catatan: shape yang dihapus disimpan sebagai referensi objek sehingga
 * saat undo, semua properti (warna, posisi, transformasi) kembali utuh.
 */
public class DeleteShapeCommand implements Command {

    private final ShapeManager shapeManager;
    private final ShapeObject shape;

    /**
     * @param shapeManager Manager yang mengelola daftar shapes
     * @param shape        Shape yang akan dihapus
     */
    public DeleteShapeCommand(ShapeManager shapeManager, ShapeObject shape) {
        this.shapeManager = shapeManager;
        this.shape = shape;
    }

    /**
     * Menghapus shape dari daftar.
     */
    @Override
    public void execute() {
        shapeManager.removeShape(shape);
    }

    /**
     * Mengembalikan shape yang dihapus ke daftar (undo).
     */
    @Override
    public void undo() {
        shapeManager.addShape(shape);
        shapeManager.selectShape(shape);
    }
}
