package cash.z.wallet.sdk.dao

import androidx.room.*
import cash.z.wallet.sdk.vo.Note
import cash.z.wallet.sdk.vo.NoteQuery

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(block: Note)

    @Query("SELECT * FROM received_notes WHERE id_note = :id")
    fun findById(id: Int): Note?

    @Query("DELETE FROM received_notes WHERE id_note = :id")
    fun deleteById(id: Int)

    /**
     * Query blocks, transactions and received_notes to aggregate information on send/receive
     */
    @Query("""
        SELECT received_notes.tx            AS txId,
               received_notes.value,
               transactions.block           AS height,
               transactions.raw IS NOT NULL AS sent,
               blocks.time
        FROM   received_notes,
               transactions,
               blocks
        WHERE  received_notes.tx = transactions.id_tx
               AND blocks.height = transactions.block
        ORDER  BY height DESC;
    """)
    fun getAll(): List<NoteQuery>

    @Delete
    fun delete(block: Note)

    @Query("SELECT COUNT(id_note) FROM received_notes")
    fun count(): Int
}