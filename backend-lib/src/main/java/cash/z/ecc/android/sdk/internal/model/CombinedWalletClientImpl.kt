package cash.z.ecc.android.sdk.internal.model

import co.electriccoin.lightwallet.client.CombinedWalletClient
import co.electriccoin.lightwallet.client.LightWalletClient
import co.electriccoin.lightwallet.client.PartialTorWalletClient
import co.electriccoin.lightwallet.client.ServiceMode
import co.electriccoin.lightwallet.client.model.BlockHeightUnsafe
import co.electriccoin.lightwallet.client.model.LightWalletEndpoint
import co.electriccoin.lightwallet.client.model.ShieldedProtocolEnum
import co.electriccoin.lightwallet.client.util.use
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Suppress("TooManyFunctions")
class CombinedWalletClientImpl private constructor(
    private val lightWalletClient: LightWalletClient,
    private val torClient: TorClient,
    private val endpoint: LightWalletEndpoint,
) : CombinedWalletClient {
    private val cache = mutableMapOf<ServiceMode, PartialTorWalletClient>()

    private val factorySemaphore = Mutex()

    override suspend fun dispose() {
        lightWalletClient.dispose()
        factorySemaphore.withLock {
            torClient.dispose()
            cache.forEach { (_, client) -> client.dispose() }
        }
    }

    override suspend fun fetchTransaction(
        txId: ByteArray,
        serviceMode: ServiceMode
    ) = execute(serviceMode) { fetchTransaction(txId) }

    override suspend fun getServerInfo(
        serviceMode: ServiceMode
    ) = execute(serviceMode) { getServerInfo() }

    override suspend fun getLatestBlockHeight(
        serviceMode: ServiceMode
    ) = execute(serviceMode) { getLatestBlockHeight() }

    override suspend fun submitTransaction(
        tx: ByteArray,
        serviceMode: ServiceMode
    ) = execute(serviceMode) { submitTransaction(tx) }

    override suspend fun getTreeState(
        height: BlockHeightUnsafe,
        serviceMode: ServiceMode
    ) = execute(serviceMode) { getTreeState(height) }

    override suspend fun fetchUtxos(
        tAddresses: List<String>,
        startHeight: BlockHeightUnsafe,
        serviceMode: ServiceMode
    ) = lightWalletClient.fetchUtxos(tAddresses, startHeight)

    override fun getBlockRange(
        heightRange: ClosedRange<BlockHeightUnsafe>,
        serviceMode: ServiceMode
    ) = lightWalletClient.getBlockRange(heightRange)

    override fun getTAddressTransactions(
        tAddress: String,
        blockHeightRange: ClosedRange<BlockHeightUnsafe>,
        serviceMode: ServiceMode
    ) = lightWalletClient.getTAddressTransactions(tAddress, blockHeightRange)

    override fun getSubtreeRoots(
        startIndex: UInt,
        shieldedProtocol: ShieldedProtocolEnum,
        maxEntries: UInt,
        serviceMode: ServiceMode
    ) = lightWalletClient.getSubtreeRoots(startIndex, shieldedProtocol, maxEntries)

    override fun reconnect() = lightWalletClient.reconnect()

    private suspend inline fun <T> execute(
        serviceMode: ServiceMode,
        block: PartialTorWalletClient.() -> T
    ): T =
        if (serviceMode == ServiceMode.UniqueTor) {
            create().use { block(it) }
        } else {
            block(getOrCreate(serviceMode))
        }

    private suspend fun getOrCreate(serviceMode: ServiceMode) =
        factorySemaphore.withLock {
            withContext(Dispatchers.Default) {
                cache.getOrPut(serviceMode) { create() }
            }
        }

    private suspend fun create() = torClient.createWalletClient("https://${endpoint.host}:${endpoint.port}")

    companion object Factory {
        suspend fun new(
            endpoint: LightWalletEndpoint,
            lightWalletClient: LightWalletClient,
            torClient: TorClient
        ) = CombinedWalletClientImpl(
            endpoint = endpoint,
            lightWalletClient = lightWalletClient,
            torClient = torClient
        )
    }
}
