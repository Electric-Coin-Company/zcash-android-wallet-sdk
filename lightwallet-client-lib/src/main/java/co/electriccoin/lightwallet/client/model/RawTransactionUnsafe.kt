package co.electriccoin.lightwallet.client.model

import cash.z.wallet.sdk.internal.rpc.Service.RawTransaction

/**
 * RawTransaction contains the complete transaction data, which has come from the Light Wallet server.
 *
 * It is marked as "unsafe" because it is not guaranteed to be valid.
 */
class RawTransactionUnsafe(val height: BlockHeightUnsafe, val data: ByteArray) {
    companion object {
        fun new(rawTransaction: RawTransaction) =
            RawTransactionUnsafe(
                BlockHeightUnsafe(rawTransaction.height),
                rawTransaction.data.toByteArray()
            )
    }
}
