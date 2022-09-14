package co.electriccoin.lightwallet.client

import android.content.Context
import cash.z.wallet.sdk.rpc.CompactFormats
import cash.z.wallet.sdk.rpc.Service
import co.electriccoin.lightwallet.client.internal.AndroidChannelFactory
import co.electriccoin.lightwallet.client.internal.BlockingLightWalletClientImpl
import co.electriccoin.lightwallet.client.model.BlockHeightUnsafe
import co.electriccoin.lightwallet.client.model.LightWalletEndpoint
import co.electriccoin.lightwallet.client.model.LightWalletEndpointInfoUnsafe
import co.electriccoin.lightwallet.client.model.Response

/**
 * Client for interacting with lightwalletd.
 */
interface BlockingLightWalletClient {

    /**
     * @return the full transaction info.
     */
    fun fetchTransaction(txId: ByteArray): Service.RawTransaction?

    /**
     * @param tAddress the transparent address to use.
     * @param startHeight the starting height to use.
     *
     * @return the UTXOs for the given address from the [startHeight].
     */
    fun fetchUtxos(
        tAddress: String,
        startHeight: BlockHeightUnsafe
    ): List<Service.GetAddressUtxosReply>

    /**
     * @param heightRange the inclusive range to fetch. For instance if 1..5 is given, then every
     * block in that range will be fetched, including 1 and 5.
     *
     * @return a list of compact blocks for the given range
     *
     */
    fun getBlockRange(heightRange: ClosedRange<BlockHeightUnsafe>): Sequence<CompactFormats.CompactBlock>

    /**
     * @return the latest block height known to the service.
     */
    fun getLatestBlockHeight(): Response<BlockHeightUnsafe>

    /**
     * @return basic server information.
     */
    fun getServerInfo(): Response<LightWalletEndpointInfoUnsafe>

    /**
     * Gets all the transactions for a given t-address over the given range.  In practice, this is
     * effectively the same as an RPC call to a node that's running an insight server. The data is
     * indexed and responses are fairly quick.
     *
     * @return a list of transactions that correspond to the given address for the given range.
     */
    fun getTAddressTransactions(
        tAddress: String,
        blockHeightRange: ClosedRange<BlockHeightUnsafe>
    ): Sequence<Service.RawTransaction>

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
    suspend fun submitTransaction(spendTransaction: ByteArray): Service.SendResponse

    companion object {
        internal const val DEFAULT_ERROR_CODE = 3000
    }
}

/**
 * @return A new client specifically for Android devices.
 */
fun BlockingLightWalletClient.Companion.new(
    context: Context,
    lightWalletEndpoint: LightWalletEndpoint
): BlockingLightWalletClient =
    BlockingLightWalletClientImpl.new(AndroidChannelFactory(context), lightWalletEndpoint)
