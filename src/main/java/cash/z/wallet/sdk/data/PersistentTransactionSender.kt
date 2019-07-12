package cash.z.wallet.sdk.data

import cash.z.wallet.sdk.dao.ClearedTransaction
import cash.z.wallet.sdk.data.TransactionUpdateRequest.RefreshSentTx
import cash.z.wallet.sdk.data.TransactionUpdateRequest.SubmitPendingTx
import cash.z.wallet.sdk.db.PendingTransaction
import cash.z.wallet.sdk.db.isMined
import cash.z.wallet.sdk.db.isPending
import cash.z.wallet.sdk.service.LightWalletService
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor

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
    override var onSubmissionError: ((Throwable) -> Unit)? = null

    fun CoroutineScope.requestUpdate(triggerSend: Boolean) = launch {
        twig("requesting update: $triggerSend")
        if (!channel.isClosedForSend) {
            twig("submitting request")
            channel.send(if (triggerSend) SubmitPendingTx else RefreshSentTx)
            twig("done submitting request")
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
            twig("actor received message: ${msg.javaClass.simpleName}")
            when (msg) {
                is SubmitPendingTx -> updatePendingTransactions()
                is RefreshSentTx -> refreshSentTransactions()
            }
        }
    }

    private fun CoroutineScope.startMonitor() = launch {
        delay(5000) // todo see if we need a formal initial delay
        while (!channel.isClosedForSend && isActive) {
            requestUpdate(true)
            delay(calculateDelay())
        }
        twig("TransactionMonitor stopping!")
    }

    private fun calculateDelay(): Long {
        return initialMonitorDelay
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
        encoder: RawTransactionEncoder,
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
        encoder: RawTransactionEncoder,
        tx: PendingTransaction
    ): PendingTransaction = withContext(IO) {
        val currentHeight = service.safeLatestBlockHeight()
        (manager as PersistentTransactionManager).manageCreation(encoder, tx, currentHeight).also {
            // submit what we've just created
            requestUpdate(true)
        }
    }

    override suspend fun cleanupPreparedTransaction(tx: PendingTransaction) {
        if (tx.raw == null) {
            (manager as PersistentTransactionManager).abortTransaction(tx)
        }
    }

    //  TODO: get this from the channel instead
    var previousSentTxs: List<PendingTransaction>? = null

    private suspend fun notifyIfChanged(currentSentTxs: List<PendingTransaction>) = withContext(IO) {
        twig("notifyIfChanged: listener null? ${listenerChannel == null} closed? ${listenerChannel?.isClosedForSend}")
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
        if (currentSents.isEmpty() && previousSents == null) return false.also { twig("checking pending txs: detected nothing happened yet") } // if nothing has happened, that doesn't count as a change
        if (previousSents == null) return true.also { twig("checking pending txs: detected first set of txs!") } // the first set of transactions is automatically a change
        if (previousSents.size != currentSents.size) return true.also { twig("checking pending txs: detected size change from ${previousSents.size} to ${currentSents.size}") } // can't be the same and have different sizes, duh

        for (tx in currentSents) {
            if (!previousSents.contains(tx)) return true.also { twig("checking pending txs: detected change for $tx") }
        }
        return false.also { twig("checking pending txs: detected no changes in pending txs") }
    }

    /**
     * Check on all sent transactions and if they've changed, notify listeners. This method can be called proactively
     * when anything interesting has occurred with a transaction (via [requestUpdate]).
     */
    private suspend fun refreshSentTransactions(): List<PendingTransaction> = withContext(IO) {
        twig("refreshing all sent transactions")
        val allSentTransactions = (manager as PersistentTransactionManager).getAll() // TODO: make this crash and catch error gracefully
        notifyIfChanged(allSentTransactions)
        allSentTransactions
    }

    /**
     * Submit all pending transactions that have not expired.
     */
    private suspend fun updatePendingTransactions() = withContext(IO) {
        try {
            twig("received request to submit pending transactions")
            val allTransactions = refreshSentTransactions()
            var pendingCount = 0
            val currentHeight = service.safeLatestBlockHeight()
            allTransactions.filter { !it.isMined() }.forEach { tx ->
                if (tx.isPending(currentHeight)) {
                    pendingCount++
                    try {
                        manager.manageSubmission(service, tx)
                    } catch (t: Throwable) {
                        twig("Warning: manageSubmission failed")
                        onSubmissionError?.invoke(t)
                    }
                } else {
                    findMatchingClearedTx(tx)?.let {
                        twig("matching cleared transaction found! $tx")
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

    private fun findMatchingClearedTx(tx: PendingTransaction): PendingTransaction? {
        return if (tx.txId == null) null else {
            (ledger as PollingTransactionRepository)
                .findTransactionByRawId(tx.txId)?.firstOrNull()?.toPendingTransactionEntity()
        }
    }
}

private fun ClearedTransaction?.toPendingTransactionEntity(): PendingTransaction? {
    if(this == null) return null
    return PendingTransaction(
        address = address ?: "",
        value = value,
        memo = memo ?: "",
        minedHeight = height ?: -1,
        txId = rawTransactionId
    )
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



private fun String?.toTxError(): TransactionError {
    return FailedTransaction("$this")
}

data class FailedTransaction(override val message: String) : TransactionError

/*
states:
** creating
** failed to create
CREATED
EXPIRED
MINED
SUBMITTED
INVALID
** attempting submission
** attempted submission

bookkeeper, register, treasurer, mint, ledger


    private fun checkTx(transactionId: Long) {
        if (transactionId < 0) {
            throw SweepException.Creation
        } else {
            twig("successfully created transaction!")
        }
    }

    private fun checkRawTx(transactionRaw: ByteArray?) {
        if (transactionRaw == null) {
            throw SweepException.Disappeared
        } else {
            twig("found raw transaction in the dataDb")
        }
    }

    private fun checkResponse(response: Service.SendResponse) {
        if (response.errorCode < 0) {
            throw SweepException.IncompletePass(response)
        } else {
            twig("successfully submitted. error code: ${response.errorCode}")
        }
    }

    sealed class SweepException(val errorMessage: String) : RuntimeException(errorMessage) {
        object Creation : SweepException("failed to create raw transaction")
        object Disappeared : SweepException("unable to find a matching raw transaction. This means the rust backend said it created a TX but when we looked for it in the DB it was missing!")
        class IncompletePass(response: Service.SendResponse) : SweepException("submit failed with error code: ${response.errorCode} and message ${response.errorMessage}")
    }

 */