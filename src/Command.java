/**
 * Interface Command - Bagian dari implementasi Command Pattern untuk sistem Undo/Redo
 * Dibuat oleh: Anggota 4
 *
 * Setiap aksi yang dapat di-undo/redo harus mengimplementasikan interface ini.
 * Contoh aksi: menambah shape, menghapus shape, dsb.
 */
public interface Command {

    /**
     * Menjalankan perintah (aksi utama).
     * Dipanggil saat pertama kali aksi dilakukan, atau saat redo.
     */
    void execute();

    /**
     * Membatalkan perintah (kebalikan dari execute).
     * Dipanggil saat undo dilakukan.
     */
    void undo();
}
