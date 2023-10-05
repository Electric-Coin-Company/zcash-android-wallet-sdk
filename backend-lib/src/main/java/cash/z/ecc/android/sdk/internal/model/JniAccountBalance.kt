package cash.z.ecc.android.sdk.internal.model

import androidx.annotation.Keep

/**
 * Serves as cross layer (Kotlin, Rust) communication class.
 *
 * @param account the account ID
 * @param saplingTotalBalance The total account balance in the Sapling pool, including unconfirmed funds.
 * @param saplingVerifiedBalance The verified account balance in the Sapling pool.
 * @throws IllegalArgumentException if the values are inconsistent.
 */
@Keep
class JniAccountBalance(
    val account: Int,
    val saplingTotalBalance: Long,
    val saplingVerifiedBalance: Long,
) {
    init {
        require(saplingTotalBalance >= saplingVerifiedBalance) {
            "Total Sapling balance $saplingTotalBalance must not be " +
                "less than verified Sapling balance $saplingVerifiedBalance."
        }
    }
}
