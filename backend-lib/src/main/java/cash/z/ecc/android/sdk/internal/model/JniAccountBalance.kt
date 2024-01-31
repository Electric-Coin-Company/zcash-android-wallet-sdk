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
class JniAccountBalance(
    val account: Int,
    val saplingVerifiedBalance: Long,
    val saplingChangePending: Long,
    val saplingValuePending: Long,
    val orchardVerifiedBalance: Long,
    val orchardChangePending: Long,
    val orchardValuePending: Long,
    val unshieldedBalance: Long,
) {}
