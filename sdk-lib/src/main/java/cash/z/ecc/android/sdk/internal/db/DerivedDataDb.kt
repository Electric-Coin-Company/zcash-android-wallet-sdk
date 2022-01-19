package cash.z.ecc.android.sdk.internal.db

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import cash.z.ecc.android.sdk.db.entity.Account
import cash.z.ecc.android.sdk.db.entity.Block
import cash.z.ecc.android.sdk.db.entity.ConfirmedTransaction
import cash.z.ecc.android.sdk.db.entity.EncodedTransaction
import cash.z.ecc.android.sdk.db.entity.Received
import cash.z.ecc.android.sdk.db.entity.Sent
import cash.z.ecc.android.sdk.db.entity.TransactionEntity
import cash.z.ecc.android.sdk.db.entity.Utxo
import cash.z.ecc.android.sdk.internal.twig
import cash.z.ecc.android.sdk.type.UnifiedAddressAccount

//
// Database
//

/**
 * The "Data DB," where all data derived from the compact blocks is stored. Most importantly, this
 * database contains transaction information and can be queried for the current balance. The
 * "blocks" table contains a copy of everything that has been scanned. In the future, that table can
 * be truncated up to the last scanned block, for storage efficiency. Wallets should only read from,
 * but never write to, this database.
 */
@Database(
    entities = [
        TransactionEntity::class,
        Block::class,
        Received::class,
        Account::class,
        Sent::class,
        Utxo::class
    ],
    version = 7,
    exportSchema = true
)
abstract class DerivedDataDb : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun blockDao(): BlockDao
    abstract fun receivedDao(): ReceivedDao
    abstract fun sentDao(): SentDao
    abstract fun accountDao(): AccountDao

    //
    // Migrations
    //

    companion object {

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("PRAGMA foreign_keys = OFF;")
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS received_notes_new (
                        id_note INTEGER PRIMARY KEY, tx INTEGER NOT NULL,
                        output_index INTEGER NOT NULL, account INTEGER NOT NULL,
                        diversifier BLOB NOT NULL, value INTEGER NOT NULL,
                        rcm BLOB NOT NULL, nf BLOB NOT NULL UNIQUE,
                        is_change INTEGER NOT NULL, memo BLOB,
                        spent INTEGER,
                        FOREIGN KEY (tx) REFERENCES transactions(id_tx),
                        FOREIGN KEY (account) REFERENCES accounts(account),
                        FOREIGN KEY (spent) REFERENCES transactions(id_tx),
                        CONSTRAINT tx_output UNIQUE (tx, output_index)
                    ); 
                    """.trimIndent()
                )
                database.execSQL("INSERT INTO received_notes_new SELECT * FROM received_notes;")
                database.execSQL("DROP TABLE received_notes;")
                database.execSQL("ALTER TABLE received_notes_new RENAME TO received_notes;")
                database.execSQL("PRAGMA foreign_keys = ON;")
            }
        }

        val MIGRATION_4_3 = object : Migration(4, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("PRAGMA foreign_keys = OFF;")
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS received_notes_new (
                        id_note INTEGER PRIMARY KEY,
                        tx INTEGER NOT NULL,
                        output_index INTEGER NOT NULL,
                        account INTEGER NOT NULL,
                        diversifier BLOB NOT NULL,
                        value INTEGER NOT NULL,
                        rcm BLOB NOT NULL,
                        nf BLOB NOT NULL UNIQUE,
                        is_change INTEGER NOT NULL,
                        memo BLOB,
                        spent INTEGER,
                        FOREIGN KEY (tx) REFERENCES transactions(id_tx),
                        FOREIGN KEY (account) REFERENCES accounts(account),
                        FOREIGN KEY (spent) REFERENCES transactions(id_tx),
                        CONSTRAINT tx_output UNIQUE (tx, output_index)
                    ); 
                    """.trimIndent()
                )
                database.execSQL("INSERT INTO received_notes_new SELECT * FROM received_notes;")
                database.execSQL("DROP TABLE received_notes;")
                database.execSQL("ALTER TABLE received_notes_new RENAME TO received_notes;")
                database.execSQL("PRAGMA foreign_keys = ON;")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("PRAGMA foreign_keys = OFF;")
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS received_notes_new (
                        id_note INTEGER PRIMARY KEY,
                        tx INTEGER NOT NULL,
                        output_index INTEGER NOT NULL,
                        account INTEGER NOT NULL,
                        diversifier BLOB NOT NULL,
                        value INTEGER NOT NULL,
                        rcm BLOB NOT NULL,
                        nf BLOB NOT NULL UNIQUE,
                        is_change INTEGER NOT NULL,
                        memo BLOB,
                        spent INTEGER,
                        FOREIGN KEY (tx) REFERENCES transactions(id_tx),
                        FOREIGN KEY (account) REFERENCES accounts(account),
                        FOREIGN KEY (spent) REFERENCES transactions(id_tx),
                        CONSTRAINT tx_output UNIQUE (tx, output_index)
                    ); 
                    """.trimIndent()
                )
                database.execSQL("INSERT INTO received_notes_new SELECT * FROM received_notes;")
                database.execSQL("DROP TABLE received_notes;")
                database.execSQL("ALTER TABLE received_notes_new RENAME TO received_notes;")
                database.execSQL("PRAGMA foreign_keys = ON;")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS utxos (
                        id_utxo INTEGER PRIMARY KEY,
                        address TEXT NOT NULL, 
                        prevout_txid BLOB NOT NULL, 
                        prevout_idx INTEGER NOT NULL, 
                        script BLOB NOT NULL, 
                        value_zat INTEGER NOT NULL, 
                        height INTEGER NOT NULL,
                        spent_in_tx INTEGER,
                        FOREIGN KEY (spent_in_tx) REFERENCES transactions(id_tx),
                        CONSTRAINT tx_outpoint UNIQUE (prevout_txid, prevout_idx)
                    ); 
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("PRAGMA foreign_keys = OFF;")
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS accounts_new (
                        account INTEGER PRIMARY KEY,
                        extfvk TEXT NOT NULL,
                        address TEXT NOT NULL,
                        transparent_address TEXT NOT NULL
                    ); 
                    """.trimIndent()
                )
                database.execSQL("DROP TABLE accounts;")
                database.execSQL("ALTER TABLE accounts_new RENAME TO accounts;")
                database.execSQL("PRAGMA foreign_keys = ON;")
            }
        }
    }
}

