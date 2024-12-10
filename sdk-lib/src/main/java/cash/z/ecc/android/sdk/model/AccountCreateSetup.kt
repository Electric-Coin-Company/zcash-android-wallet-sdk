package cash.z.ecc.android.sdk.model

/**
 * Wrapper for the create account API based on seed phrase
 *
 * @param accountName A human-readable name for the account
 * @param keySource A string identifier or other metadata describing the source of the seed
 * @param seed the wallet's seed phrase. This is required the first time a new wallet is set up. For
 * subsequent calls, seed is only needed if [InitializerException.SeedRequired] is thrown.
 */
data class AccountCreateSetup(
    val accountName: String,
    val keySource: String?,
    val seed: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AccountCreateSetup

        if (accountName != other.accountName) return false
        if (keySource != other.keySource) return false
        if (!seed.contentEquals(other.seed)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = accountName.hashCode()
        result = 31 * result + (keySource?.hashCode() ?: 0)
        result = 31 * result + seed.contentHashCode()
        return result
    }
}
