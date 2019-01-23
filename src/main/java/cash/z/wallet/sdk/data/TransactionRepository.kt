package cash.z.wallet.sdk.data

import cash.z.wallet.sdk.vo.NoteQuery
import cash.z.wallet.sdk.vo.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ReceiveChannel
import java.math.BigDecimal

interface TransactionRepository {
    fun start(parentScope: CoroutineScope)
    fun stop()
    fun balance(): ReceiveChannel<Long>
    fun allTransactions(): ReceiveChannel<List<NoteQuery>>
    fun lastScannedHeight(): Long
    suspend fun findTransactionById(txId: Long): Transaction?
    suspend fun deleteTransactionById(txId: Long)
}