package cash.z.wallet.sdk.data

import cash.z.wallet.sdk.secure.Wallet
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext

class WalletTransactionEncoder(
    private val wallet: Wallet,
    private val repository: TransactionRepository
) : RawTransactionEncoder {

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
            ?: throw TransactionNotEncodedException(transactionId))
    }
}

class TransactionNotFoundException(transactionId: Long) : RuntimeException("Unable to find transactionId " +
        "$transactionId in the repository. This means the wallet created a transaction and then returned a row ID " +
        "that does not actually exist. This is a scenario where the wallet should have thrown an exception but failed " +
        "to do so.")

class TransactionNotEncodedException(transactionId: Long) : RuntimeException("The transaction returned by the wallet," +
        " with id $transactionId, does not have any raw data. This is a scenario where the wallet should have thrown" +
        " an exception but failed to do so.")