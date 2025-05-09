package cash.z.ecc.android.sdk.internal.model

import cash.z.ecc.android.sdk.internal.ext.toHexReversed
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.FirstClassByteArray

internal data class EncodedTransaction(
    val txId: FirstClassByteArray,
    val raw: FirstClassByteArray,
    val expiryHeight: BlockHeight?
) {
    override fun toString() = "EncodedTransaction"

    fun txIdString(): String = txId.byteArray.toHexReversed()
}
