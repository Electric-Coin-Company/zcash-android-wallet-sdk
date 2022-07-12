package cash.z.ecc.android.sdk.type

/**
 * A grouping of keys that correspond to a single wallet account but do not have spend authority.
 *
 * @param extfvk the extended full viewing key which provides the ability to see inbound and
 * outbound shielded transactions. It can also be used to derive a z-addr.
 * @param extpub the extended public key which provides the ability to see transparent
 * transactions. It can also be used to derive a t-addr.
 */
data class UnifiedViewingKey(
    val extfvk: String = "",
    val extpub: String = ""
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
