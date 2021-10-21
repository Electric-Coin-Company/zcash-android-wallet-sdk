package cash.z.ecc.android.sdk.internal.transaction

import android.content.Context
import androidx.paging.PagedList
import androidx.room.Room
import androidx.room.RoomDatabase
import cash.z.ecc.android.sdk.internal.db.AccountDao
import cash.z.ecc.android.sdk.internal.db.BlockDao
import cash.z.ecc.android.sdk.internal.db.DerivedDataDb
import cash.z.ecc.android.sdk.internal.db.TransactionDao
import cash.z.ecc.android.sdk.db.entity.ConfirmedTransaction
import cash.z.ecc.android.sdk.exception.RepositoryException
import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.internal.ext.android.RefreshableDataSourceFactory
import cash.z.ecc.android.sdk.internal.ext.android.toFlowPagedList
import cash.z.ecc.android.sdk.internal.ext.android.toRefreshable
import cash.z.ecc.android.sdk.internal.ext.tryWarn
import cash.z.ecc.android.sdk.internal.twig
import cash.z.ecc.android.sdk.internal.SdkDispatchers
import cash.z.ecc.android.sdk.jni.RustBackend
import cash.z.ecc.android.sdk.type.UnifiedAddressAccount
import cash.z.ecc.android.sdk.type.UnifiedViewingKey
import cash.z.ecc.android.sdk.type.WalletBirthday
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Example of a repository that leverages the Room paging library to return a [PagedList] of
 * transactions. Consumers can register as a page listener and receive an interface that allows for
 * efficiently paging data.
 *
 * @param pageSize transactions per page. This influences pre-fetch and memory configuration.
 */
