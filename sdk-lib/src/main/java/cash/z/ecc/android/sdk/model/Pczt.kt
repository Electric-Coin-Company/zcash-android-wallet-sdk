package cash.z.ecc.android.sdk.model

class Pczt(
    private val inner: ByteArray
) {
    /**
     * Exposes this PCZT's serialized [ByteArray] for conveyance purposes.
     */
    fun toByteArray(): ByteArray {
        return inner
    }
}
