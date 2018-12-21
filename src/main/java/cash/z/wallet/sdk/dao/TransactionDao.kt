package cash.z.wallet.sdk.dao

import androidx.room.*
import cash.z.wallet.sdk.vo.Transaction

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(block: Transaction)

    @Query("SELECT * FROM transactions WHERE id_tx = :id")
    fun findById(id: Int): Transaction?

    @Query("DELETE FROM transactions WHERE id_tx = :id")
    fun deleteById(id: Int)

    @Query("SELECT * FROM transactions WHERE 1")
    fun getAll(): List<Transaction>

    @Delete
    fun delete(block: Transaction)

    @Query("SELECT COUNT(id_tx) FROM transactions")
    fun count(): Int

}