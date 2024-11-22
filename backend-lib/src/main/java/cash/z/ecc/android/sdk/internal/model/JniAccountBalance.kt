package cash.z.ecc.android.sdk.internal.model

import androidx.annotation.Keep

/**
 * Serves as cross layer (Kotlin, Rust) communication class.
 *
 * @param account the account ID
 * @param saplingVerifiedBalance The verified account balance in the Sapling pool.
 * @param saplingChangePending The value in the account of Sapling change notes that do
 *        not yet have sufficient confirmations to be spendable.
 * @param saplingValuePending The value in the account of all remaining received Sapling
 *        notes that either do not have sufficient confirmations to be spendable, or for
 *        which witnesses cannot yet be constructed without additional scanning.
 * @param orchardVerifiedBalance The verified account balance in the Orchard pool.
 * @param orchardChangePending The value in the account of Orchard change notes that do
 *        not yet have sufficient confirmations to be spendable.
 * @param orchardValuePending The value in the account of all remaining received Orchard
 *        notes that either do not have sufficient confirmations to be spendable, or for
 *        which witnesses cannot yet be constructed without additional scanning.
 * @param unshieldedBalance The total account balance in the transparent pool,
 *        including unconfirmed funds, that must be shielded before use.
 * @throws IllegalArgumentException if the values are inconsistent.
 */
@Keep
@Suppress("LongParameterList")
class JniAccountBalance(
    val account: ByteArray,
    val saplingVerifiedBalance: Long,
    val saplingChangePending: Long,
    val saplingValuePending: Long,
    val orchardVerifiedBalance: Long,
    val orchardChangePending: Long,
    val orchardValuePending: Long,
    val unshieldedBalance: Long,
) {
    init {
        require(saplingVerifiedBalance >= MIN_INCLUSIVE) {
            "Sapling verified balance $saplingVerifiedBalance must by equal or above $MIN_INCLUSIVE"
        }
        require(saplingChangePending >= MIN_INCLUSIVE) {
            "Sapling change pending $saplingChangePending must by equal or above $MIN_INCLUSIVE"
        }
        require(saplingValuePending >= MIN_INCLUSIVE) {
            "Sapling value pending $saplingValuePending must by equal or above $MIN_INCLUSIVE"
        }
        require(orchardVerifiedBalance >= MIN_INCLUSIVE) {
            "Orchard verified balance $orchardVerifiedBalance must by equal or above $MIN_INCLUSIVE"
        }
        require(orchardChangePending >= MIN_INCLUSIVE) {
            "Orchard change pending $orchardChangePending must by equal or above $MIN_INCLUSIVE"
        }
        require(orchardValuePending >= MIN_INCLUSIVE) {
            "Orchard value pending $orchardValuePending must by equal or above $MIN_INCLUSIVE"
        }
        require(unshieldedBalance >= MIN_INCLUSIVE) {
            "Unshielded balance $unshieldedBalance must by equal or above $MIN_INCLUSIVE"
        }
    }

    companion object {
        const val MIN_INCLUSIVE = 0
    }
}
