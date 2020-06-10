package cash.z.ecc.android.sdk.block

import cash.z.ecc.android.sdk.service.LightWalletService
import cash.z.wallet.sdk.rpc.Service
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext

/**
 * Serves as a source of compact blocks received from the light wallet server. Once started, it will use the given
 * lightwallet service to request all the appropriate blocks and compact block store to persist them. By delegating to
 * these dependencies, the downloader remains agnostic to the particular implementation of how to retrieve and store
 * data; although, by default the SDK uses gRPC and SQL.
 *
 * @property lightwalletService the service used for requesting compact blocks
 * @property compactBlockStore responsible for persisting the compact blocks that are received
 */
open class CompactBlockDownloader(
    val lightwalletService: LightWalletService,
    val compactBlockStore: CompactBlockStore
) {

    /**
     * Requests the given range of blocks from the lightwalletService and then persists them to the
     * compactBlockStore.
     *
     * @param heightRange the inclusive range of heights to request. For example 10..20 would
     * request 11 blocks (including block 10 and block 20).
     *
     * @return the number of blocks that were returned in the results from the lightwalletService.
     */
    suspend fun downloadBlockRange(heightRange: IntRange): Int = withContext(IO) {
        val result = lightwalletService.getBlockRange(heightRange)
        compactBlockStore.write(result)
        result.size
    }

    /**
     * Rewind the storage to the given height, usually to handle reorgs.
     *
     * @param height the height to which the data will rewind.
     */
    suspend fun rewindToHeight(height: Int) = withContext(IO) {
        // TODO: cancel anything in flight
        compactBlockStore.rewindTo(height)
    }

    /**
     * Return the latest block height known by the lightwalletService.
     *
     * @return the latest block height.
     */
    suspend fun getLatestBlockHeight() = withContext(IO) {
        lightwalletService.getLatestBlockHeight()
    }

    /**
     * Return the latest block height that has been persisted into the [CompactBlockStore].
     *
     * @return the latest block height that has been persisted.
     */
    suspend fun getLastDownloadedHeight() = withContext(IO) {
        compactBlockStore.getLatestHeight()
    }

    suspend fun getServerInfo(): Service.LightdInfo = withContext(IO) {
        lightwalletService.getServerInfo()
    }

    /**
     * Stop this downloader and cleanup any resources being used.
     */
    fun stop() {
        lightwalletService.shutdown()
        compactBlockStore.close()
    }

    /**
     * Fetch the details of a known transaction.
     *
     * @return the full transaction info.
     */
    fun fetchTransaction(txId: ByteArray) = lightwalletService.fetchTransaction(txId)

}

