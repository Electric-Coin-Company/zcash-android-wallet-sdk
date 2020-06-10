package cash.z.ecc.android.sdk.transaction

import cash.z.ecc.android.sdk.db.entity.EncodedTransaction

interface TransactionEncoder {
    /**
     * Creates a transaction, throwing an exception whenever things are missing. When the provided
     * wallet implementation doesn't throw an exception, we wrap the issue into a descriptive
     * exception ourselves (rather than using double-bangs for things).
     *
     * @param spendingKey the key associated with the notes that will be spent.
     * @param zatoshi the amount of zatoshi to send.
     * @param toAddress the recipient's address.
     * @param memo the optional memo to include as part of the transaction.
     * @param fromAccountIndex the optional account id to use. By default, the 1st account is used.
     *
     * @return the successfully encoded transaction or an exception
     */
    suspend fun createTransaction(
        spendingKey: String,
        zatoshi: Long,
        toAddress: String,
        memo: ByteArray? = byteArrayOf(),
        fromAccountIndex: Int = 0
    ): EncodedTransaction

    /**
     * Utility function to help with validation. This is not called during [createTransaction]
     * because this class asserts that all validation is done externally by the UI, for now.
     *
     * @param address the address to validate
     *
     * @return true when the given address is a valid z-addr
     */
    suspend fun isValidShieldedAddress(address: String): Boolean

    /**
     * Utility function to help with validation. This is not called during [createTransaction]
     * because this class asserts that all validation is done externally by the UI, for now.
     *
     * @param address the address to validate
     *
     * @return true when the given address is a valid t-addr
     */
    suspend fun isValidTransparentAddress(address: String): Boolean

    /**
     * Return the consensus branch that the encoder is using when making transactions.
     */
    suspend fun getConsensusBranchId(): Long
}
