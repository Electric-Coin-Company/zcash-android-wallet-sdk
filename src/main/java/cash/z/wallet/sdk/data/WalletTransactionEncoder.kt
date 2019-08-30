package cash.z.wallet.sdk.data

import cash.z.wallet.sdk.entity.EncodedTransaction
import cash.z.wallet.sdk.exception.TransactionNotEncodedException
import cash.z.wallet.sdk.exception.TransactionNotFoundException
import cash.z.wallet.sdk.secure.Wallet
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext

class WalletTransactionEncoder(
    private val wallet: Wallet,
    private val repository: TransactionRepository
) : TransactionEncoder {

    /**
     * Creates a transaction, throwing an exception whenever things are missing. When the provided wallet implementation
     * doesn't throw an exception, we wrap the issue into a descriptive exception ourselves (rather than using
     * double-bangs for things).
     */
    override suspend fun create(zatoshi: Long, toAddress: String, memo: String): EncodedTransaction = withContext(IO) {
        val transactionId = wallet.createRawSendTransaction(zatoshi, toAddress, memo)
        val transaction = repository.findTransactionById(transactionId)
            ?: throw TransactionNotFoundException(transactionId)
        EncodedTransaction(transaction.transactionId, transaction.raw
            ?: throw TransactionNotEncodedException(transactionId)
        )
    }
}
