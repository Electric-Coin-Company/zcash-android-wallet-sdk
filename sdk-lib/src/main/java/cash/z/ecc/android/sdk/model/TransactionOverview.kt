package cash.z.ecc.android.sdk.model

import cash.z.ecc.android.sdk.internal.model.DbTransactionOverview

/**
 * High level transaction information, suitable for mapping to a display of transaction history.
 *
 * Note that both sent and received transactions will have a positive net value.  Consumers of this class must check
 * [isSentTransaction] if displaying negative values is desired.
 *
 * Pending transactions are identified by a null [minedHeight].  Pending transactions are considered expired if the
 * last synced block exceeds the [expiryHeight].
 */
data class TransactionOverview internal constructor(
    val id: Long,
    val rawId: FirstClassByteArray,
    val minedHeight: BlockHeight?,
    val expiryHeight: BlockHeight?,
    val index: Long,
    val raw: FirstClassByteArray?,
    val isSentTransaction: Boolean,
    val netValue: Zatoshi,
    val feePaid: Zatoshi,
    val isChange: Boolean,
    val receivedNoteCount: Int,
    val sentNoteCount: Int,
    val memoCount: Int,
    val blockTimeEpochSeconds: Long,
    val transactionState: TransactionState
) {
    override fun toString() = "TransactionOverview"

    companion object {
        internal fun new(
            dbTransactionOverview: DbTransactionOverview,
            latestBlockHeight: BlockHeight?
        ): TransactionOverview {
            return TransactionOverview(
                dbTransactionOverview.id,
                dbTransactionOverview.rawId,
                dbTransactionOverview.minedHeight,
                dbTransactionOverview.expiryHeight,
                dbTransactionOverview.index,
                dbTransactionOverview.raw,
                dbTransactionOverview.isSentTransaction,
                dbTransactionOverview.netValue,
                dbTransactionOverview.feePaid,
                dbTransactionOverview.isChange,
                dbTransactionOverview.receivedNoteCount,
                dbTransactionOverview.sentNoteCount,
                dbTransactionOverview.memoCount,
                dbTransactionOverview.blockTimeEpochSeconds,
                TransactionState.new(
                    latestBlockHeight,
                    dbTransactionOverview.minedHeight,
                    dbTransactionOverview.expiryHeight
                )
            )
        }
    }
}

enum class TransactionState {
    Confirmed,
    Pending,
    Expired;

    companion object {

        private const val MIN_CONFIRMATIONS = 10
        internal fun new(
            latestBlockHeight: BlockHeight?,
            minedHeight: BlockHeight?,
            expiryHeight: BlockHeight?
        ): TransactionState {
            return if (latestBlockHeight == null) {
                Pending
            } else if (minedHeight != null && (latestBlockHeight.value - minedHeight.value) >= MIN_CONFIRMATIONS) {
                Confirmed
            } else if (minedHeight != null && (latestBlockHeight.value - minedHeight.value) < MIN_CONFIRMATIONS) {
                Pending
            } else if (minedHeight == null && ((expiryHeight?.value ?: Long.MAX_VALUE) < latestBlockHeight.value)) {
                Pending
            } else {
                Expired
            }
        }
    }
}
