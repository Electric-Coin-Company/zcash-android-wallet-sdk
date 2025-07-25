package cash.z.ecc.android.sdk.model

import co.electriccoin.lightwallet.client.ServiceMode

/**
 * @param isTorEnabled indicates whether tor should be used for network connection. True if enabled, false if disabled,
 * null if disabled and not explicitly set
 */
data class SdkFlags(
    val isTorEnabled: Boolean
) {
    infix fun ifTor(other: ServiceMode): ServiceMode = if (isTorEnabled) other else ServiceMode.Direct
}
