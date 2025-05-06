package co.electriccoin.lightwallet.client

import co.electriccoin.lightwallet.client.model.BlockHeightUnsafe
import co.electriccoin.lightwallet.client.model.CompactBlockUnsafe
import co.electriccoin.lightwallet.client.model.GetAddressUtxosReplyUnsafe
import co.electriccoin.lightwallet.client.model.RawTransactionUnsafe
import co.electriccoin.lightwallet.client.model.Response
import co.electriccoin.lightwallet.client.model.ShieldedProtocolEnum
import co.electriccoin.lightwallet.client.model.SubtreeRootUnsafe
import kotlinx.coroutines.flow.Flow
import java.io.Closeable

/**
 * Client for interacting with lightwalletd.
 */
@Suppress("TooManyFunctions")
interface BaseWalletClient : BasicWalletClient, Closeable {

    /**
     * @param tAddresses the array containing the transparent addresses to use.
     * @param startHeight the starting height to use.
     *
     * @return a flow of UTXOs for the given addresses from the [startHeight].
     *
     * @throws IllegalArgumentException when empty argument provided
     */
    suspend fun fetchUtxos(
        tAddresses: List<String>,
        startHeight: BlockHeightUnsafe
    ): Flow<Response<GetAddressUtxosReplyUnsafe>>

    /**
     * @param heightRange the inclusive range to fetch. For instance if 1..5 is given, then every
     * block in that range will be fetched, including 1 and 5.
     *
     * @return a flow of compact blocks for the given range
     *
     * @throws IllegalArgumentException when empty argument provided
     */
    fun getBlockRange(heightRange: ClosedRange<BlockHeightUnsafe>): Flow<Response<CompactBlockUnsafe>>

    /**
     * Gets all the transactions for a given t-address over the given range.  In practice, this is
     * effectively the same as an RPC call to a node that's running an insight server. The data is
     * indexed and responses are fairly quick.
     *
     * @return a flow of transactions that correspond to the given address for the given range.
     *
     * @throws IllegalArgumentException when empty argument provided
     */
    fun getTAddressTransactions(
        tAddress: String,
        blockHeightRange: ClosedRange<BlockHeightUnsafe>
    ): Flow<Response<RawTransactionUnsafe>>

    /**
     * Returns a stream of information about roots of subtrees of the Sapling and Orchard note commitment trees.
     *
     * @return a flow of information about roots of subtrees of the Sapling and Orchard note commitment trees.
     *
     * @param startIndex Index identifying where to start returning subtree roots
     * @param shieldedProtocol Shielded protocol to return subtree roots for. See `ShieldedProtocolEnum` enum class.
     * @param maxEntries Maximum number of entries to return, or 0 for all entries
     *
     * @throws IllegalArgumentException when empty argument provided
     */
    fun getSubtreeRoots(
        startIndex: UInt,
        shieldedProtocol: ShieldedProtocolEnum,
        maxEntries: UInt
    ): Flow<Response<SubtreeRootUnsafe>>

    /**
     * Reconnect to the same or a different server. This is useful when the connection is
     * unrecoverable. That might be time to switch to a mirror or just reconnect.
     */
    fun reconnect()

    /**
     * Cleanup any connections when the service is shutting down and not going to be used again.
     */
    fun shutdown()
}
