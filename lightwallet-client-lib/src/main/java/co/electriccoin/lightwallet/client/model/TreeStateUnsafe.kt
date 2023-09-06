package co.electriccoin.lightwallet.client.model

import cash.z.wallet.sdk.internal.rpc.Service.TreeState

class TreeStateUnsafe(
    val encoded: ByteArray
) {
    companion object {
        fun new(treeState: TreeState): TreeStateUnsafe {
            return TreeStateUnsafe(treeState.toByteArray())
        }

        fun fromParts(
            height: Long,
            hash: String,
            time: Int,
            tree: String
        ): TreeStateUnsafe {
            val treeState = TreeState.newBuilder()
                .setHeight(height)
                .setHash(hash)
                .setTime(time)
                .setSaplingTree(tree)
                .build()
            return TreeStateUnsafe.new(treeState)
        }
    }
}