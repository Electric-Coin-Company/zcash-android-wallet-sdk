package cash.z.ecc.android.sdk.model

sealed class Transaction {
    data class Received(
        val id: Long,
        val rawId: FirstClassByteArray,
        val minedHeight: BlockHeight,
        val index: Long,
        val raw: FirstClassByteArray,
        val receivedTotal: Zatoshi,
        val receivedNoteCount: Int,
        val memoCount: Int,
        val time: Long
    ) : Transaction() {
        override fun toString() = "ReceivedTransaction"
    }

    data class Sent(
        val id: Long,
        val rawId: FirstClassByteArray,
        val minedHeight: BlockHeight,
        val expiryHeight: BlockHeight,
        val index: Long,
        val raw: FirstClassByteArray,
        val sentTotal: Zatoshi,
        val sentNoteCount: Int,
        val memoCount: Int,
        val time: Long
    ) : Transaction() {
        override fun toString() = "SentTransaction"
    }
}
