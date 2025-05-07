package cash.z.ecc.android.sdk.internal.model

import co.electriccoin.lightwallet.client.PartialWalletClient
import co.electriccoin.lightwallet.client.LightWalletClient
import co.electriccoin.lightwallet.client.BaseTorWalletClient
import co.electriccoin.lightwallet.client.WalletClient
import co.electriccoin.lightwallet.client.model.BlockHeightUnsafe
import co.electriccoin.lightwallet.client.model.Response
import co.electriccoin.lightwallet.client.model.ShieldedProtocolEnum
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CombinedWalletClient(
    private val lightWalletClient: LightWalletClient,
    private val torWalletClient: BaseTorWalletClient,
) : WalletClient {

    private val semaphore = Mutex()

    override suspend fun fetchTransaction(txId: ByteArray) = executeOverTorOrDefault { it.fetchTransaction(txId) }

    override suspend fun getServerInfo() = executeOverTorOrDefault { it.getServerInfo() }

    override suspend fun getLatestBlockHeight() = executeOverTorOrDefault { it.getLatestBlockHeight() }

    override suspend fun submitTransaction(tx: ByteArray) = executeOverTorOrDefault { it.submitTransaction(tx) }

    override suspend fun getTreeState(height: BlockHeightUnsafe) = executeOverTorOrDefault { it.getTreeState(height) }

    override suspend fun fetchUtxos(
        tAddresses: List<String>,
        startHeight: BlockHeightUnsafe
    ) = lightWalletClient.fetchUtxos(tAddresses, startHeight)

    override fun getBlockRange(
        heightRange: ClosedRange<BlockHeightUnsafe>
    ) = lightWalletClient.getBlockRange(heightRange)

    override fun getTAddressTransactions(
        tAddress: String,
        blockHeightRange: ClosedRange<BlockHeightUnsafe>
    ) = lightWalletClient.getTAddressTransactions(tAddress, blockHeightRange)

    override fun getSubtreeRoots(
        startIndex: UInt,
        shieldedProtocol: ShieldedProtocolEnum,
        maxEntries: UInt
    ) = lightWalletClient.getSubtreeRoots(startIndex, shieldedProtocol, maxEntries)

    override fun reconnect() = lightWalletClient.reconnect()

    override fun shutdown() = lightWalletClient.shutdown()

    override fun close() = lightWalletClient.close()

    private suspend inline fun <reified T> executeOverTorOrDefault(
        block: (PartialWalletClient) -> Response<T>,
    ): Response<T> {
        return semaphore.withLock {
            val torResult = block(torWalletClient)
            if (torResult is Response.Failure.OverTor) {
                block(lightWalletClient)
            } else {
                torResult
            }
        }
    }
}