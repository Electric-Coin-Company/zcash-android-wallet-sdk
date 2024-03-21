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
            saplingTree: String,
            orchardTree: String
        ): TreeStateUnsafe {
            val treeState =
                TreeState.newBuilder()
                    .setHeight(height)
                    .setHash(hash)
                    .setTime(time)
                    .setSaplingTree(saplingTree)
                    .setOrchardTree(orchardTree)
                    .build()
            return new(treeState)
        }
    }
}
