package cash.z.ecc.android.sdk.db

import androidx.room.*
import cash.z.ecc.android.sdk.db.entity.PendingTransactionEntity
import kotlinx.coroutines.flow.Flow


//
// Database
//

/**
 * Database for pending transaction information. Unlike with the "Data DB," the wallet is free to
 * write to this database. In a way, this almost serves as a local mempool for all transactions
 * initiated by this wallet. Currently, the data necessary to support expired transactions is there
 * but it is not being leveraged.
 */
@Database(
    entities = [
        PendingTransactionEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class PendingTransactionDb : RoomDatabase() {
    abstract fun pendingTransactionDao(): PendingTransactionDao
}


//
// Data Access Objects
//

/**
 * Data access object providing crud for pending transactions.
 */
@Dao
interface PendingTransactionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun create(transaction: PendingTransactionEntity): Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(transaction: PendingTransactionEntity)

    @Delete
    suspend fun delete(transaction: PendingTransactionEntity)

    @Query("UPDATE pending_transactions SET cancelled = 1 WHERE id = :id")
    suspend fun cancel(id: Long)

    @Query("SELECT * FROM pending_transactions WHERE id = :id")
    suspend fun findById(id: Long): PendingTransactionEntity?

    @Query("SELECT * FROM pending_transactions ORDER BY createTime")
    fun getAll(): Flow<List<PendingTransactionEntity>>

    @Query("SELECT * FROM pending_transactions WHERE id = :id")
    fun monitorById(id: Long): Flow<PendingTransactionEntity>
}


