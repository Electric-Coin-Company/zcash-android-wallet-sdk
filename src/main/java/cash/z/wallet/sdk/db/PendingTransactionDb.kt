package cash.z.wallet.sdk.db

import androidx.room.*
import cash.z.wallet.sdk.entity.PendingTransactionEntity
import kotlinx.coroutines.flow.Flow


//
// Database
//

@Database(
    entities = [
        PendingTransactionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PendingTransactionDb : RoomDatabase() {
    abstract fun pendingTransactionDao(): PendingTransactionDao
}


//
// Data Access Objects
//

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


