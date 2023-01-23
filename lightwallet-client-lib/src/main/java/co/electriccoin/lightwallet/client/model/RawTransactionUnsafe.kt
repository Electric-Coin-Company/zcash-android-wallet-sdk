package co.electriccoin.lightwallet.client.model

import cash.z.wallet.sdk.internal.rpc.Service.RawTransaction

class RawTransactionUnsafe(val height: BlockHeightUnsafe, val data: ByteArray) {
    companion object {
        fun new(rawTransaction: RawTransaction) = RawTransactionUnsafe(
            BlockHeightUnsafe(rawTransaction.height),
            rawTransaction.data.toByteArray()
        )
    }
}
