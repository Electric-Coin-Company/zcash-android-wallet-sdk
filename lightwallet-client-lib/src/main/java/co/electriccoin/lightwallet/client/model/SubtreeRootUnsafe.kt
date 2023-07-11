package co.electriccoin.lightwallet.client.model

import cash.z.wallet.sdk.internal.rpc.Service.SubtreeRoot

/**
 * SubtreeRoot contains information about roots of subtrees of the Sapling and Orchard note commitment trees, which
 * has come from the Light Wallet server.
 *
 * It is marked as "unsafe" because it is not guaranteed to be valid.
 *
 * @param rootHash The 32-byte Merkle root of the subtree
 * @param completingBlockHash The hash of the block that completed this subtree.
 * @param completingBlockHeight The height of the block that completed this subtree in the main chain.
 */
class SubtreeRootUnsafe(
    val rootHash: ByteArray,
    val completingBlockHash: ByteArray,
    val completingBlockHeight: BlockHeightUnsafe
) {
    companion object {
        fun new(subtreeRoot: SubtreeRoot) = SubtreeRootUnsafe(
            rootHash = subtreeRoot.rootHash.toByteArray(),
            completingBlockHash = subtreeRoot.completingBlockHash.toByteArray(),
            completingBlockHeight = BlockHeightUnsafe(subtreeRoot.completingBlockHeight),
        )
    }
}
