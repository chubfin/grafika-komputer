import java.util.ArrayDeque;
import java.util.Deque;

/**
 * HistoryManager - Pengelola riwayat aksi untuk sistem Undo/Redo
 * Dibuat oleh: Anggota 4
 *
 * Menggunakan dua stack:
 * - undoStack: menyimpan perintah yang sudah dieksekusi (bisa di-undo)
 * - redoStack: menyimpan perintah yang sudah di-undo (bisa di-redo)
 *
 * Alur kerja:
 *   executeCommand -> push ke undoStack, kosongkan redoStack
 *   undo           -> pop dari undoStack, push ke redoStack
 *   redo           -> pop dari redoStack, push ke undoStack
 */
public class HistoryManager {

    // Batas maksimal riwayat undo agar memori tidak habis
    private static final int MAX_HISTORY = 50;

    /** Stack untuk menyimpan perintah yang dapat di-undo */
    private final Deque<Command> undoStack;

    /** Stack untuk menyimpan perintah yang dapat di-redo */
    private final Deque<Command> redoStack;

    public HistoryManager() {
        this.undoStack = new ArrayDeque<>();
        this.redoStack = new ArrayDeque<>();
    }

    /**
     * Mengeksekusi sebuah perintah dan mencatatnya ke riwayat.
     * Setiap kali aksi baru dieksekusi, redoStack dikosongkan
     * karena riwayat "masa depan" tidak lagi relevan.
     *
     * @param command Perintah yang akan dieksekusi
     */
    public void executeCommand(Command command) {
        command.execute();

        // Simpan ke undoStack dengan batas maksimal
        if (undoStack.size() >= MAX_HISTORY) {
            // Hapus perintah paling lama (di bagian bawah deque)
            ((ArrayDeque<Command>) undoStack).removeLast();
        }
        undoStack.push(command);

        // Aksi baru membatalkan semua redo yang tersimpan
        redoStack.clear();
    }

    /**
     * Membatalkan aksi terakhir.
     * Memindahkan perintah dari undoStack ke redoStack.
     *
     * @return true jika undo berhasil, false jika tidak ada yang bisa di-undo
     */
    public boolean undo() {
        if (undoStack.isEmpty()) {
            return false;
        }

        Command command = undoStack.pop();
        command.undo();
        redoStack.push(command);
        return true;
    }

    /**
     * Mengulangi aksi yang sudah di-undo.
     * Memindahkan perintah dari redoStack ke undoStack.
     *
     * @return true jika redo berhasil, false jika tidak ada yang bisa di-redo
     */
    public boolean redo() {
        if (redoStack.isEmpty()) {
            return false;
        }

        Command command = redoStack.pop();
        command.execute();
        undoStack.push(command);
        return true;
    }

    /**
     * @return true jika ada perintah yang dapat di-undo
     */
    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    /**
     * @return true jika ada perintah yang dapat di-redo
     */
    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    /**
     * Menghapus seluruh riwayat (undo dan redo).
     * Berguna saat membuka file baru atau mereset kanvas.
     */
    public void clearHistory() {
        undoStack.clear();
        redoStack.clear();
    }
}
