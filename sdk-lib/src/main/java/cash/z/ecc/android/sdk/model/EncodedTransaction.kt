package cash.z.ecc.android.sdk.model

data class EncodedTransaction(
    val txId: FirstClassByteArray,
    override val raw: FirstClassByteArray,
    val expiryHeight: BlockHeight?
) : SignedTransaction
