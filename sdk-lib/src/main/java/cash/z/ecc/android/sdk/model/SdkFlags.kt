package cash.z.ecc.android.sdk.model

/**
 * @param isTorEnabled indicates whether tor should be used for network connection. True if enabled, false if disabled,
 * null if disabled and not explicitly set
 */
data class SdkFlags(
    val isTorEnabled: Boolean
)
