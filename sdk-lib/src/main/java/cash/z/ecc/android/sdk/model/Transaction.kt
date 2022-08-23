package cash.z.ecc.android.sdk.model

/**
 * Common interface between confirmed transactions on the blockchain and pending transactions being
 * constructed.
 */
interface Transaction {
    val id: Long
    val value: Zatoshi
    val memo: FirstClassByteArray?
}
