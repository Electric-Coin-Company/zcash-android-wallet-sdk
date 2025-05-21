package cash.z.ecc.android.sdk.internal.model

import co.electriccoin.lightwallet.client.LightWalletClient
import co.electriccoin.lightwallet.client.PartialTorWalletClient
import co.electriccoin.lightwallet.client.WalletClient
import co.electriccoin.lightwallet.client.model.BlockHeightUnsafe
import co.electriccoin.lightwallet.client.model.ShieldedProtocolEnum
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Suppress("TooManyFunctions")
class CombinedWalletClient private constructor(
    private val lightWalletClient: LightWalletClient,
    private val torWalletClient: PartialTorWalletClient,
) : WalletClient {
    private val semaphore = Mutex()

    override suspend fun dispose() =
        semaphore.withLock {
            lightWalletClient.dispose()
            torWalletClient.dispose()
        }

    override suspend fun fetchTransaction(txId: ByteArray) =
        semaphore.withLock {
            torWalletClient.fetchTransaction(txId)
        }

    override suspend fun getServerInfo() =
        semaphore.withLock {
            torWalletClient.getServerInfo()
        }

    override suspend fun getLatestBlockHeight() =
        semaphore.withLock {
            torWalletClient.getLatestBlockHeight()
        }

    override suspend fun submitTransaction(tx: ByteArray) =
        semaphore.withLock {
            torWalletClient.submitTransaction(tx)
        }

    override suspend fun getTreeState(height: BlockHeightUnsafe) =
        semaphore.withLock {
            torWalletClient.getTreeState(height)
        }

    override suspend fun fetchUtxos(tAddresses: List<String>, startHeight: BlockHeightUnsafe) =
        semaphore.withLock {
            lightWalletClient.fetchUtxos(tAddresses, startHeight)
        }

    override fun getBlockRange(heightRange: ClosedRange<BlockHeightUnsafe>) =
        lightWalletClient.getBlockRange(heightRange)

    override fun getTAddressTransactions(tAddress: String, blockHeightRange: ClosedRange<BlockHeightUnsafe>) =
        lightWalletClient.getTAddressTransactions(tAddress, blockHeightRange)

    override fun getSubtreeRoots(
        startIndex: UInt,
        shieldedProtocol: ShieldedProtocolEnum,
        maxEntries: UInt
    ) = lightWalletClient.getSubtreeRoots(startIndex, shieldedProtocol, maxEntries)

    override fun reconnect() = lightWalletClient.reconnect()

    companion object Factory {
        suspend fun new(lightWalletClient: LightWalletClient, torWalletClient: PartialTorWalletClient) =
            CombinedWalletClient(lightWalletClient = lightWalletClient, torWalletClient = torWalletClient,)
    }
}
