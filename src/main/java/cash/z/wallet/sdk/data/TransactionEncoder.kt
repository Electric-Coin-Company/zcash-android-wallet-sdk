package cash.z.wallet.sdk.data

import cash.z.wallet.sdk.entity.EncodedTransaction

interface TransactionEncoder {
    /**
     * Creates a signed transaction
     */
    suspend fun create(zatoshi: Long, toAddress: String, memo: String = ""): EncodedTransaction
}
