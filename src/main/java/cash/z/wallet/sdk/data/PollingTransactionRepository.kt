package cash.z.wallet.sdk.data

import android.content.Context
import android.text.TextUtils
import androidx.room.Room
import androidx.room.RoomDatabase
import cash.z.wallet.sdk.dao.BlockDao
import cash.z.wallet.sdk.dao.TransactionDao
import cash.z.wallet.sdk.dao.WalletTransaction
import cash.z.wallet.sdk.db.DerivedDataDb
import cash.z.wallet.sdk.exception.RepositoryException
import cash.z.wallet.sdk.exception.RustLayerException
import cash.z.wallet.sdk.jni.JniConverter
import cash.z.wallet.sdk.entity.Transaction
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.distinct
import java.util.*

/**
 * Repository that does polling for simplicity. We will implement an alternative version that uses live data as well as
 * one that creates triggers and then reference them here. For now this is the most basic example of keeping track of
 * changes.
 */
open class PollingTransactionRepository(
    private val derivedDataDb: DerivedDataDb,
    private val derivedDataDbPath: String,
    private val converter: JniConverter,
    private val pollFrequencyMillis: Long = 2000L,
    logger: Twig = SilentTwig()
) : TransactionRepository, Twig by logger {

    /**
     * Constructor that creates the database and then executes a callback on it.
     */
    constructor(
        context: Context,
        dataDbName: String,
        pollFrequencyMillis: Long = 2000L,
        converter: JniConverter = JniConverter(),
        logger: Twig = SilentTwig(),
        dbCallback: (DerivedDataDb) -> Unit = {}
    ) : this(
        Room.databaseBuilder(context, DerivedDataDb::class.java, dataDbName)
            .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
            .fallbackToDestructiveMigration()
            .build(),
        context.getDatabasePath(dataDbName).absolutePath,
        converter,
        pollFrequencyMillis,
        logger
    ) {
        dbCallback(derivedDataDb)
    }

    internal val blocks: BlockDao = derivedDataDb.blockDao()
    private val transactions: TransactionDao = derivedDataDb.transactionDao()
    private lateinit var pollingJob: Job
    private val balanceChannel = ConflatedBroadcastChannel<Long>()
    private val allTransactionsChannel = ConflatedBroadcastChannel<List<WalletTransaction>>()
    private val existingTransactions = listOf<WalletTransaction>()
    private val wasPreviouslyStarted
        get() = !existingTransactions.isEmpty() || balanceChannel.isClosedForSend || allTransactionsChannel.isClosedForSend

    override fun start(parentScope: CoroutineScope) {
        //  prevent restarts so the behavior of this class is easier to reason about
        if (wasPreviouslyStarted) throw RepositoryException.FalseStart

        twig("starting")

        pollingJob = parentScope.launch {
            poll()
        }
    }

    override fun stop() {
        twig("stopping")
        // when polling ends, we call stop which can result in a duplicate call to stop
        // So keep stop idempotent, rather than crashing with "Channel was closed" errors
        // probably should make a safeCancel extension function for this and use it everywhere that we cancel a channel
        try{ if (!balanceChannel.isClosedForSend) balanceChannel.cancel() }catch (t:Throwable){}
        try{ if (!allTransactionsChannel.isClosedForSend) allTransactionsChannel.cancel() }catch (t:Throwable){}
        try{ if (!pollingJob.isCancelled) pollingJob.cancel() }catch (t:Throwable){}
    }

    override fun balance(): ReceiveChannel<Long> {
        return balanceChannel.openSubscription().distinct()
    }

    override fun allTransactions(): ReceiveChannel<List<WalletTransaction>> {
        return allTransactionsChannel.openSubscription()
    }

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

    override suspend fun deleteTransactionById(txId: Long) = withContext(IO) {
        twigTask("deleting transaction with id ${txId}") {
            transactions.deleteById(txId)
        }
    }

    private suspend fun poll() = withContext(IO) {
        try {
            var previousTransactions: List<WalletTransaction>? = null
            while (isActive
                && !balanceChannel.isClosedForSend
                && !allTransactionsChannel.isClosedForSend
            ) {
                twigTask("polling for transactions") {
                    val newTransactions = transactions.getAll()

                    if (hasChanged(previousTransactions, newTransactions)) {
                        twig("loaded ${newTransactions.count()} transactions and changes were detected!")
                        allTransactionsChannel.send(newTransactions)
                        sendLatestBalance()
                        previousTransactions = newTransactions
                    } else {
                        twig("loaded ${newTransactions.count()} transactions but no changes detected.")
                    }
                }
                delay(pollFrequencyMillis)
            }
        } finally {
            stop()
        }
    }

    private fun hasChanged(oldTxs: List<WalletTransaction>?, newTxs: List<WalletTransaction>): Boolean {
        fun pr(t: List<WalletTransaction>?): String {
            if(t == null) return "none"
            val str = StringBuilder()
            for (tx in t) {
                str.append("\n@TWIG: ").append(tx.toString())
            }
            return str.toString()
        }
        val sends = newTxs.filter { it.isSend }
        if(sends.isNotEmpty()) twig("SENDS hasChanged: old-txs: ${pr(oldTxs?.filter { it.isSend })}\n@TWIG: new-txs: ${pr(sends)}")

        // shortcuts first
        if (newTxs.isEmpty() && oldTxs == null) return false.also { twig("detected nothing happened yet") } // if nothing has happened, that doesn't count as a change
        if (oldTxs == null) return true.also { twig("detected first set of txs!") } // the first set of transactions is automatically a change
        if (oldTxs.size != newTxs.size) return true.also { twig("detected size difference") } // can't be the same and have different sizes, duh

        for (note in newTxs) {
            if (!oldTxs.contains(note)) return true.also { twig("detected change for $note") }
        }
        return false.also { twig("detected no changes in all new txs") }
    }

    private suspend fun sendLatestBalance() = withContext(IO) {
        twigTask("sending balance") {
            try {
                // TODO: use wallet here
                val balance = converter.getBalance(derivedDataDbPath,  0)
                twig("balance: $balance")
                balanceChannel.send(balance)
            } catch (t: Throwable) {
                twig("failed to get balance due to $t")
                throw RustLayerException.BalanceException(t)
            }
        }
    }
}