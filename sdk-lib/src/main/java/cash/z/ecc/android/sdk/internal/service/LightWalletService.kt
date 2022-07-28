package cash.z.ecc.android.sdk.internal.service

import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.wallet.sdk.rpc.CompactFormats
import cash.z.wallet.sdk.rpc.Service

/**
 * Service for interacting with lightwalletd. Implementers of this service should make blocking
 * calls because async concerns are handled at a higher level.
 */
interface LightWalletService {

    /**
     * Fetch the details of a known transaction.
     *
     * @return the full transaction info.
     */
    fun fetchTransaction(txId: ByteArray): Service.RawTransaction?

    /**
     * Fetch all UTXOs for the given address, going back to the start height.
     *
     * @param tAddress the transparent address to use.
     * @param startHeight the starting height to use.
     *
     * @return the UTXOs for the given address from the startHeight.
     */
    fun fetchUtxos(tAddress: String, startHeight: BlockHeight): List<Service.GetAddressUtxosReply>

    /**
     * Return the given range of blocks.
     *
     * @param heightRange the inclusive range to fetch. For instance if 1..5 is given, then every
     * block in that range will be fetched, including 1 and 5.
     *
     * @return a list of compact blocks for the given range
     *
     */
    fun getBlockRange(heightRange: ClosedRange<BlockHeight>): Sequence<CompactFormats.CompactBlock>

    /**
     * Return the latest block height known to the service.
     *
     * @return the latest block height known to the service.
     */
    fun getLatestBlockHeight(): BlockHeight

    /**
     * Return basic information about the server such as:
     *
     * ```
     * {
     *     "version": "0.2.1",
     *     "vendor": "ECC LightWalletD",
     *     "taddrSupport": true,
     *     "chainName": "main",
     *     "saplingActivationHeight": 419200,
     *     "consensusBranchId": "2bb40e60",
     *     "blockHeight": 861272
     * }
     * ```
     *
     * @return useful server details.
     */
    fun getServerInfo(): Service.LightdInfo

    /**
     * Gets all the transactions for a given t-address over the given range.  In practice, this is
     * effectively the same as an RPC call to a node that's running an insight server. The data is
     * indexed and responses are fairly quick.
     *
     * @return a list of transactions that correspond to the given address for the given range.
     */
    fun getTAddressTransactions(tAddress: String, blockHeightRange: ClosedRange<BlockHeight>): List<Service.RawTransaction>

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
    fun submitTransaction(spendTransaction: ByteArray): Service.SendResponse
}
