package co.electriccoin.lightwallet.client.model

import cash.z.wallet.sdk.internal.rpc.CompactFormats.CompactBlock

/**
 * CompactBlock is a packaging of ONLY the data from a block that's needed to:
 * 1. Detect a payment to your shielded Sapling address
 * 2. Detect a spend of your shielded Sapling notes
 * 3. Update your witnesses to generate new Sapling spend proofs.
 *
 * It is marked as "unsafe" because it is not guaranteed to be valid.
 */
@Suppress("LongParameterList")
class CompactBlockUnsafe(
    val protoVersion: Int, // the version of this wire format, for storage
    val height: Long, // the height of this block
    val hash: ByteArray, // the ID (hash) of this block, same as in block explorers
    val prevHash: ByteArray, // the ID (hash) of this block's predecessor
    val time: Int, // Unix epoch time when the block was mined
    val header: ByteArray, // (hash, prevHash, and time) OR (full header)
    val vtx: List<CompactTxUnsafe> // zero or more compact transactions from this block
) {
    companion object {
        fun new(compactBlock: CompactBlock) = CompactBlockUnsafe(
            protoVersion = compactBlock.protoVersion,
            height = compactBlock.height,
            hash = compactBlock.hash.toByteArray(),
            prevHash = compactBlock.prevHash.toByteArray(),
            time = compactBlock.time,
            header = compactBlock.header.toByteArray(),
            vtx = compactBlock.vtxList.map { CompactTxUnsafe.new(it) }
        )
    }

    fun toByteArray(): ByteArray {
        // Fixme need proper implementation
        error("Intentionally not implemented.")
    }
}