//
// Data Access Objects
//

/**
 * The data access object for blocks, used for determining the last scanned height.
 */
@Dao
interface BlockDao {
    @Query("SELECT COUNT(height) FROM blocks")
    suspend fun count(): Int

    @Query("SELECT MAX(height) FROM blocks")
    suspend fun lastScannedHeight(): Int

    @Query("SELECT MIN(height) FROM blocks")
    suspend fun firstScannedHeight(): Int

    @Query("SELECT hash FROM BLOCKS WHERE height = :height")
    suspend fun findHashByHeight(height: Int): ByteArray?
}

/**
 * The data access object for notes, used for determining whether transactions exist.
 */
@Dao
interface ReceivedDao {
    @Query("SELECT COUNT(tx) FROM received_notes")
    suspend fun count(): Int
}

/**
 * The data access object for sent notes, used for determining whether outbound transactions exist.
 */
@Dao
interface SentDao {
    @Query("SELECT COUNT(tx) FROM sent_notes")
    suspend fun count(): Int
}

@Dao
interface AccountDao {
    @Query("SELECT COUNT(account) FROM accounts")
    suspend fun count(): Int

    @Query(
        """
        SELECT account AS accountId,
               transparent_address AS rawTransparentAddress,
               address AS rawShieldedAddress
        FROM accounts
        WHERE account = :id
        """
    )
    suspend fun findAccountById(id: Int): UnifiedAddressAccount?
}

/**
 * The data access object for transactions, used for querying all transaction information, including
 * whether transactions are mined.
 */
@Dao
interface TransactionDao {
    @Query("SELECT COUNT(id_tx) FROM transactions")
    suspend fun count(): Int

    @Query("SELECT COUNT(block) FROM transactions WHERE block IS NULL")
    suspend fun countUnmined(): Int

    @Query(
        """
        SELECT transactions.txid AS txId, 
               transactions.raw  AS raw,
               transactions.expiry_height AS expiryHeight
        FROM   transactions
        WHERE  id_tx = :id AND raw is not null
        """
    )
    suspend fun findEncodedTransactionById(id: Long): EncodedTransaction?

    @Query(
        """
        SELECT transactions.block
        FROM   transactions 
        WHERE  txid = :rawTransactionId
        LIMIT  1 
    """
    )
    suspend fun findMinedHeight(rawTransactionId: ByteArray): Int?

    /**
     * Query sent transactions that have been mined, sorted so the newest data is at the top.
     */
    @Query(
        """
        SELECT transactions.id_tx         AS id,
               transactions.block         AS minedHeight,
               transactions.tx_index      AS transactionIndex,
               transactions.txid          AS rawTransactionId,
               transactions.expiry_height AS expiryHeight,
               transactions.raw           AS raw,
               sent_notes.address         AS toAddress,
               sent_notes.value           AS value,
               sent_notes.memo            AS memo,
               sent_notes.id_note         AS noteId,
               blocks.time                AS blockTimeInSeconds
        FROM   transactions
               LEFT JOIN sent_notes 
                      ON transactions.id_tx = sent_notes.tx
               LEFT JOIN blocks
                      ON transactions.block = blocks.height
        WHERE  transactions.raw IS NOT NULL
               AND minedheight > 0
        ORDER  BY block IS NOT NULL, height DESC, time DESC, txid DESC
        LIMIT  :limit
    """
    )
    fun getSentTransactions(limit: Int = Int.MAX_VALUE): DataSource.Factory<Int, ConfirmedTransaction>

