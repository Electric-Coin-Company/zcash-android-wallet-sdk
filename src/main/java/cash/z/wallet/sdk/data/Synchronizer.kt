package cash.z.wallet.sdk.data

import cash.z.wallet.sdk.dao.WalletTransaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel

interface Synchronizer {

    /* Lifecycle */
    fun start(parentScope: CoroutineScope): Synchronizer
    fun stop()

    /* Channels */
    // NOTE: each of these are expected to be a broadcast channel, such that [receive] always returns the latest value
    fun activeTransactions(): ReceiveChannel<Map<ActiveTransaction, TransactionState>>
    fun allTransactions(): ReceiveChannel<List<WalletTransaction>>
    fun balance(): ReceiveChannel<Long>
    fun progress(): ReceiveChannel<Int>

    /* Status */
    suspend fun isOutOfSync(): Boolean
    suspend fun isFirstRun(): Boolean
    val address: String

    /**
     * Called whenever there is an uncaught exception.
     *
     * @return true when the error has been handled and the Synchronizer should continue. False when the error is
     * unrecoverable and the Synchronizer should [stop].
     */
    var onSynchronizerErrorListener: ((Throwable?) -> Boolean)?

    /* Operations */
    suspend fun sendToAddress(zatoshi: Long, toAddress: String)
    fun cancelSend(transaction: ActiveSendTransaction): Boolean
}