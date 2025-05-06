package cash.z.ecc.android.sdk.internal.model

import co.electriccoin.lightwallet.client.BasicWalletClient
import co.electriccoin.lightwallet.client.LightWalletClient
import co.electriccoin.lightwallet.client.BaseTorWalletClient
import co.electriccoin.lightwallet.client.BaseWalletClient
import co.electriccoin.lightwallet.client.model.BlockHeightUnsafe
import co.electriccoin.lightwallet.client.model.Response
import co.electriccoin.lightwallet.client.model.ShieldedProtocolEnum
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CombinedWalletClient(
    private val lightWalletClient: LightWalletClient,
    private val torWalletClient: BaseTorWalletClient,
) : BaseWalletClient {

    private val semaphore = Mutex()

    override suspend fun fetchTransaction(txId: ByteArray) = executeTorOrGrpc { it.fetchTransaction(txId) }

    override suspend fun getServerInfo() = executeTorOrGrpc { it.getServerInfo() }

    override suspend fun getLatestBlockHeight() = executeTorOrGrpc { it.getLatestBlockHeight() }

    override suspend fun submitTransaction(tx: ByteArray) = executeTorOrGrpc { it.submitTransaction(tx) }

    override suspend fun getTreeState(height: BlockHeightUnsafe) = executeTorOrGrpc { it.getTreeState(height) }

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

    private suspend inline fun <reified T> executeTorOrGrpc(
        block: (BasicWalletClient) -> Response<T>,
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