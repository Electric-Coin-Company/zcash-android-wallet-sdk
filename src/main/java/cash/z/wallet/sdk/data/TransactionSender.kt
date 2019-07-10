package cash.z.wallet.sdk.data

import cash.z.wallet.sdk.db.PendingTransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.SendChannel

interface TransactionSender {
    fun start(scope: CoroutineScope)
    fun stop()
    fun notifyOnChange(channel: SendChannel<List<PendingTransactionEntity>>)
    /** only necessary when there is a long delay between starting a transaction and beginning to create it. Like when sweeping a wallet that first needs to be scanned. */
    suspend fun prepareTransaction(amount: Long, address: String, memo: String): PendingTransactionEntity?
    suspend fun sendPreparedTransaction(encoder: RawTransactionEncoder, tx: PendingTransactionEntity): PendingTransactionEntity
    suspend fun cleanupPreparedTransaction(tx: PendingTransactionEntity)
    suspend fun sendToAddress(encoder: RawTransactionEncoder, zatoshi: Long, toAddress: String, memo: String = "", fromAccountId: Int = 0): PendingTransactionEntity
    suspend fun cancel(existingTransaction: PendingTransactionEntity): Unit?

    var onSubmissionError: ((Throwable) -> Unit)?
}