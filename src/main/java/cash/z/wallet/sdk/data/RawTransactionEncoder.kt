package cash.z.wallet.sdk.data

interface RawTransactionEncoder {
    /**
     * Creates a raw transaction that is unsigned.
     */
    suspend fun create(zatoshi: Long, toAddress: String, memo: String = ""): EncodedTransaction
}

data class EncodedTransaction(val txId: ByteArray, val raw: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EncodedTransaction) return false

        if (!txId.contentEquals(other.txId)) return false
        if (!raw.contentEquals(other.raw)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = txId.contentHashCode()
        result = 31 * result + raw.contentHashCode()
        return result
    }
}
