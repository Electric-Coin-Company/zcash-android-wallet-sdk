package co.electriccoin.lightwallet.client.model

import cash.z.wallet.sdk.internal.rpc.Service

/**
 * It is marked as "unsafe" because it is not guaranteed to be valid.
 */
class GetAddressUtxosReplyUnsafe(
    val address: String,
    val txid: ByteArray,
    val index: Int,
    val script: ByteArray,
    val valueZat: Long,
    val height: Long
) {
    companion object {

        fun new(getAddressUtxosReply: Service.GetAddressUtxosReply): GetAddressUtxosReplyUnsafe {
            return GetAddressUtxosReplyUnsafe(
                address = getAddressUtxosReply.address,
                txid = getAddressUtxosReply.txid.toByteArray(),
                index = getAddressUtxosReply.index,
                script = getAddressUtxosReply.script.toByteArray(),
                valueZat = getAddressUtxosReply.valueZat,
                height = getAddressUtxosReply.height,
            )
        }
    }
}
