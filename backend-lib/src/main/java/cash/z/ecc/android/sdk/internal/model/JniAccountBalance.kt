package cash.z.ecc.android.sdk.internal.model

import androidx.annotation.Keep

/**
 * Serves as cross layer (Kotlin, Rust) communication class.
 *
 * @param account the account ID
 * @param saplingTotalBalance The total account balance in the Sapling pool,
 *        including unconfirmed funds.
 * @param saplingVerifiedBalance The verified account balance in the Sapling pool.
 * @param orchardTotalBalance The total account balance in the Orchard pool,
 *        including unconfirmed funds.
 * @param orchardVerifiedBalance The verified account balance in the Orchard pool.
 * @param unshieldedBalance The total account balance in the transparent pool,
 *        including unconfirmed funds, that must be shielded before use.
 * @throws IllegalArgumentException if the values are inconsistent.
 */
@Keep
class JniAccountBalance(
    val account: Int,
    val saplingTotalBalance: Long,
    val saplingVerifiedBalance: Long,
    val orchardTotalBalance: Long,
    val orchardVerifiedBalance: Long,
    val unshieldedBalance: Long,
) {
    init {
        require(saplingTotalBalance >= saplingVerifiedBalance) {
            "Total Sapling balance $saplingTotalBalance must not be " +
                "less than verified Sapling balance $saplingVerifiedBalance."
        }
        require(orchardTotalBalance >= orchardVerifiedBalance) {
            "Total Orchard balance $orchardTotalBalance must not be " +
                "less than verified Orchard balance $orchardVerifiedBalance."
        }
    }
}
