package cash.z.ecc.android.sdk.internal.model

import co.electriccoin.lightwallet.client.model.TreeStateUnsafe

class TreeState(
    val encoded: ByteArray
) {
    companion object {
        fun new(unsafe: TreeStateUnsafe): TreeState {
            // Potential validation comes here
            return TreeState(
                encoded = unsafe.encoded
            )
        }

        fun fromParts(
            height: Long,
            hash: String,
            time: Int,
            saplingTree: String,
            orchardTree: String
        ): TreeState {
            val unsafeTreeState = TreeStateUnsafe.fromParts(height, hash, time, saplingTree, orchardTree)
            return TreeState.new(unsafeTreeState)
        }
    }
}
