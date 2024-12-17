package cash.z.ecc.android.sdk.model

import cash.z.ecc.android.sdk.internal.model.JniAccountUsk

/**
 * Account related model class providing a [ZIP 316](https://zips.z.cash/zip-0316) Unified Spending Key.
 *
 * This is the spend authority for an account under the wallet's seed.
 *
 * An instance of this class contains all of the per-pool spending keys that could be
 * derived at the time of its creation. As such, it is not suitable for long-term storage,
 * export/import, or backup purposes.
 */
class AccountUsk private constructor(
    /**
     * The account UUID used to derive this key.
     */
    val accountUuid: AccountUuid,
    /**
     * The [ZIP 316](https://zips.z.cash/zip-0316) Unified Spending Key.
     */
    private val usk: UnifiedSpendingKey
) {
    override fun toString() = "AccountUsk(account=$accountUuid, usk=$usk)"

    companion object {
        suspend fun new(jniAccountUsk: JniAccountUsk): AccountUsk =
            AccountUsk(
                accountUuid = AccountUuid.new(jniAccountUsk.accountUuid),
                usk = UnifiedSpendingKey.new(jniAccountUsk.bytes)
            )
    }
}
