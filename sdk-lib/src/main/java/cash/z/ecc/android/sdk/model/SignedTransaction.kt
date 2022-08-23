package cash.z.ecc.android.sdk.model

/**
 * Interface for anything that's able to provide signed transaction bytes.
 */
interface SignedTransaction {
    val raw: FirstClassByteArray
}
