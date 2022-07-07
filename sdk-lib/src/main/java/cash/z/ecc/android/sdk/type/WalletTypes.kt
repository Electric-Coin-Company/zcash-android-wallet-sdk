package cash.z.ecc.android.sdk.type

/**
 * Model object for holding a wallet birthday.
 *
 * @param height the height at the time the wallet was born.
 * @param hash the hash of the block at the height.
 * @param time the block time at the height. Represented as seconds since the Unix epoch.
 * @param tree the sapling tree corresponding to the height.
 */
data class WalletBirthday(
    val height: Int = -1,
    val hash: String = "",
    val time: Long = -1,
    // Note: this field does NOT match the name of the JSON, so will break with field-based JSON parsing
    val tree: String = ""
) {
    companion object
}

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

enum class ZcashNetwork(
    val id: Int,
    val networkName: String,
    val saplingActivationHeight: Int,
    val defaultHost: String,
    val defaultPort: Int
) {
    Testnet(0, "testnet", 280_000, "testnet.lightwalletd.com", 9067),
    Mainnet(1, "mainnet", 419_200, "mainnet.lightwalletd.com", 9067);

    companion object {
        fun from(id: Int) = values().first { it.id == id }
    }
}
