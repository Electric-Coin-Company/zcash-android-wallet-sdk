package cash.z.ecc.fixture

import cash.z.ecc.android.sdk.internal.model.JniBlockMeta
import cash.z.ecc.android.sdk.model.ZcashNetwork
import java.io.File

internal class FakeRustBackendFixture {

    private val DEFAULT_SAPLING_PARAM_DIR = FilePathFixture.newRootDir()
    private val DEFAULT_NETWORK = ZcashNetwork.Mainnet
    private val DEFAULT_METADATA = mutableListOf<JniBlockMeta>()

    fun new(
        saplingParamDir: File = DEFAULT_SAPLING_PARAM_DIR,
        network: ZcashNetwork = DEFAULT_NETWORK,
        metadata: MutableList<JniBlockMeta> = DEFAULT_METADATA
    ) = FakeRustBackend(
        saplingParamDir = saplingParamDir,
        network = network,
        metadata = metadata
    )
}
