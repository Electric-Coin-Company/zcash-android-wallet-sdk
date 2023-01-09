package cash.z.ecc.android.sdk.model

sealed class Transaction {
    data class Received internal constructor(
        val id: Long,
        val rawId: FirstClassByteArray,
        val minedHeight: BlockHeight,
        val expiryHeight: BlockHeight?,
        val index: Long,
        val raw: FirstClassByteArray?,
        val receivedByAccount: Account,
        val receivedTotal: Zatoshi,
        val receivedNoteCount: Int,
        val memoCount: Int,
        val blockTimeEpochSeconds: Long
    ) : Transaction() {
        override fun toString() = "ReceivedTransaction"
    }

    data class Sent internal constructor(
        val id: Long,
        val rawId: FirstClassByteArray,
        val minedHeight: BlockHeight,
        val expiryHeight: BlockHeight?,
        val index: Long,
        val raw: FirstClassByteArray?,
        val sentFromAccount: Account,
        val sentTotal: Zatoshi,
        val sentNoteCount: Int,
        val memoCount: Int,
        val blockTimeEpochSeconds: Long
    ) : Transaction() {
        override fun toString() = "SentTransaction"
    }
}
