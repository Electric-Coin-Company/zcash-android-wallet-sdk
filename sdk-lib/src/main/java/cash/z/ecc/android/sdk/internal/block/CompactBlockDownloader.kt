package cash.z.ecc.android.sdk.internal.block

import cash.z.ecc.android.sdk.internal.ext.retryUpTo
import cash.z.ecc.android.sdk.internal.model.from
import cash.z.ecc.android.sdk.internal.repository.CompactBlockRepository
import cash.z.ecc.android.sdk.internal.twig
import cash.z.ecc.android.sdk.model.BlockHeight
import co.electriccoin.lightwallet.client.BlockingLightWalletClient
import co.electriccoin.lightwallet.client.model.BlockHeightUnsafe
import co.electriccoin.lightwallet.client.model.LightWalletEndpointInfoUnsafe
import co.electriccoin.lightwallet.client.model.Response
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext

/**
 * Serves as a source of compact blocks received from the light wallet server. Once started, it will use the given
 * lightwallet service to request all the appropriate blocks and compact block store to persist them. By delegating to
 * these dependencies, the downloader remains agnostic to the particular implementation of how to retrieve and store
 * data; although, by default the SDK uses gRPC and SQL.
 *
 * @property lightWalletClient the service used for requesting compact blocks
 * @property compactBlockStore responsible for persisting the compact blocks that are received
 */
open class CompactBlockDownloader private constructor(val compactBlockRepository: CompactBlockRepository) {

    lateinit var lightWalletClient: BlockingLightWalletClient
        private set

    constructor(
        lightWalletService: BlockingLightWalletClient,
        compactBlockRepository: CompactBlockRepository
    ) : this(compactBlockRepository) {
        this.lightWalletClient = lightWalletService
    }

    /**
     * Requests the given range of blocks from the lightwalletService and then persists them to the
     * compactBlockStore.
     *
     * @param heightRange the inclusive range of heights to request. For example 10..20 would
     * request 11 blocks (including block 10 and block 20).
     *
     * @return the number of blocks that were returned in the results from the lightwalletService.
     */
    suspend fun downloadBlockRange(heightRange: ClosedRange<BlockHeight>): Int = withContext(IO) {
        val result = lightWalletClient.getBlockRange(
            BlockHeightUnsafe.from(heightRange.start)..BlockHeightUnsafe.from(heightRange.endInclusive)
        )
        compactBlockRepository.write(result)
    }

    /**
     * Rewind the storage to the given height, usually to handle reorgs. Deletes all blocks above
     * the given height.
     *
     * @param height the height to which the data will rewind.
     */
    suspend fun rewindToHeight(height: BlockHeight) =
        // TODO [#685]: cancel anything in flight
        // TODO [#685]: https://github.com/zcash/zcash-android-wallet-sdk/issues/685
        compactBlockRepository.rewindTo(height)

    /**
     * Return the latest block height known by the lightwalletService.
     *
     * @return the latest block height.
     */
    suspend fun getLatestBlockHeight() =
        lightWalletClient.getLatestBlockHeight()

    /**
     * Return the latest block height that has been persisted into the [CompactBlockRepository].
     *
     * @return the latest block height that has been persisted.
     */
    suspend fun getLastDownloadedHeight() =
        compactBlockRepository.getLatestHeight()

    suspend fun getServerInfo(): LightWalletEndpointInfoUnsafe? = withContext(IO) {
        retryUpTo(GET_SERVER_INFO_RETRIES) {
            when (val result = lightWalletClient.getServerInfo()) {
                is Response.Success -> return@withContext result.result
                else -> {
                    lightWalletClient.reconnect()
                    twig("WARNING: reconnecting to service in response to failure (retry #${it + 1})")
                }
            }
        }

        null
    }

    /**
     * Stop this downloader and cleanup any resources being used.
     */
    suspend fun stop() {
        withContext(Dispatchers.IO) {
            lightWalletClient.shutdown()
        }
        compactBlockRepository.close()
    }

    /**
     * Fetch the details of a known transaction.
     *
     * @return the full transaction info.
     */
    fun fetchTransaction(txId: ByteArray) = lightWalletClient.fetchTransaction(txId)

    companion object {
        private const val GET_SERVER_INFO_RETRIES = 6
    }
}
