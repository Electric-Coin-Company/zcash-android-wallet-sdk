package co.electriccoin.lightwallet.client.model

import cash.z.wallet.sdk.internal.rpc.CompactFormats.CompactSaplingSpend

/**
 * CompactSaplingSpend is a Sapling Spend Description as described in 7.3 of the Zcash protocol specification.
 *
 * It is marked as "unsafe" because it is not guaranteed to be valid.
 */
class CompactSaplingSpendUnsafe(
    val nf: ByteArray // nullifier (see the Zcash protocol specification)
) {
    companion object {
        fun new(compactSaplingSpend: CompactSaplingSpend) = CompactSaplingSpendUnsafe(
            nf = compactSaplingSpend.nf.toByteArray()
        )
    }
}
