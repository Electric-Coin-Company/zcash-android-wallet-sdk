package cash.z.ecc.android.sdk.fixture

import cash.z.ecc.android.sdk.internal.model.JniBlockMeta
import cash.z.ecc.android.sdk.model.ZcashNetwork

internal object FakeRustBackendFixture {

    private val DEFAULT_NETWORK = ZcashNetwork.Testnet
    fun new(
        network: ZcashNetwork = DEFAULT_NETWORK,
        metadata: MutableList<JniBlockMeta> = mutableListOf()
    ) = FakeRustBackend(
        networkId = network.id,
        metadata = metadata
    )
}
