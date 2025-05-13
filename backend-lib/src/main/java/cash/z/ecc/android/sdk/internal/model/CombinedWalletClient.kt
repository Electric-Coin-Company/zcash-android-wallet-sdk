package cash.z.ecc.android.sdk.internal.model

import co.electriccoin.lightwallet.client.PartialTorWalletClient
import co.electriccoin.lightwallet.client.LightWalletClient
import co.electriccoin.lightwallet.client.WalletClient
import co.electriccoin.lightwallet.client.model.BlockHeightUnsafe
import co.electriccoin.lightwallet.client.model.ShieldedProtocolEnum

class CombinedWalletClient(
    private val lightWalletClient: LightWalletClient,
    private val torWalletClient: PartialTorWalletClient,
) : WalletClient {

    override suspend fun fetchTransaction(txId: ByteArray) = torWalletClient.fetchTransaction(txId)

    override suspend fun getServerInfo() = torWalletClient.getServerInfo()

    override suspend fun getLatestBlockHeight() = torWalletClient.getLatestBlockHeight()

    override suspend fun submitTransaction(tx: ByteArray) = torWalletClient.submitTransaction(tx)

    override suspend fun getTreeState(height: BlockHeightUnsafe) = torWalletClient.getTreeState(height)

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
}