package cash.z.wallet.sdk.dao

import androidx.room.*
import cash.z.wallet.sdk.vo.Transaction

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(transaction: Transaction)

    @Query("SELECT * FROM transactions WHERE id_tx = :id")
    fun findById(id: Long): Transaction?

    @Query("DELETE FROM transactions WHERE id_tx = :id")
    fun deleteById(id: Long)

    @Query("SELECT * FROM transactions WHERE 1")
    fun getAll(): List<Transaction>

    @Delete
    fun delete(transaction: Transaction)

    @Query("SELECT COUNT(id_tx) FROM transactions")
    fun count(): Int

}