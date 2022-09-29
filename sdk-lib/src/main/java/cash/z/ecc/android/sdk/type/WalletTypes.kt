package cash.z.ecc.android.sdk.type

import cash.z.ecc.android.sdk.model.FirstClassByteArray

/**
 * A [ZIP 316] Unified Full Viewing Key, corresponding to a single wallet account.
 *
 * A `UnifiedFullViewingKey` has the authority to view transactions for an account, but
 * does not have spend authority. It can be used to derive a [UnifiedAddress] for the
 * account.
 *
 * @param[encoding] The string encoding of the UFVK.
 */
data class UnifiedFullViewingKey(
    val encoding: String = ""
)

data class UnifiedAddressAccount(
    val accountId: Int = -1,
    override val rawShieldedAddress: String = "",
    override val rawTransparentAddress: String = ""
) : UnifiedAddress

interface UnifiedAddress {
    val rawShieldedAddress: String
    val rawTransparentAddress: String
}
