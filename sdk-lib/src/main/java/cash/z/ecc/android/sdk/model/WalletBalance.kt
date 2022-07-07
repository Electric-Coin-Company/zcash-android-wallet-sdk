package cash.z.ecc.android.sdk.model

/**
 * Data structure to hold the total and available balance of the wallet. This is what is
 * received on the balance channel.
 *
 * @param total the total balance, ignoring funds that cannot be used.
 * @param available the amount of funds that are available for use. Typical reasons that funds
 * may be unavailable include fairly new transactions that do not have enough confirmations or
 * notes that are tied up because we are awaiting change from a transaction. When a note has
 * been spent, its change cannot be used until there are enough confirmations.
 */
data class WalletBalance(
    val total: Zatoshi,
    val available: Zatoshi
) {
    init {
        require(total.value >= available.value) { "Wallet total balance must be >= available balance" }
    }

    val pending = total - available

    operator fun plus(other: WalletBalance): WalletBalance =
        WalletBalance(
            total + other.total,
            available + other.available
        )
}
