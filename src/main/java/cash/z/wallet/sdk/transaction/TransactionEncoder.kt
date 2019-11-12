package cash.z.wallet.sdk.transaction

import cash.z.wallet.sdk.entity.EncodedTransaction

interface TransactionEncoder {
    /**
     * Creates a signed transaction
     */
    suspend fun createTransaction(
        spendingKey: String,
        zatoshi: Long,
        toAddress: String,
        memo: ByteArray? = byteArrayOf(),
        fromAccountIndex: Int = 0
    ): EncodedTransaction
}
