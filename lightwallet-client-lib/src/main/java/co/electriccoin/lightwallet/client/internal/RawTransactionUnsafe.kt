package co.electriccoin.lightwallet.client.internal

import cash.z.wallet.sdk.internal.rpc.Service.RawTransaction
import co.electriccoin.lightwallet.client.model.BlockHeightUnsafe

class RawTransactionUnsafe(val height: BlockHeightUnsafe, val data: ByteArray) {
    companion object {
        fun new(rawTransaction: RawTransaction) = RawTransactionUnsafe(
            BlockHeightUnsafe(rawTransaction.height),
            rawTransaction.data.toByteArray()
        )
    }
}
