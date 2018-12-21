package cash.z.wallet.sdk.dao

import androidx.room.*
import cash.z.wallet.sdk.vo.Note

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(block: Note)

    @Query("SELECT * FROM received_notes WHERE id_note = :id")
    fun findById(id: Int): Note?

    @Query("DELETE FROM received_notes WHERE id_note = :id")
    fun deleteById(id: Int)

    @Query("SELECT * FROM received_notes WHERE 1")
    fun getAll(): List<Note>

    @Delete
    fun delete(block: Note)

    @Query("SELECT COUNT(id_note) FROM received_notes")
    fun count(): Int
}