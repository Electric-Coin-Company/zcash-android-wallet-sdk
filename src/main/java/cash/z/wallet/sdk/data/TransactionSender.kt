package cash.z.wallet.sdk.data

import cash.z.wallet.sdk.entity.PendingTransaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.SendChannel

interface TransactionSender {
    fun start(scope: CoroutineScope)
    fun stop()
    fun notifyOnChange(channel: SendChannel<List<PendingTransaction>>)
    /** only necessary when there is a long delay between starting a transaction and beginning to create it. Like when sweeping a wallet that first needs to be scanned. */
    suspend fun prepareTransaction(amount: Long, address: String, memo: String): PendingTransaction?
    suspend fun sendPreparedTransaction(encoder: TransactionEncoder, tx: PendingTransaction): PendingTransaction
    suspend fun cleanupPreparedTransaction(tx: PendingTransaction)
    suspend fun sendToAddress(encoder: TransactionEncoder, zatoshi: Long, toAddress: String, memo: String = "", fromAccountId: Int = 0): PendingTransaction
    suspend fun cancel(existingTransaction: PendingTransaction): Unit?

    var onSubmissionError: ((Throwable) -> Boolean)?
}