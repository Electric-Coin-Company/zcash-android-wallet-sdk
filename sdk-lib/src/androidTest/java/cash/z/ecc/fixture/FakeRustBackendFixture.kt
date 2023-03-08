package cash.z.ecc.fixture

import cash.z.ecc.android.sdk.internal.model.JniBlockMeta
import cash.z.ecc.android.sdk.model.ZcashNetwork
import java.io.File

internal class FakeRustBackendFixture {

    private val DEFAULT_SAPLING_PARAM_DIR = File(DatabasePathFixture.new())
    private val DEFAULT_NETWORK = ZcashNetwork.Testnet

    fun new(
        saplingParamDir: File = DEFAULT_SAPLING_PARAM_DIR,
        network: ZcashNetwork = DEFAULT_NETWORK,
        metadata: MutableList<JniBlockMeta> = mutableListOf()
    ) = FakeRustBackend(
        saplingParamDir = saplingParamDir,
        network = network,
        metadata = metadata
    )
}
