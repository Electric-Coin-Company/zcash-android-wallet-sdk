package cash.z.wallet.sdk.dao

import androidx.room.*
import cash.z.wallet.sdk.entity.Transaction

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(transaction: Transaction)

    @Query("SELECT * FROM transactions WHERE id_tx = :id")
    fun findById(id: Long): Transaction?

    @Query("DELETE FROM transactions WHERE id_tx = :id")
    fun deleteById(id: Long)

    /**
     * Query transactions, aggregating information on send/receive, sorted carefully so the newest data is at the top
     * and the oldest transactions are at the bottom.
     */
    @Query("""
        SELECT transactions.id_tx             AS txId,
               transactions.block             AS height,
               transactions.raw IS NOT NULL   AS isSend,
               transactions.block IS NOT NULL AS isMined,
               blocks.time                    AS timeInSeconds,
               sent_notes.address             AS address,
               CASE
                 WHEN transactions.raw IS NOT NULL THEN sent_notes.value
                 ELSE received_notes.value
               END                            AS value
        FROM   transactions
               LEFT JOIN sent_notes
                      ON transactions.id_tx = sent_notes.tx
               LEFT JOIN received_notes
                      ON transactions.id_tx = received_notes.tx
               LEFT JOIN blocks
                      ON transactions.block = blocks.height
        ORDER  BY block IS NOT NUll, height DESC, time DESC, txId DESC
    """)
    fun getAll(): List<WalletTransaction>

    @Delete
    fun delete(transaction: Transaction)

    @Query("SELECT COUNT(id_tx) FROM transactions")
    fun count(): Int

}

data class WalletTransaction(
    val txId: Long = 0L,
    val value: Long = 0L,
    val height: Int? = null,
    val isSend: Boolean = false,
    val timeInSeconds: Long = 0L,
    val address: String? = null,
    val isMined: Boolean = false
)