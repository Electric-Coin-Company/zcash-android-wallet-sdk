package cash.z.ecc.android.sdk.model

/**
 * Optional wrapper for the Synchronizer setup
 *
 * @param accountName Optional account name that will be created as part of the new wallet setup process based
 * on the given seed
 * @param keySource Optional key source that will be persisted alongside the account created in the new
 * wallet setup process based on the given seed
 * @param seed the wallet's seed phrase. This is required the first time a new wallet is set up. For
 * subsequent calls, seed is only needed if [InitializerException.SeedRequired] is thrown.
 */
data class AccountSetup(
    val accountName: String,
    val keySource: String?,
    val seed: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AccountSetup

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
