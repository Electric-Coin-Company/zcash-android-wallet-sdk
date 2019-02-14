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
    fun getAddress(): String

    /* Operations */
    suspend fun sendToAddress(zatoshi: Long, toAddress: String)
    fun cancelSend(transaction: ActiveSendTransaction): Boolean
}