    /**
     * Query transactions, aggregating information on send/receive, sorted carefully so the newest
     * data is at the top and the oldest transactions are at the bottom.
     */
    @Query(
        """
        SELECT transactions.id_tx     AS id,
               transactions.block     AS minedHeight,
               transactions.tx_index  AS transactionIndex,
               transactions.txid      AS rawTransactionId,
               received_notes.value   AS value,
               received_notes.memo    AS memo,
               received_notes.id_note AS noteId,
               blocks.time            AS blockTimeInSeconds
        FROM   transactions
               LEFT JOIN received_notes
                      ON transactions.id_tx = received_notes.tx
               LEFT JOIN blocks
                      ON transactions.block = blocks.height
        WHERE  received_notes.is_change != 1
        ORDER  BY minedheight DESC, blocktimeinseconds DESC, id DESC
        LIMIT  :limit
    """
    )
    fun getReceivedTransactions(limit: Int = Int.MAX_VALUE): DataSource.Factory<Int, ConfirmedTransaction>

    /**
     * Query all transactions, joining outbound and inbound transactions into the same table.
     */
    @Query(
        """
         SELECT transactions.id_tx          AS id,
               transactions.block           AS minedHeight,
               transactions.tx_index        AS transactionIndex,
               transactions.txid            AS rawTransactionId,
               transactions.expiry_height   AS expiryHeight,
               transactions.raw             AS raw,
               sent_notes.address           AS toAddress,
               CASE
                 WHEN sent_notes.value IS NOT NULL THEN sent_notes.value
                 ELSE received_notes.value
               end                          AS value,
               CASE
                 WHEN sent_notes.memo IS NOT NULL THEN sent_notes.memo
                 ELSE received_notes.memo
               end                          AS memo,
               CASE
                 WHEN sent_notes.id_note IS NOT NULL THEN sent_notes.id_note
                 ELSE received_notes.id_note
               end                          AS noteId,
               blocks.time                  AS blockTimeInSeconds
         FROM   transactions
               LEFT JOIN received_notes
                      ON transactions.id_tx = received_notes.tx
               LEFT JOIN sent_notes
                      ON transactions.id_tx = sent_notes.tx
               LEFT JOIN blocks
                      ON transactions.block = blocks.height
        /* we want all received txs except those that are change and all sent transactions (even those that haven't been mined yet). Note: every entry in the 'send_notes' table has a non-null value for 'address' */
        WHERE  ( sent_notes.address IS NULL 
                 AND received_notes.is_change != 1 ) 
                OR sent_notes.address IS NOT NULL   
         ORDER  BY ( minedheight IS NOT NULL ),
                  minedheight DESC,
                  blocktimeinseconds DESC,
                  id DESC
         LIMIT  :limit
    """
    )
    fun getAllTransactions(limit: Int = Int.MAX_VALUE): DataSource.Factory<Int, ConfirmedTransaction>

    /**
     * Query the transactions table over the given block range, this includes transactions that
     * should not show up in most UIs. The intended purpose of this request is to find new
     * transactions that need to be enhanced via follow-up requests to the server.
     */
    @Query(
        """
        SELECT transactions.id_tx         AS id, 
               transactions.block         AS minedHeight, 
               transactions.tx_index      AS transactionIndex, 
               transactions.txid          AS rawTransactionId, 
               transactions.expiry_height AS expiryHeight, 
               transactions.raw           AS raw, 
               sent_notes.address         AS toAddress, 
               CASE 
                 WHEN sent_notes.value IS NOT NULL THEN sent_notes.value 
                 ELSE received_notes.value 
               end                        AS value, 
               CASE 
                 WHEN sent_notes.memo IS NOT NULL THEN sent_notes.memo 
                 ELSE received_notes.memo 
               end                        AS memo, 
               CASE 
                 WHEN sent_notes.id_note IS NOT NULL THEN sent_notes.id_note 
                 ELSE received_notes.id_note 
               end                        AS noteId, 
               blocks.time                AS blockTimeInSeconds 
        FROM   transactions 
               LEFT JOIN received_notes 
                      ON transactions.id_tx = received_notes.tx 
               LEFT JOIN sent_notes 
                      ON transactions.id_tx = sent_notes.tx 
               LEFT JOIN blocks 
                      ON transactions.block = blocks.height 
        WHERE  :blockRangeStart <= minedheight 
               AND minedheight <= :blockRangeEnd 
        ORDER  BY ( minedheight IS NOT NULL ), 
                  minedheight ASC, 
                  blocktimeinseconds DESC, 
                  id DESC 
        LIMIT  :limit 
    """
    )
    suspend fun findAllTransactionsByRange(blockRangeStart: Int, blockRangeEnd: Int = blockRangeStart, limit: Int = Int.MAX_VALUE): List<ConfirmedTransaction>