class PagedTransactionRepository(
    val appContext: Context,
    val pageSize: Int = 10,
    val rustBackend: RustBackend,
    val birthday: WalletBirthday,
    val viewingKeys: List<UnifiedViewingKey>,
    val overwriteVks: Boolean = false,
) : TransactionRepository {

    private val lazy = LazyPropertyHolder()

    override val receivedTransactions get() = lazy.receivedTransactions
    override val sentTransactions get() = lazy.sentTransactions
    override val allTransactions get() = lazy.allTransactions

    //
    // TransactionRepository API
    //

    override fun invalidate() = lazy.allTransactionsFactory.refresh()

    override fun lastScannedHeight(): Int {
        return lazy.blocks.lastScannedHeight()
    }

    override fun firstScannedHeight(): Int {
        return lazy.blocks.firstScannedHeight()
    }

    override fun isInitialized(): Boolean {
        return lazy.blocks.count() > 0
    }

    override suspend fun findEncodedTransactionById(txId: Long) = withContext(IO) {
        lazy.transactions.findEncodedTransactionById(txId)
    }

    override suspend fun findNewTransactions(blockHeightRange: IntRange): List<ConfirmedTransaction> =
        lazy.transactions.findAllTransactionsByRange(blockHeightRange.first, blockHeightRange.last)

    override suspend fun findMinedHeight(rawTransactionId: ByteArray) = withContext(IO) {
        lazy.transactions.findMinedHeight(rawTransactionId)
    }

    override suspend fun findMatchingTransactionId(rawTransactionId: ByteArray): Long? =
        lazy.transactions.findMatchingTransactionId(rawTransactionId)

    override suspend fun cleanupCancelledTx(rawTransactionId: ByteArray) = lazy.transactions.cleanupCancelledTx(rawTransactionId)
    override suspend fun deleteExpired(lastScannedHeight: Int): Int {
        // let expired transactions linger in the UI for a little while
        return lazy.transactions.deleteExpired(lastScannedHeight - (ZcashSdk.EXPIRY_OFFSET / 2))
    }
    override suspend fun count(): Int = withContext(IO) {
        lazy.transactions.count()
    }

    override suspend fun getAccount(accountId: Int): UnifiedAddressAccount? = lazy.accounts.findAccountById(accountId)

    override suspend fun getAccountCount(): Int = lazy.accounts.count()

    override suspend fun prepare() {
        if (lazy.isPrepared.get()) {
            twig("Warning: skipped the preparation step because we're already prepared!")
        } else {
            twig("Preparing repository for use...")
            initMissingDatabases()
            // provide the database to all the lazy properties that are waiting for it to exist
            lazy.db = buildDatabase()
            applyKeyMigrations()
        }
    }

    /**
     * Create any databases that don't already exist via Rust. Originally, this was done on the Rust
     * side because Rust was intended to own the "dataDb" and Kotlin just reads from it. Since then,
     * it has been more clear that Kotlin should own the data and just let Rust use it.
     */
    private suspend fun initMissingDatabases() {
        maybeCreateDataDb()
        maybeInitBlocksTable(birthday)
        maybeInitAccountsTable(viewingKeys)
    }

    /**
     * Create the dataDb and its table, if it doesn't exist.
     */
    private suspend fun maybeCreateDataDb() {
        tryWarn("Warning: did not create dataDb. It probably already exists.") {
            rustBackend.initDataDb()
            twig("Initialized wallet for first run file: ${rustBackend.pathDataDb}")
        }
    }

    /**
     * Initialize the blocks table with the given birthday, if needed.
     */
    private suspend fun maybeInitBlocksTable(birthday: WalletBirthday) {
        // TODO: consider converting these to typed exceptions in the welding layer
        tryWarn(
            "Warning: did not initialize the blocks table. It probably was already initialized.",
            ifContains = "table is not empty"
        ) {
            rustBackend.initBlocksTable(
                birthday.height,
                birthday.hash,
                birthday.time,
                birthday.tree
            )
            twig("seeded the database with sapling tree at height ${birthday.height}")
        }
        twig("database file: ${rustBackend.pathDataDb}")
    }

    /**
     * Initialize the accounts table with the given viewing keys.
     */
    private suspend fun maybeInitAccountsTable(viewingKeys: List<UnifiedViewingKey>) {
        // TODO: consider converting these to typed exceptions in the welding layer
        tryWarn(
            "Warning: did not initialize the accounts table. It probably was already initialized.",
            ifContains = "table is not empty"
        ) {
            rustBackend.initAccountsTable(*viewingKeys.toTypedArray())
            twig("Initialized the accounts table with ${viewingKeys.size} viewingKey(s)")
        }
    }

    /**
     * Build the database and apply migrations.
     */
    private fun buildDatabase(): DerivedDataDb {
        twig("Building dataDb and applying migrations")
        return Room.databaseBuilder(appContext, DerivedDataDb::class.java, rustBackend.pathDataDb)
            .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
            .addMigrations(DerivedDataDb.MIGRATION_3_4)
            .addMigrations(DerivedDataDb.MIGRATION_4_3)
            .addMigrations(DerivedDataDb.MIGRATION_4_5)
            .addMigrations(DerivedDataDb.MIGRATION_5_6)
            .addMigrations(DerivedDataDb.MIGRATION_6_7)
            .build().also {
                // TODO: document why we do this. My guess is to catch database issues early or to trigger migrations--I forget why it was added but there was a good reason?
                it.openHelper.writableDatabase.beginTransaction()
                it.openHelper.writableDatabase.endTransaction()
            }
    }

    private suspend fun applyKeyMigrations() {
        if (overwriteVks) {
            twig("applying key migrations . . .")
            maybeInitAccountsTable(viewingKeys)
        }
    }

    /**
     * Close the underlying database.
     */
    suspend fun close() {
        withContext(Dispatchers.IO) {
            lazy.db?.close()
        }
    }

    // TODO: begin converting these into Data Access API. For now, just collect the desired operations and iterate/refactor, later
    fun findBlockHash(height: Int): ByteArray? = lazy.blocks.findHashByHeight(height)
    fun getTransactionCount(): Int = lazy.transactions.count()

    // TODO: convert this into a wallet repository rather than "transaction repository"

    /**
     * Helper class that holds all the properties that depend on the database being prepared. If any
     * properties are accessed before then, it results in an Unprepared Exception.
     */
    inner class LazyPropertyHolder {
        var isPrepared = AtomicBoolean(false)
        var db: DerivedDataDb? = null
            set(value) {
                field = value
                if (value != null) {
                    isPrepared.set(true)
                    trigger.value = true
                }
            }
        private val trigger = MutableStateFlow(false)

        // DAOs
        val blocks: BlockDao by lazyDb { db!!.blockDao() }
        val accounts: AccountDao by lazyDb { db!!.accountDao() }
        val transactions: TransactionDao by lazyDb { db!!.transactionDao() }

        // Transaction Flows
        val allTransactionsFactory: RefreshableDataSourceFactory<Int, ConfirmedTransaction> by lazyDb {
            transactions.getAllTransactions().toRefreshable()
        }

        val allTransactions = flow<List<ConfirmedTransaction>> {
            // emit a placeholder while we await the db to be set
            emit(emptyList())
            trigger.first { it }
            emitAll(allTransactionsFactory.toFlowPagedList(pageSize))
        }

        val receivedTransactions = flow<List<ConfirmedTransaction>> {
            // emit a placeholder while we await the db to be set
            emit(emptyList())
            trigger.first { it }
            emitAll(transactions.getReceivedTransactions().toRefreshable().toFlowPagedList(pageSize))
        }

        val sentTransactions = flow<List<ConfirmedTransaction>> {
            // emit a placeholder while we await the db to be set
            emit(emptyList())
            trigger.first { it }
            emitAll(transactions.getSentTransactions().toRefreshable().toFlowPagedList(pageSize))
        }

        /**
         * If isPrepared is true, execute the given block and cache the value, always returning it
         * to future requests. Otherwise, throw an Unprepared exception.
         */
        inline fun <T> lazyDb(crossinline block: () -> T) = object : Lazy<T> {
            val cached: T? = null
            override val value: T
                get() = cached ?: if (isPrepared.get()) block() else throw RepositoryException.Unprepared
            override fun isInitialized(): Boolean = cached != null
        }
    }
}
