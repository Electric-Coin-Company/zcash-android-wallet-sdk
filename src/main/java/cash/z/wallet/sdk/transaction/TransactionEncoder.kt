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

    suspend fun isValidShieldedAddress(address: String): Boolean
    suspend fun isValidTransparentAddress(address: String): Boolean
}