    // Experimental: cleanup cancelled transactions
    //               This should probably be a rust call but there's not a lot of bandwidth for this
    //               work to happen in librustzcash. So prove the concept on our side, first
    //               then move the logic to the right place. Especially since the data access API is
    //               coming soon
    @Transaction
    suspend fun cleanupCancelledTx(rawTransactionId: ByteArray): Boolean {
        var success = false
        try {
            var hasInitialMatch = false
            var hasFinalMatch = true
            twig("[cleanup] cleanupCancelledTx starting...")
            findUnminedTransactionIds(rawTransactionId).also {
                twig("[cleanup] cleanupCancelledTx found ${it.size} matching transactions to cleanup")
            }.forEach { transactionId ->
                hasInitialMatch = true
                removeInvalidOutboundTransaction(transactionId)
            }
            hasFinalMatch = findMatchingTransactionId(rawTransactionId) != null
            success = hasInitialMatch && !hasFinalMatch
            twig("[cleanup] cleanupCancelledTx Done. success? $success")
        } catch (t: Throwable) {
            twig("[cleanup] failed to cleanup transaction due to: $t")
        }
        return success
    }

    @Transaction
    suspend fun removeInvalidOutboundTransaction(transactionId: Long): Boolean {
        var success = false
        try {
            twig("[cleanup] removing invalid transactionId:$transactionId")
            val result = unspendTransactionNotes(transactionId)
            twig("[cleanup] unspent ($result) notes matching transaction $transactionId")
            findSentNoteIds(transactionId)?.forEach { noteId ->
                twig("[cleanup] WARNING: deleting invalid sent noteId:$noteId")
                deleteSentNote(noteId)
            }

            // delete the UTXOs because these are effectively cached and we don't have a good way of knowing whether they're spent
            deleteUtxos(transactionId).let { count ->
                twig("[cleanup] removed $count UTXOs matching transactionId $transactionId")
            }

            twig("[cleanup] WARNING: deleting invalid transactionId $transactionId")
            success = deleteTransaction(transactionId) != 0
            twig("[cleanup] removeInvalidTransaction Done. success? $success")
        } catch (t: Throwable) {
            twig("[cleanup] failed to remove Invalid Transaction due to: $t")
        }
        return success
    }

    @Transaction
    suspend fun deleteExpired(lastHeight: Int): Int {
        var count = 0
        findExpiredTxs(lastHeight).forEach { transactionId ->
            if (removeInvalidOutboundTransaction(transactionId)) count++
        }
        return count
    }

    //
    // Private-ish functions (these will move to rust, or the data access API eventually)
    //

    @Query(
        """
        SELECT transactions.id_tx AS id
        FROM   transactions 
        WHERE  txid = :rawTransactionId
               AND block IS NULL
    """
    )
    suspend fun findUnminedTransactionIds(rawTransactionId: ByteArray): List<Long>

    @Query(
        """
        SELECT transactions.id_tx AS id
        FROM   transactions 
        WHERE  txid = :rawTransactionId
        LIMIT 1
    """
    )
    suspend fun findMatchingTransactionId(rawTransactionId: ByteArray): Long?

    @Query(
        """
        SELECT sent_notes.id_note AS id
        FROM   sent_notes 
        WHERE  tx = :transactionId 
    """
    )
    suspend fun findSentNoteIds(transactionId: Long): List<Int>?

    @Query("DELETE FROM sent_notes WHERE id_note = :id")
    suspend fun deleteSentNote(id: Int): Int

    @Query("DELETE FROM transactions WHERE id_tx = :id")
    suspend fun deleteTransaction(id: Long): Int

    @Query("UPDATE received_notes SET spent = null WHERE spent = :transactionId")
    suspend fun unspendTransactionNotes(transactionId: Long): Int

    @Query("DELETE FROM utxos WHERE spent_in_tx = :utxoId")
    suspend fun deleteUtxos(utxoId: Long): Int

    @Query(
        """
        SELECT transactions.id_tx
        FROM   transactions
        WHERE  created IS NOT NULL
            AND block IS NULL
            AND tx_index IS NULL
            AND expiry_height < :lastheight
    """
    )
    suspend fun findExpiredTxs(lastheight: Int): List<Long>
}
