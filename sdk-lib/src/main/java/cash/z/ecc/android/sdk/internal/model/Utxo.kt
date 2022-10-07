package cash.z.ecc.android.sdk.internal.model

import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.FirstClassByteArray
import cash.z.ecc.android.sdk.model.Zatoshi

internal data class Utxo(
    val id: Long? = 0L,
    val address: String,
    val txid: FirstClassByteArray,
    val transactionIndex: Int,
    val script: FirstClassByteArray,

    val value: Zatoshi,

    val height: BlockHeight?,
    val spent: Int? = 0
)
