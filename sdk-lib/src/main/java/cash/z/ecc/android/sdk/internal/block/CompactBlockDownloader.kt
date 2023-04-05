package cash.z.ecc.android.sdk.internal.block

import cash.z.ecc.android.sdk.internal.Twig
import cash.z.ecc.android.sdk.internal.ext.retryUpTo
import cash.z.ecc.android.sdk.internal.model.from
import cash.z.ecc.android.sdk.internal.repository.CompactBlockRepository
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
 * lightWallet client to request all the appropriate blocks and compact block store to persist them. By delegating to
 * these dependencies, the downloader remains agnostic to the particular implementation of how to retrieve and store
 * data; although, by default the SDK uses gRPC and SQL.
 *
 * @property lightWalletClient the client used for requesting compact blocks
 * @property compactBlockStore responsible for persisting the compact blocks that are received
 */
open class CompactBlockDownloader private constructor(val compactBlockRepository: CompactBlockRepository) {

    lateinit var lightWalletClient: BlockingLightWalletClient
        private set

    constructor(
        lightWalletClient: BlockingLightWalletClient,
        compactBlockRepository: CompactBlockRepository
    ) : this(compactBlockRepository) {
        this.lightWalletClient = lightWalletClient
    }

    /**
     * Requests the given range of blocks from the lightWalletClient and then persists them to the
     * compactBlockStore.
     *
     * @param heightRange the inclusive range of heights to request. For example 10..20 would
     * request 11 blocks (including block 10 and block 20).
     *
     * @return the number of blocks that were returned in the results from the lightWalletClient, or -1 in case of
     * any communication error.
     */
    suspend fun downloadBlockRange(heightRange: ClosedRange<BlockHeight>): Int = withContext(IO) {
        val response = lightWalletClient.getBlockRange(
            BlockHeightUnsafe.from(heightRange.start)..BlockHeightUnsafe.from(heightRange.endInclusive)
        )
        when (response) {
            is Response.Success -> {
                Twig.debug {
                    "Downloading blocks in range: $heightRange succeeded with ${response.result.count()} blocks " +
                        "downloaded."
                }
                compactBlockRepository.write(response.result)
            }
            else -> {
                Twig.debug { "Downloading blocks in range: $heightRange failed with: $response." }
                -1
            }
        }
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
     * Return the latest block height known by the lightWalletClient.
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
            when (val response = lightWalletClient.getServerInfo()) {
                is Response.Success -> return@withContext response.result
                else -> {
                    lightWalletClient.reconnect()
                    Twig.warn { "WARNING: reconnecting to server in response to failure (retry #${it + 1})" }
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
