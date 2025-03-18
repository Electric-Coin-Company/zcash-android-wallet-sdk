package cash.z.ecc.android.sdk.model

class Pczt(
    private val inner: ByteArray
) {
    /**
     * Exposes this PCZT's serialized [ByteArray] for conveyance purposes.
     */
    fun toByteArray(): ByteArray = inner

    /**
     * Clones this object with its inner data
     */
    fun clonePczt() = Pczt(this.toByteArray().copyOf())

    // Override to prevent leaking data in logs
    override fun toString() = "Pczt(size=${inner.size})"
}
