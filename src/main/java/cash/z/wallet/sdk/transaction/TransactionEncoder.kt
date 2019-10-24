package cash.z.wallet.sdk.transaction

import cash.z.wallet.sdk.entity.EncodedTransaction

interface TransactionEncoder {
    /**
     * Creates a signed transaction
     */
    suspend fun create(zatoshi: Long, toAddress: String, memo: String = ""): EncodedTransaction
}
