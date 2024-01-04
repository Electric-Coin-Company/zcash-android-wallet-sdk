package cash.z.ecc.fixture

import cash.z.ecc.android.sdk.internal.model.JniBlockMeta
import cash.z.ecc.android.sdk.model.ZcashNetwork

internal class FakeRustBackendFixture {
    private val defaultNetwork = ZcashNetwork.Testnet

    fun new(
        network: ZcashNetwork = defaultNetwork,
        metadata: MutableList<JniBlockMeta> = mutableListOf()
    ) = FakeRustBackend(
        networkId = network.id,
        metadata = metadata
    )
}
