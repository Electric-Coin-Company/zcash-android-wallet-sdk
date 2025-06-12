package cash.z.ecc.android.sdk.internal.model

import co.electriccoin.lightwallet.client.CombinedWalletClient
import co.electriccoin.lightwallet.client.LightWalletClient
import co.electriccoin.lightwallet.client.PartialTorWalletClient
import co.electriccoin.lightwallet.client.PartialWalletClient
import co.electriccoin.lightwallet.client.ServiceMode
import co.electriccoin.lightwallet.client.model.BlockHeightUnsafe
import co.electriccoin.lightwallet.client.model.CompactBlockUnsafe
import co.electriccoin.lightwallet.client.model.GetAddressUtxosReplyUnsafe
import co.electriccoin.lightwallet.client.model.LightWalletEndpoint
import co.electriccoin.lightwallet.client.model.RawTransactionUnsafe
import co.electriccoin.lightwallet.client.model.Response
import co.electriccoin.lightwallet.client.model.ShieldedProtocolEnum
import co.electriccoin.lightwallet.client.model.SubtreeRootUnsafe
import co.electriccoin.lightwallet.client.util.use
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
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

    private val semaphore = Mutex()

    override suspend fun dispose() {
        semaphore.withLock {
            lightWalletClient.dispose()
            torClient.dispose()
            cache.forEach { (_, client) -> client.dispose() }
            cache.clear()
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
    ): Flow<Response<GetAddressUtxosReplyUnsafe>> {
        require(serviceMode == ServiceMode.Direct)
        return execute(serviceMode) {
            require(this is LightWalletClient)
            fetchUtxos(tAddresses, startHeight)
        }
    }

    override suspend fun getBlockRange(
        heightRange: ClosedRange<BlockHeightUnsafe>,
        serviceMode: ServiceMode
    ): Flow<Response<CompactBlockUnsafe>> {
        require(serviceMode == ServiceMode.Direct)
        return execute(serviceMode) {
            require(this is LightWalletClient)
            getBlockRange(heightRange)
        }
    }

    override suspend fun getTAddressTransactions(
        tAddress: String,
        blockHeightRange: ClosedRange<BlockHeightUnsafe>,
        serviceMode: ServiceMode
    ): Flow<Response<RawTransactionUnsafe>> {
        require(serviceMode == ServiceMode.Direct)
        return execute(serviceMode) {
            require(this is LightWalletClient)
            getTAddressTransactions(tAddress, blockHeightRange)
        }
    }

    override suspend fun getSubtreeRoots(
        startIndex: UInt,
        shieldedProtocol: ShieldedProtocolEnum,
        maxEntries: UInt,
        serviceMode: ServiceMode
    ): Flow<Response<SubtreeRootUnsafe>> {
        require(serviceMode == ServiceMode.Direct)
        return execute(serviceMode) {
            require(this is LightWalletClient)
            getSubtreeRoots(startIndex, shieldedProtocol, maxEntries)
        }
    }

    override fun reconnect() = lightWalletClient.reconnect()

    @Suppress("TooGenericExceptionCaught")
    private suspend inline fun <T> execute(
        serviceMode: ServiceMode,
        block: PartialWalletClient.() -> T
    ): T =
        semaphore.withLock {
            when (serviceMode) {
                ServiceMode.Direct -> block(lightWalletClient)
                ServiceMode.UniqueTor -> create().use { block(it) }
                ServiceMode.DefaultTor,
                is ServiceMode.Group ->
                    try {
                        block(getOrCreate(serviceMode))
                    } catch (e: Exception) {
                        remove(serviceMode)
                        throw e
                    }
            }
        }

    private suspend fun remove(serviceMode: ServiceMode) =
        withContext(Dispatchers.Default) { cache.remove(serviceMode) }

    private suspend fun getOrCreate(serviceMode: ServiceMode) =
        withContext(Dispatchers.Default) { cache.getOrPut(serviceMode) { create() } }

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
