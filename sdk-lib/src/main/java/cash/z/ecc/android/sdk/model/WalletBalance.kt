package cash.z.ecc.android.sdk.model

/**
 * Data structure to hold the balance of the wallet. This is what is received on the balance channel.
 *
 * @param available The amount of funds that are available for use. Typical reasons that funds
 * may be unavailable include fairly new transactions that do not have enough confirmations or
 * notes that are tied up because we are awaiting change from a transaction. When a note has
 * been spent, its change cannot be used until there are enough confirmations.
 * @param changePending The value in the account of change notes that do not yet have sufficient confirmations to be
 * spendable.
 * @param valuePending The value in the account of all remaining received notes that either do not have sufficient
 * confirmations to be spendable, or for which witnesses cannot yet be constructed without additional scanning.
 */
data class WalletBalance(
    val available: Zatoshi,
    val changePending: Zatoshi,
    val valuePending: Zatoshi
) {
    /**
     * The current total balance is calculated as a sum of [available], [changePending],
     * and [valuePending].
     */
    val total = available + changePending + valuePending

    /**
     * The current pending balance is calculated as the difference between [total] and [available] balances.
     */
    val pending = total - available
}
