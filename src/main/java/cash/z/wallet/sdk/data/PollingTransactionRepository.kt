package cash.z.wallet.sdk.data

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import cash.z.wallet.sdk.db.*
import cash.z.wallet.sdk.entity.ClearedTransaction
import cash.z.wallet.sdk.entity.Transaction
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO

/**
 * Repository that does polling for simplicity. We will implement an alternative version that uses live data as well as
 * one that creates triggers and then reference them here. For now this is the most basic example of keeping track of
 * changes.
 */
open class PollingTransactionRepository(
    private val derivedDataDb: DerivedDataDb,
    private val pollFrequencyMillis: Long = 2000L,
    private val limit: Int = Int.MAX_VALUE
) : TransactionRepository {

    /**
     * Constructor that creates the database.
     */
    constructor(
        context: Context,
        dataDbName: String,
        pollFrequencyMillis: Long = 2000L
    ) : this(
        Room.databaseBuilder(context, DerivedDataDb::class.java, dataDbName)
            .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
            .build(),
        pollFrequencyMillis
    )

    private val blocks: BlockDao = derivedDataDb.blockDao()
    private val receivedNotes: ReceivedDao = derivedDataDb.receivedDao()
    private val sentNotes: SentDao = derivedDataDb.sentDao()
    private val transactions: TransactionDao = derivedDataDb.transactionDao()
    protected var pollingJob: Job? = null

    override fun lastScannedHeight(): Int {
        return blocks.lastScannedHeight()
    }

    override fun isInitialized(): Boolean {
        return blocks.count() > 0
    }

    override suspend fun findTransactionById(txId: Long): Transaction? = withContext(IO) {
        twig("finding transaction with id $txId on thread ${Thread.currentThread().name}")
        val transaction = transactions.findById(txId)
        twig("found ${transaction?.id}")
        transaction
    }

    override suspend fun findTransactionByRawId(rawTxId: ByteArray): Transaction? = withContext(IO) {
        transactions.findByRawId(rawTxId)
    }

    override suspend fun deleteTransactionById(txId: Long) = withContext(IO) {
        twigTask("deleting transaction with id $txId") {
            transactions.deleteById(txId)
        }
    }
    override suspend fun getClearedTransactions(): List<ClearedTransaction> = withContext(IO) {
        transactions.getSentTransactions(limit) + transactions.getReceivedTransactions(limit)
    }

    override suspend fun monitorChanges(listener: () -> Unit) = withContext(IO) {
        // since the only thing mutable is unmined transactions, we can simply check for new data rows rather than doing any deep comparisons
        // in the future we can leverage triggers instead
        pollingJob?.cancel()
        pollingJob = launch {
            val txCount = ValueHolder(-1, "Transaction Count")
            val unminedCount = ValueHolder(-1, "Unmined Transaction Count")
            val sentCount = ValueHolder(-1, "Sent Transaction Count")
            val receivedCount = ValueHolder(-1, "Received Transaction Count")

            while (coroutineContext.isActive) {
                // we check all conditions to avoid duplicate notifications whenever a change impacts multiple tables
                // if counting becomes slower than the blocktime (highly unlikely) then this could be optimized to call the listener early and continue counting afterward but there's no need for that complexity now
                if (txCount.changed(transactions.count())
                    || unminedCount.changed(transactions.countUnmined())
                    || sentCount.changed(sentNotes.count())
                    || receivedCount.changed(receivedNotes.count())
                ) {
                    twig("Notifying listener that changes have been detected in transactions!")
                    listener.invoke()
                } else {
                    twig("No changes detected in transactions.")
                }
                delay(pollFrequencyMillis)
            }
        }
    }

    fun stop() {
        pollingJob?.cancel().also { pollingJob = null }
        derivedDataDb.close()
    }

}

/**
 * Reduces some of the boilerplate of checking a value for changes.
 */
internal class ValueHolder<T>(var value: T, val description: String = "Value") {

    /**
     * Hold the new value and report whether it has changed.
     */
    fun changed(newValue: T): Boolean {
        return if (newValue == value) {
            false
        } else {
            twig("$description changed from $value to $newValue")
            value = newValue
            true
        }
    }
}
