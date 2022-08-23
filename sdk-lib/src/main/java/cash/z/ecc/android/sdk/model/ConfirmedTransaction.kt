package cash.z.ecc.android.sdk.model

/**
 * A mined, shielded transaction. Since this is a [MinedTransaction], it represents data
 * on the blockchain.
 */
data class ConfirmedTransaction(
    override val id: Long,
    override val value: Zatoshi,
    override val memo: FirstClassByteArray?,
    override val noteId: Long = 0L,
    override val blockTimeInSeconds: Long,
    override val minedHeight: BlockHeight,
    override val transactionIndex: Int,
    override val rawTransactionId: FirstClassByteArray,

    // properties that differ from received transactions
    val toAddress: String? = null,
    val expiryHeight: BlockHeight?,
    override val raw: FirstClassByteArray
) : MinedTransaction, SignedTransaction
