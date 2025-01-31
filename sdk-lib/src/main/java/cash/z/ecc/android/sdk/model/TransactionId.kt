package cash.z.ecc.android.sdk.model

import cash.z.ecc.android.sdk.internal.ext.toHexReversed

/**
 * Typesafe wrapper class for the transaction ID identifier.
 *
 * @param value Byte array of the the transaction ID wrapped in [FirstClassByteArray]
 */
data class TransactionId internal constructor(
    val value: FirstClassByteArray,
) {
    init {
        require(value.byteArray.isNotEmpty()) {
            "Transaction ID must not be empty"
        }
    }

    companion object {
        fun new(byteArray: FirstClassByteArray) = TransactionId(byteArray)

        fun new(byteArray: ByteArray) = TransactionId(FirstClassByteArray(byteArray))
    }

    /**
     * @return Transaction ID in String
     */
    fun txIdString() = value.byteArray.toHexReversed()

    override fun toString() = "TransactionId"
}
