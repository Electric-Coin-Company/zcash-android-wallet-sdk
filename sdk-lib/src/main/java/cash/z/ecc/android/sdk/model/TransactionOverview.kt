package cash.z.ecc.android.sdk.model

import cash.z.ecc.android.sdk.internal.ext.toHexReversed
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
    val rawId: FirstClassByteArray,
    val minedHeight: BlockHeight?,
    val expiryHeight: BlockHeight?,
    val index: Long?,
    val raw: FirstClassByteArray?,
    val isSentTransaction: Boolean,
    val netValue: Zatoshi,
    val feePaid: Zatoshi?,
    val isChange: Boolean,
    val receivedNoteCount: Int,
    val sentNoteCount: Int,
    val memoCount: Int,
    val blockTimeEpochSeconds: Long?,
    val transactionState: TransactionState,
    val isShielding: Boolean
) {
    override fun toString() = "TransactionOverview"

    /**
     * @return Transaction ID in String obtained from `rawId`
     */
    fun txIdString() = rawId.byteArray.toHexReversed()

    companion object {
        internal fun new(
            dbTransactionOverview: DbTransactionOverview,
            latestBlockHeight: BlockHeight?
        ): TransactionOverview {
            return TransactionOverview(
                rawId = dbTransactionOverview.rawId,
                minedHeight = dbTransactionOverview.minedHeight,
                expiryHeight = dbTransactionOverview.expiryHeight,
                index = dbTransactionOverview.index,
                raw = dbTransactionOverview.raw,
                isSentTransaction = dbTransactionOverview.isSentTransaction,
                netValue = dbTransactionOverview.netValue,
                feePaid = dbTransactionOverview.feePaid,
                isChange = dbTransactionOverview.isChange,
                receivedNoteCount = dbTransactionOverview.receivedNoteCount,
                sentNoteCount = dbTransactionOverview.sentNoteCount,
                memoCount = dbTransactionOverview.memoCount,
                blockTimeEpochSeconds = dbTransactionOverview.blockTimeEpochSeconds,
                transactionState =
                TransactionState.new(
                    latestBlockHeight,
                    dbTransactionOverview.minedHeight,
                    dbTransactionOverview.expiryHeight
                ),
                isShielding = dbTransactionOverview.isShielding
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
            return latestBlockHeight?.let { chainTip ->
                minedHeight?.let { minedHeight ->
                    // A transaction mined in the latest block has 1 confirmation.
                    if ((chainTip + 1 - minedHeight) >= MIN_CONFIRMATIONS) {
                        Confirmed
                    } else {
                        Pending
                    }
                } ?: expiryHeight?.let { expiryHeight ->
                    // Expiry height is the last height at which a transaction can be mined.
                    // If the chain tip is greater than or equal to the expiry height, the
                    // transaction can never be mined. A value of 0 disables expiry.
                    if (expiryHeight.value == 0L || expiryHeight > chainTip) {
                        Pending
                    } else {
                        Expired
                    }
                }
                // Base case: either we don't know the latest block height (unlikely if we
                // know about transactions), or the transaction is both unmined and has an
                // unknown expiry height (because we haven't seen the full transaction).
                // Treat these as Pending because the status will change as we sync.
            } ?: Pending
        }
    }
}
