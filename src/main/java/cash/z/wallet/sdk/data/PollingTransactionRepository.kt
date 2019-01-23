package cash.z.wallet.sdk.data

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import cash.z.wallet.sdk.dao.BlockDao
import cash.z.wallet.sdk.dao.NoteDao
import cash.z.wallet.sdk.dao.TransactionDao
import cash.z.wallet.sdk.db.DerivedDataDb
import cash.z.wallet.sdk.exception.RepositoryException
import cash.z.wallet.sdk.exception.RustLayerException
import cash.z.wallet.sdk.jni.JniConverter
import cash.z.wallet.sdk.vo.NoteQuery
import cash.z.wallet.sdk.vo.Transaction
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.distinct

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

    private val notes: NoteDao = derivedDataDb.noteDao()
    internal val blocks: BlockDao = derivedDataDb.blockDao()
    private val transactions: TransactionDao = derivedDataDb.transactionDao()
    private lateinit var pollingJob: Job
    private val balanceChannel = ConflatedBroadcastChannel<Long>()
    private val allTransactionsChannel = ConflatedBroadcastChannel<List<NoteQuery>>()
    val existingTransactions = listOf<NoteQuery>()
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
        balanceChannel.cancel()
        allTransactionsChannel.cancel()
        pollingJob.cancel()
    }

    override fun balance(): ReceiveChannel<Long> {
        return balanceChannel.openSubscription().distinct()
    }

    override fun allTransactions(): ReceiveChannel<List<NoteQuery>> {
        return allTransactionsChannel.openSubscription()
    }

    override fun lastScannedHeight(): Long {
        return blocks.lastScannedHeight()
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
            var previousNotes: List<NoteQuery>? = null
            while (isActive
                && !balanceChannel.isClosedForSend
                && !allTransactionsChannel.isClosedForSend
            ) {
                twigTask("polling for transactions") {
                    val newNotes = notes.getAll()

                    if (hasChanged(previousNotes, newNotes)) {
                        twig("loaded ${notes.count()} transactions and changes were detected!")
                        allTransactionsChannel.send(newNotes)
                        sendLatestBalance()
                        previousNotes = newNotes
                    } else {
                        twig("loaded ${notes.count()} transactions but no changes detected.")
                    }
                }
                delay(pollFrequencyMillis)
            }
        } finally {
            stop()
        }
    }

    private fun hasChanged(oldNotes: List<NoteQuery>?, newNotes: List<NoteQuery>): Boolean {
        // shortcuts first
        if (newNotes.isEmpty() && oldNotes == null) return false // if nothing has happened, that doesn't count as a change
        if (oldNotes == null) return true
        if (oldNotes.size != newNotes.size) return true

        for (note in newNotes) {
            if (!oldNotes.contains(note)) return true
        }
        return false
    }


//    private suspend fun poll() = withContext(IO) {
//        try {
//            while (isActive && !transactionChannel.isClosedForSend && !balanceChannel.isClosedForSend && !allTransactionsChannel.isClosedForSend) {
//                twigTask("polling for transactions") {
//                    val newTransactions = checkForNewTransactions()
//                    newTransactions?.takeUnless { it.isEmpty() }?.forEach {
//                        existingTransactions.union(listOf(it))
//                        transactionChannel.send(it)
//                        allTransactionsChannel.send(existingTransactions)
//                    }?.also {
//                        twig("discovered ${newTransactions?.size} transactions!")
//                        // only update the balance when we've had some new transactions
//                        sendLatestBalance()
//                    }
//                }
//                delay(pollFrequencyMillis)
//            }
//        } finally {
//            // if the job is cancelled, it should be the same as the repository stopping.
//            // otherwise, it over-complicates things and makes it harder to reason about the behavior of this class.
//            stop()
//        }
//    }
//
//    protected open fun checkForNewTransactions(): Set<NoteQuery>? {
//        val notes = notes.getAll()
//        twig("object $this : checking for new transactions. previousCount: ${existingTransactions.size}   currentCount: ${notes.size}")
//        return notes.subtract(existingTransactions)
//    }

    private suspend fun sendLatestBalance() = withContext(IO) {
        twigTask("sending balance") {
            try {
                val balance = converter.getBalance(derivedDataDbPath)
                twig("balance: $balance")
                balanceChannel.send(balance)
            } catch (t: Throwable) {
                twig("failed to get balance due to $t")
                throw RustLayerException.BalanceException(t)
            }
        }
    }
}