package cash.z.ecc.android.sdk.internal.model

import cash.z.ecc.android.sdk.internal.ext.toHexReversed
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.FirstClassByteArray
import cash.z.ecc.android.sdk.model.Zatoshi

internal data class DbTransactionOverview internal constructor(
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
    val blockTimeEpochSeconds: Long?
) {
    override fun toString() = "DbTransactionOverview"

    fun txIdString(): String {
        return rawId.byteArray.toHexReversed()
    }
}
