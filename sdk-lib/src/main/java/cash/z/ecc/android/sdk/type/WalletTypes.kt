package cash.z.ecc.android.sdk.type

/**
 * Data structure to hold the total and available balance of the wallet. This is what is
 * received on the balance channel.
 *
 * @param totalZatoshi the total balance, ignoring funds that cannot be used.
 * @param availableZatoshi the amount of funds that are available for use. Typical reasons that funds
 * may be unavailable include fairly new transactions that do not have enough confirmations or
 * notes that are tied up because we are awaiting change from a transaction. When a note has
 * been spent, its change cannot be used until there are enough confirmations.
 */
data class WalletBalance(
    val totalZatoshi: Long = -1,
    val availableZatoshi: Long = -1
) {
    val pendingZatoshi = totalZatoshi.coerceAtLeast(0) - availableZatoshi.coerceAtLeast(0)
    operator fun plus(other: WalletBalance): WalletBalance {
        return if (
            totalZatoshi == -1L && other.totalZatoshi == -1L &&
            availableZatoshi == -1L && other.availableZatoshi == -1L
        ) {
            // if everything is uninitialized, then return the same
            WalletBalance(-1L, -1L)
        } else {
            // otherwise, ignore any uninitialized values
            WalletBalance(
                totalZatoshi = totalZatoshi.coerceAtLeast(0) + other.totalZatoshi.coerceAtLeast(0),
                availableZatoshi = availableZatoshi.coerceAtLeast(0) + other.availableZatoshi.coerceAtLeast(
                    0
                )
            )
        }
    }
}

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
    val tree: String = ""
)

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

interface ZcashNetwork {
    val id: Int
    val networkName: String
    val saplingActivationHeight: Int
    val defaultHost: String
    val defaultPort: Int

    // Required to enable extension functions
    companion object
}

enum class NetworkType(
    override val id: Int,
    override val networkName: String,
    override val saplingActivationHeight: Int,
    override val defaultHost: String,
    override val defaultPort: Int
) : ZcashNetwork {
    Testnet(0, "testnet", 280_000, "testnet.lightwalletd.com", 9067),
    Mainnet(1, "mainnet", 419_200, "mainnet.lightwalletd.com", 9067);

    companion object {
        fun from(id: Int) = values().first { it.id == id }
    }
}
