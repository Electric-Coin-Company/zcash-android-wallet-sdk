package co.electriccoin.lightwallet.client

import co.electriccoin.lightwallet.client.model.BlockHeightUnsafe
import co.electriccoin.lightwallet.client.model.CompactBlockUnsafe
import co.electriccoin.lightwallet.client.model.GetAddressUtxosReplyUnsafe
import co.electriccoin.lightwallet.client.model.LightWalletEndpointInfoUnsafe
import co.electriccoin.lightwallet.client.model.RawTransactionUnsafe
import co.electriccoin.lightwallet.client.model.Response
import co.electriccoin.lightwallet.client.model.SendResponseUnsafe
import co.electriccoin.lightwallet.client.model.ShieldedProtocolEnum
import co.electriccoin.lightwallet.client.model.SubtreeRootUnsafe
import co.electriccoin.lightwallet.client.model.TreeStateUnsafe
import co.electriccoin.lightwallet.client.util.Disposable
import kotlinx.coroutines.flow.Flow

/**
 * Client for interacting with lightwalletd.
 */
@Suppress("TooManyFunctions")
interface CombinedWalletClient : Disposable {
    /**
     * @param tAddresses the array containing the transparent addresses to use.
     * @param startHeight the starting height to use.
     *
     * @return a flow of UTXOs for the given addresses from the [startHeight].
     *
     * @throws IllegalArgumentException when empty argument provided or when serviceMode is not [ServiceMode.Direct]
     */
    suspend fun fetchUtxos(
        tAddresses: List<String>,
        startHeight: BlockHeightUnsafe,
        serviceMode: ServiceMode
    ): Flow<Response<GetAddressUtxosReplyUnsafe>>

    /**
     * @param heightRange the inclusive range to fetch. For instance if 1..5 is given, then every
     * block in that range will be fetched, including 1 and 5.
     *
     * @return a flow of compact blocks for the given range
     *
     * @throws IllegalArgumentException when empty argument provided or when serviceMode is not [ServiceMode.Direct]
     */
    suspend fun getBlockRange(
        heightRange: ClosedRange<BlockHeightUnsafe>,
        serviceMode: ServiceMode
    ): Flow<Response<CompactBlockUnsafe>>

    /**
     * Gets all the transactions for a given t-address over the given range.  In practice, this is
     * effectively the same as an RPC call to a node that's running an insight server. The data is
     * indexed and responses are fairly quick.
     *
     * @return a flow of transactions that correspond to the given address for the given range.
     *
     * @throws IllegalArgumentException when empty argument provided or when serviceMode is not [ServiceMode.Direct]
     */
    suspend fun getTAddressTransactions(
        tAddress: String,
        blockHeightRange: ClosedRange<BlockHeightUnsafe>,
        serviceMode: ServiceMode
    ): Flow<Response<RawTransactionUnsafe>>

    /**
     * Returns a stream of information about roots of subtrees of the note commitment tree for the specified protocol.
     *
     * @return a flow of information about roots of subtrees of the note commitment tree for `shieldedProtocol`.
     *
     * @param startIndex Index identifying where to start returning subtree roots
     * @param shieldedProtocol Shielded protocol to return subtree roots for. See `ShieldedProtocolEnum` enum class.
     * @param maxEntries Maximum number of entries to return, or 0 for all entries
     *
     * @throws IllegalArgumentException when empty argument provided or when serviceMode is not [ServiceMode.Direct]
     */
    suspend fun getSubtreeRoots(
        startIndex: UInt,
        shieldedProtocol: ShieldedProtocolEnum,
        maxEntries: UInt,
        serviceMode: ServiceMode
    ): Flow<Response<SubtreeRootUnsafe>>

    /**
     * @return a flow of transactions in the mempool.
     */
    suspend fun observeMempool(serviceMode: ServiceMode): Flow<Response<RawTransactionUnsafe>>

    /**
     * @return useful server details.
     */
    suspend fun getServerInfo(
        serviceMode: ServiceMode
    ): Response<LightWalletEndpointInfoUnsafe>

    /**
     * @return the latest block height known to the service.
     */
    suspend fun getLatestBlockHeight(
        serviceMode: ServiceMode
    ): Response<BlockHeightUnsafe>

    /**
     * @return the full transaction info.
     */
    suspend fun fetchTransaction(
        txId: ByteArray,
        serviceMode: ServiceMode
    ): Response<RawTransactionUnsafe>

    /**
     * Submit a raw transaction.
     *
     * @return the response from the server.
     */
    suspend fun submitTransaction(
        tx: ByteArray,
        serviceMode: ServiceMode
    ): Response<SendResponseUnsafe>

    /**
     * @return information about roots of subtrees of the Sapling and Orchard note commitment trees.
     */
    suspend fun getTreeState(
        height: BlockHeightUnsafe,
        serviceMode: ServiceMode
    ): Response<TreeStateUnsafe>

    suspend fun checkSingleUseTransparentAddress(
        accountUuid: ByteArray,
        serviceMode: ServiceMode
    ): Response<String?>

    /**
     * Reconnect to the same or a different server. This is useful when the connection is
     * unrecoverable. That might be time to switch to a mirror or just reconnect.
     */
    fun reconnect()
}

/**
 * Mode that determines which connection is used for the lightwalletd networking calls.
 */
sealed interface ServiceMode {
    /**
     * GRPC connection is used, no Tor involved.
     */
    data object Direct : ServiceMode

    /**
     * Default Tor connection is used, lives for the lifetime of the CombinedWalletClient.
     */
    data object DefaultTor : ServiceMode

    /**
     * Tor connection is used, each time a new one, not held in memory, used only once.
     */
    data object UniqueTor : ServiceMode

    /**
     * Tor connection is used tagged by a given group name (String).
     * Tags are held in memory for the lifetime of the CombinedWalletClient.
     */
    data class Group(
        val group: String
    ) : ServiceMode
}
