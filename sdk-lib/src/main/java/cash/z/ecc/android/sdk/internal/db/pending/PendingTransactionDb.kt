package cash.z.ecc.android.sdk.internal.db.pending

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 2,
    exportSchema = true
)
internal abstract class PendingTransactionDb : RoomDatabase() {
    abstract fun pendingTransactionDao(): PendingTransactionDao

    companion object {

        /*
         * Non-automatic migration required because to_address became nullable.
         */
        internal val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                 ALTER TABLE pending_transactions RENAME TO pending_transactions_old;
                 CREATE TABLE pending_transactions(
                     id                         INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                     to_address                 TEXT,
                     to_internal_account_index  INTEGER,
                     sent_from_account_index    INTEGER NOT NULL,
                     mined_height               INTEGER,
                     expiry_height              INTEGER,
                     cancelled                  INTEGER,
                     encode_attempts            INTEGER DEFAULT (0),
                     error_message              TEXT,
                     error_code                 INTEGER,
                     submit_attempts            INTEGER DEFAULT (0),
                     create_time                INTEGER,
                     txid                       BLOB,
                     value                      INTEGER NOT NULL,
                     raw                        BLOB,
                     memo                       BLOB,
                     fee                        INTEGER
                 );
                 INSERT INTO pending_transactions
                 SELECT
                     id,
                     toAddress,
                     NULL,
                     accountIndex,
                     minedHeight,
                     expiryHeight,
                     cancelled,
                     encodeAttempts,
                     errorMessage,
                     errorCode,
                     submitAttempts,
                     createTime,
                     txid,
                     value,
                     raw,
                     memo,
                     NULL
                 FROM pending_transactions_old;
                 DROP TABLE pending_transactions_old
                 """
                )
            }
        }
    }
}

//
// Data Access Objects
//

/**
 * Data access object providing crud for pending transactions.
 */
@Dao
@Suppress("TooManyFunctions")
internal interface PendingTransactionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun create(transaction: PendingTransactionEntity): Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(transaction: PendingTransactionEntity)

    @Delete
    suspend fun delete(transaction: PendingTransactionEntity): Int

    @Query("UPDATE pending_transactions SET cancelled = 1 WHERE id = :id")
    suspend fun cancel(id: Long)

    @Query("SELECT * FROM pending_transactions WHERE id = :id")
    suspend fun findById(id: Long): PendingTransactionEntity?

    @Query("SELECT * FROM pending_transactions ORDER BY create_time")
    fun getAll(): Flow<List<PendingTransactionEntity>>

    @Query("SELECT * FROM pending_transactions WHERE id = :id")
    fun monitorById(id: Long): Flow<PendingTransactionEntity>

    //
    // Update helper functions
    //

    @Query("UPDATE pending_transactions SET raw_transaction_id = null WHERE id = :id")
    suspend fun removeRawTransactionId(id: Long)

    @Query("UPDATE pending_transactions SET mined_height = :minedHeight WHERE id = :id")
    suspend fun updateMinedHeight(id: Long, minedHeight: Long)

    @Query(
        "UPDATE pending_transactions SET raw = :raw, raw_transaction_id = :rawTransactionId," +
            " expiry_height = :expiryHeight WHERE id = :id"
    )
    suspend fun updateEncoding(id: Long, raw: ByteArray, rawTransactionId: ByteArray, expiryHeight: Long?)

    @Query("UPDATE pending_transactions SET error_message = :errorMessage, error_code = :errorCode WHERE id = :id")
    suspend fun updateError(id: Long, errorMessage: String?, errorCode: Int?)

    @Query("UPDATE pending_transactions SET encode_attempts = :attempts WHERE id = :id")
    suspend fun updateEncodeAttempts(id: Long, attempts: Int)

    @Query("UPDATE pending_transactions SET submit_attempts = :attempts WHERE id = :id")
    suspend fun updateSubmitAttempts(id: Long, attempts: Int)
}
