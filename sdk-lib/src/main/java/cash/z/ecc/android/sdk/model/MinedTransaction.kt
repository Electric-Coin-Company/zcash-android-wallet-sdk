package cash.z.ecc.android.sdk.model

/**
 * Parent type for transactions that have been mined. This is useful for putting all transactions in
 * one list for things like history. A mined tx should have all properties, except possibly a memo.
 */
interface MinedTransaction : Transaction {
    val minedHeight: BlockHeight
    val noteId: Long
    val blockTimeInSeconds: Long
    val transactionIndex: Int
    val rawTransactionId: FirstClassByteArray
}
