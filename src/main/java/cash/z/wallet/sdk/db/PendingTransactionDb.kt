package cash.z.wallet.sdk.db

import androidx.room.*
import cash.z.wallet.sdk.entity.PendingTransaction


//
// Database
//

@Database(
    entities = [
        PendingTransaction::class
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
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(transaction: PendingTransaction): Long

    @Delete
    fun delete(transaction: PendingTransaction)

    @Query("SELECT * from pending_transactions ORDER BY createTime")
    fun getAll(): List<PendingTransaction>
}


