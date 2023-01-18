package cash.z.ecc.fixture

import cash.z.ecc.android.sdk.internal.model.JniBlockMeta
import cash.z.ecc.android.sdk.jni.RustBackend
import cash.z.ecc.android.sdk.jni.RustBackendWelding
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.ZcashNetwork

internal class FakeRustBackend(rustBackend: RustBackend) : RustBackendWelding by rustBackend {

    val metadata = mutableListOf<JniBlockMeta>()

    override val network: ZcashNetwork
        get() = ZcashNetwork.Mainnet

    override suspend fun writeBlockMetadata(blockMetadata: Array<JniBlockMeta>): Boolean =
        metadata.addAll(blockMetadata)

    override suspend fun rewindToHeight(height: BlockHeight): Boolean {
        metadata.removeAll { it.height > height.value }
        return true
    }

    override suspend fun getLatestHeight(): BlockHeight = BlockHeight(metadata.maxOf { it.height })
}
