package cash.z.ecc.android.sdk.model

/**
 * High level transaction information, suitable for mapping to a display of transaction history.
 *
 * Note that both sent and received transactions will have a positive net value.  Consumers of this class must
 */
data class TransactionOverview internal constructor(
    internal val id: Long,
    val rawId: FirstClassByteArray,
    val minedHeight: BlockHeight,
    val expiryHeight: BlockHeight,
    val index: Long,
    val raw: FirstClassByteArray,
    val isSentTransaction: Boolean,
    val netValue: Zatoshi,
    val feePaid: Zatoshi,
    val isChange: Boolean,
    val isWalletInternal: Boolean,
    val receivedNoteCount: Int,
    val sentNoteCount: Int,
    val memoCount: Int,
    val blockTimeEpochSeconds: Long
) {
    override fun toString() = "TransactionOverview"
}
