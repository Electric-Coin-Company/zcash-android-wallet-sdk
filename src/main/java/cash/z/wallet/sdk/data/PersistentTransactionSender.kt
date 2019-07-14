package cash.z.wallet.sdk.data

import cash.z.wallet.sdk.data.PersistentTransactionSender.ChangeType.*
import cash.z.wallet.sdk.data.TransactionUpdateRequest.RefreshSentTx
import cash.z.wallet.sdk.data.TransactionUpdateRequest.SubmitPendingTx
import cash.z.wallet.sdk.entity.PendingTransaction
import cash.z.wallet.sdk.entity.isMined
import cash.z.wallet.sdk.entity.isPending
import cash.z.wallet.sdk.ext.retryWithBackoff
import cash.z.wallet.sdk.service.LightWalletService
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlin.math.min


/**
 * Monitors pending transactions and sends or retries them, when appropriate.
 */
class PersistentTransactionSender (
    private val manager: TransactionManager,
    private val service: LightWalletService,
    private val ledger: TransactionRepository
) : TransactionSender {

    private lateinit var channel: SendChannel<TransactionUpdateRequest>
    private var monitoringJob: Job? = null
    private val initialMonitorDelay = 45_000L
    private var listenerChannel: SendChannel<List<PendingTransaction>>? = null
    override var onSubmissionError: ((Throwable) -> Boolean)? = null
    private var updateResult: CompletableDeferred<ChangeType>? = null
    var lastChangeDetected: ChangeType = NoChange(0)
        set(value) {
            field = value
            val details = when(value) {
                is SizeChange -> " from ${value.oldSize} to ${value.newSize}"
                is Modified -> " The culprit: ${value.tx}"
                is NoChange -> " for the ${value.count.asOrdinal()} time"
                else -> ""
            }
            twig("Checking pending tx detected: ${value.description}$details")
            updateResult?.complete(field)
        }

    fun CoroutineScope.requestUpdate(triggerSend: Boolean) = launch {
        if (!channel.isClosedForSend) {
            channel.send(if (triggerSend) SubmitPendingTx else RefreshSentTx)
        } else {
            twig("request ignored because the channel is closed for send!!!")
        }
    }

    /**
     * Start an actor that listens for signals about what to do with transactions. This actor's lifespan is within the
     * provided [scope] and it will live until the scope is cancelled.
     */
    private fun CoroutineScope.startActor() = actor<TransactionUpdateRequest> {
        var pendingTransactionDao = 0 // actor state:
        for (msg in channel) { // iterate over incoming messages
            when (msg) {
                is SubmitPendingTx -> updatePendingTransactions()
                is RefreshSentTx -> refreshSentTransactions()
            }
        }
    }

    private fun CoroutineScope.startMonitor() = launch {
        delay(5000) // todo see if we need a formal initial delay
        while (!channel.isClosedForSend && isActive) {
            // TODO: consider refactoring this since we actually want to wait on the return value of requestUpdate
            updateResult = CompletableDeferred()
            requestUpdate(true)
            updateResult?.await()
            delay(calculateDelay())
        }
        twig("TransactionMonitor stopping!")
    }

    private fun calculateDelay(): Long {
        // if we're actively waiting on results, then poll faster
        val delay = when (lastChangeDetected) {
            FirstChange -> initialMonitorDelay / 4
            is NothingPending, is NoChange -> {
                // simple linear offset when there has been no change
                val count = (lastChangeDetected as? BackoffEnabled)?.count ?: 0
                val offset = initialMonitorDelay / 5L * count
                if (previousSentTxs?.isNotEmpty() == true) {
                    initialMonitorDelay / 4
                } else {
                    initialMonitorDelay
                } + offset
            }
            is SizeChange -> initialMonitorDelay / 4
            is Modified -> initialMonitorDelay / 4
        }
        return min(delay, initialMonitorDelay * 8).also {
            twig("Checking for pending tx changes again in ${it/1000L}s")
        }
    }

    override fun start(scope: CoroutineScope) {
        twig("TransactionMonitor starting!")
        channel = scope.startActor()
        monitoringJob?.cancel()
        monitoringJob = scope.startMonitor()
    }

    override fun stop() {
        channel.close()
        monitoringJob?.cancel()?.also { monitoringJob = null }
        manager.stop()
    }

    override fun notifyOnChange(channel: SendChannel<List<PendingTransaction>>) {
        if (channel != null) twig("warning: listener channel was not null but it probably should have been. Something else was listening with $channel!")
        listenerChannel = channel
    }

    /**
     * Generates newly persisted information about a transaction so that other processes can send.
     */
    override suspend fun sendToAddress(
        encoder: TransactionEncoder,
        zatoshi: Long,
        toAddress: String,
        memo: String,
        fromAccountId: Int
    ): PendingTransaction = withContext(IO) {
        val currentHeight = service.safeLatestBlockHeight()
        (manager as PersistentTransactionManager).manageCreation(encoder, zatoshi, toAddress, memo, currentHeight).also {
            requestUpdate(true)
        }
    }

    override suspend fun prepareTransaction(
        zatoshiValue: Long,
        address: String,
        memo: String
    ): PendingTransaction? = withContext(IO) {
        (manager as PersistentTransactionManager).initPlaceholder(zatoshiValue, address, memo).also {
            // update UI to show what we've just created. No need to submit, it has no raw data yet!
            requestUpdate(false)
        }
    }

    override suspend fun sendPreparedTransaction(
        encoder: TransactionEncoder,
        tx: PendingTransaction
    ): PendingTransaction = withContext(IO) {
        val currentHeight = service.safeLatestBlockHeight()
        (manager as PersistentTransactionManager).manageCreation(encoder, tx, currentHeight).also {
            // submit what we've just created
            requestUpdate(true)
        }
    }

    override suspend fun cleanupPreparedTransaction(tx: PendingTransaction) {
        if (tx.raw.isEmpty()) {
            (manager as PersistentTransactionManager).abortTransaction(tx)
        }
    }

    //  TODO: get this from the channel instead
    var previousSentTxs: List<PendingTransaction>? = null

    private suspend fun notifyIfChanged(currentSentTxs: List<PendingTransaction>) = withContext(IO) {
        if (hasChanged(previousSentTxs, currentSentTxs) && listenerChannel?.isClosedForSend != true) {
            twig("START notifying listenerChannel of changed txs")
            listenerChannel?.send(currentSentTxs)
            twig("DONE notifying listenerChannel of changed txs")
            previousSentTxs = currentSentTxs
        } else {
            twig("notifyIfChanged: did nothing because ${if(listenerChannel?.isClosedForSend == true) "the channel is closed." else "nothing changed."}")
        }
    }

    override suspend fun cancel(existingTransaction: PendingTransaction) = withContext(IO) {
        (manager as PersistentTransactionManager).abortTransaction(existingTransaction). also {
            requestUpdate(false)
        }
    }

    private fun hasChanged(
        previousSents: List<PendingTransaction>?,
        currentSents: List<PendingTransaction>
    ): Boolean {
        // shortcuts first
        if (currentSents.isEmpty() && previousSents.isNullOrEmpty()) return false.also {
            val count = if (lastChangeDetected is BackoffEnabled) ((lastChangeDetected as? BackoffEnabled)?.count ?: 0) + 1 else 1
            lastChangeDetected = NothingPending(count)
        }
        if (previousSents == null) return true.also { lastChangeDetected = FirstChange }
        if (previousSents.size != currentSents.size) return true.also { lastChangeDetected = SizeChange(previousSentTxs?.size ?: -1, currentSents.size) }
        for (tx in currentSents) {
            // note: implicit .equals check inside `contains` will also detect modifications
            if (!previousSents.contains(tx)) return true.also { lastChangeDetected = Modified(tx) }
        }
        return false.also {
            val count = if (lastChangeDetected is BackoffEnabled) ((lastChangeDetected as? BackoffEnabled)?.count ?: 0) + 1 else 1
            lastChangeDetected = NoChange(count)
        }
    }

    sealed class ChangeType(val description: String) {
        object FirstChange : ChangeType("This is the first time we've seen a change!")
        data class NothingPending(override val count: Int) : ChangeType("Nothing happened yet!"), BackoffEnabled
        data class NoChange(override val count: Int) : ChangeType("No changes"), BackoffEnabled
        class SizeChange(val oldSize: Int, val newSize: Int) : ChangeType("The total number of pending transactions has changed")
        class Modified(val tx: PendingTransaction) : ChangeType("At least one transaction has been modified")
    }
    interface BackoffEnabled {
        val count: Int
    }

    /**
     * Check on all sent transactions and if they've changed, notify listeners. This method can be called proactively
     * when anything interesting has occurred with a transaction (via [requestUpdate]).
     */
    private suspend fun refreshSentTransactions(): List<PendingTransaction> = withContext(IO) {
        val allSentTransactions = (manager as PersistentTransactionManager).getAll() // TODO: make this crash and catch error gracefully
        notifyIfChanged(allSentTransactions)
        allSentTransactions
    }

    /**
     * Submit all pending transactions that have not expired.
     */
    private suspend fun updatePendingTransactions() = withContext(IO) {
        try {
            val allTransactions = refreshSentTransactions()
            var pendingCount = 0
            val currentHeight = service.safeLatestBlockHeight()
            allTransactions.filter { !it.isMined() }.forEach { tx ->
                if (tx.isPending(currentHeight)) {
                    pendingCount++
                    retryWithBackoff(onSubmissionError, 1000L, 60_000L) {
                        manager.manageSubmission(service, tx)
                    }
                } else {
                    tx.rawTransactionId?.let {
                        ledger.findTransactionByRawId(tx.rawTransactionId)
                    }?.let {
                        twig("matching transaction found! $tx")
                        (manager as PersistentTransactionManager).manageMined(tx, it)
                        refreshSentTransactions()
                    }
                }
            }
            twig("given current height $currentHeight, we found $pendingCount pending txs to submit")
        } catch (t: Throwable) {
            twig("Error during updatePendingTransactions: $t caused by ${t.cause}")
        }
    }
}

private fun Int.asOrdinal(): String {
    return "$this" + if (this % 100 in 11..13) "th" else when(this % 10) {
        1 -> "st"
        2 -> "nd"
        3 -> "rd"
        else -> "th"
    }
}

private fun LightWalletService.safeLatestBlockHeight(): Int {
    return try {
        getLatestBlockHeight()
    } catch (t: Throwable) {
        twig("Warning: LightWalletService failed to return the latest height and we are returning -1 instead.")
        -1
    }
}

sealed class TransactionUpdateRequest {
    object SubmitPendingTx : TransactionUpdateRequest()
    object RefreshSentTx : TransactionUpdateRequest()
}