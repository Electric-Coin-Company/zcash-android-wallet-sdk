package cash.z.wallet.sdk.dao

import androidx.room.*
import cash.z.wallet.sdk.entity.Note

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(block: Note)

    @Query("SELECT * FROM received_notes WHERE id_note = :id")
    fun findById(id: Int): Note?

    @Query("DELETE FROM received_notes WHERE id_note = :id")
    fun deleteById(id: Int)

    @Delete
    fun delete(block: Note)

    @Query("SELECT COUNT(id_note) FROM received_notes")
    fun count(): Int
}