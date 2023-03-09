package co.electriccoin.lightwallet.client

import android.content.Context
import cash.z.wallet.sdk.internal.rpc.CompactFormats
import cash.z.wallet.sdk.internal.rpc.Service
import co.electriccoin.lightwallet.client.internal.AndroidChannelFactory
import co.electriccoin.lightwallet.client.internal.CoroutineLightWalletClientImpl
import co.electriccoin.lightwallet.client.model.BlockHeightUnsafe
import co.electriccoin.lightwallet.client.model.LightWalletEndpoint
import co.electriccoin.lightwallet.client.model.LightWalletEndpointInfoUnsafe
import co.electriccoin.lightwallet.client.model.RawTransactionUnsafe
import co.electriccoin.lightwallet.client.model.Response
import co.electriccoin.lightwallet.client.model.SendResponseUnsafe
import kotlinx.coroutines.flow.Flow

// TODO [895]: Let remaining server calls adopt new response result
// TODO [895]: https://github.com/zcash/zcash-android-wallet-sdk/issues/895

/**
 * Client for interacting with lightwalletd.
 */
interface CoroutineLightWalletClient {

    /**
     * @return the full transaction info.
     */
    suspend fun fetchTransaction(txId: ByteArray): Response<RawTransactionUnsafe>

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
    ): Flow<Service.GetAddressUtxosReply>

    /**
     * @param heightRange the inclusive range to fetch. For instance if 1..5 is given, then every
     * block in that range will be fetched, including 1 and 5.
     *
     * @return a flow of compact blocks for the given range
     *
     * @throws IllegalArgumentException when empty argument provided
     */
    fun getBlockRange(heightRange: ClosedRange<BlockHeightUnsafe>): Flow<CompactFormats.CompactBlock>

    /**
     * @return the latest block height known to the service.
     */
    suspend fun getLatestBlockHeight(): Response<BlockHeightUnsafe>

    /**
     * @return useful server details.
     */
    suspend fun getServerInfo(): Response<LightWalletEndpointInfoUnsafe>

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
    ): Flow<Service.RawTransaction>

    /**
     * Reconnect to the same or a different server. This is useful when the connection is
     * unrecoverable. That might be time to switch to a mirror or just reconnect.
     */
    fun reconnect()

    /**
     * Cleanup any connections when the service is shutting down and not going to be used again.
     */
    fun shutdown()

    /**
     * Submit a raw transaction.
     *
     * @return the response from the server.
     */
    suspend fun submitTransaction(spendTransaction: ByteArray): Response<SendResponseUnsafe>

    companion object
}

/**
 * @return A new client specifically for Android devices.
 */
fun CoroutineLightWalletClient.Companion.new(
    context: Context,
    lightWalletEndpoint: LightWalletEndpoint
): CoroutineLightWalletClient =
    CoroutineLightWalletClientImpl.new(AndroidChannelFactory(context), lightWalletEndpoint)
