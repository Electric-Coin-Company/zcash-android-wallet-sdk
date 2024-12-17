package cash.z.ecc.android.sdk.model

/**
 * Wrapper for the create account API based on seed phrase
 *
 * @param accountName A human-readable name for the account. This will be visible to the wallet
 *        user, and the wallet app may obtain it from them.
 * @param keySource A string identifier or other metadata describing the source of the seed. This
 *        should be set internally by the wallet app based on its private enumeration of spending
 *        methods it supports.
 * @param seed the wallet's seed phrase. This is required the first time a new wallet is set up. For
 * subsequent calls, seed is only needed if [InitializerException.SeedRequired] is thrown.
 */
data class AccountCreateSetup(
    val accountName: String,
    val keySource: String?,
    val seed: FirstClassByteArray,
)
