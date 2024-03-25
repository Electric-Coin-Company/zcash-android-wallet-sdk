package cash.z.ecc.android.sdk.internal.block

import cash.z.ecc.android.sdk.exception.LightWalletException
import cash.z.ecc.android.sdk.internal.Twig
import cash.z.ecc.android.sdk.internal.ext.retryUpToAndThrow
import cash.z.ecc.android.sdk.internal.model.JniBlockMeta
import cash.z.ecc.android.sdk.internal.model.ext.from
import cash.z.ecc.android.sdk.internal.repository.CompactBlockRepository
import cash.z.ecc.android.sdk.model.BlockHeight
import co.electriccoin.lightwallet.client.LightWalletClient
import co.electriccoin.lightwallet.client.model.BlockHeightUnsafe
import co.electriccoin.lightwallet.client.model.CompactBlockUnsafe
import co.electriccoin.lightwallet.client.model.LightWalletEndpointInfoUnsafe
import co.electriccoin.lightwallet.client.model.Response
import co.electriccoin.lightwallet.client.model.ShieldedProtocolEnum
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
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
@Suppress("TooManyFunctions")
open class CompactBlockDownloader private constructor(val compactBlockRepository: CompactBlockRepository) {
    private lateinit var lightWalletClient: LightWalletClient

    constructor(
        lightWalletClient: LightWalletClient,
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
     * @throws LightWalletException.DownloadBlockException if any error while downloading the blocks occurs
     * @return List of JniBlockMeta objects, which describe the original CompactBlock objects, which were just
     * downloaded and persisted on the device disk
     */
    @Throws(LightWalletException.DownloadBlockException::class)
    suspend fun downloadBlockRange(heightRange: ClosedRange<BlockHeight>): List<JniBlockMeta> {
        val filteredFlow =
            lightWalletClient.getBlockRange(
                BlockHeightUnsafe.from(heightRange.start)..BlockHeightUnsafe.from(heightRange.endInclusive)
            ).onEach { response ->
                when (response) {
                    is Response.Success -> {
                        Twig.verbose { "Downloading block at height: ${response.result.height} succeeded." }
                    }
                    is Response.Failure -> {
                        Twig.warn { "Downloading blocks in range: $heightRange failed with: ${response.description}." }
                        throw LightWalletException.DownloadBlockException(
                            response.code,
                            response.description,
                            response.toThrowable()
                        )
                    }
                }
            }
                .filterIsInstance<Response.Success<CompactBlockUnsafe>>()
                .map { response ->
                    response.result
                }
                .onCompletion {
                    if (it != null) {
                        Twig.warn { "Blocks in range $heightRange failed to download with: $it" }
                    } else {
                        Twig.verbose { "All blocks in range $heightRange downloaded successfully" }
                    }
                }

        return compactBlockRepository.write(filteredFlow)
    }

    /**
     * Rewind the storage to the given height, usually to handle reorgs. Deletes all blocks above
     * the given height.
     *
     * @param height the height to which the data will rewind.
     */
    suspend fun rewindToHeight(height: BlockHeight) =
        // TODO [#685]: Cancel anything in flight
        // TODO [#685]: https://github.com/zcash/zcash-android-wallet-sdk/issues/685
        compactBlockRepository.rewindTo(height)

    /**
     * Return the latest block height known by the lightWalletClient.
     *
     * @return the latest block height.
     */
    suspend fun getLatestBlockHeight() = lightWalletClient.getLatestBlockHeight()

    /**
     * Return the latest block height that has been persisted into the [CompactBlockRepository].
     *
     * @return the latest block height that has been persisted.
     */
    suspend fun getLastDownloadedHeight() = compactBlockRepository.getLatestHeight()

    suspend fun getServerInfo(): LightWalletEndpointInfoUnsafe? =
        withContext(IO) {
            retryUpToAndThrow(GET_SERVER_INFO_RETRIES) {
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
        withContext(IO) {
            lightWalletClient.shutdown()
        }
    }

    /**
     * Reconnect to the same or a different server. This is useful when the connection is
     * unrecoverable. That might be time to switch to a mirror or just reconnect.
     */
    suspend fun reconnect() {
        withContext(IO) {
            lightWalletClient.reconnect()
        }
    }

    /**
     * Fetch the details of a known transaction.
     *
     * @return the full transaction info.
     */
    suspend fun fetchTransaction(txId: ByteArray) = lightWalletClient.fetchTransaction(txId)

    /**
     * Fetch all UTXOs for the given addresses and from the given height.
     *
     * @return Flow of UTXOs for the given [tAddresses] from the [startHeight]
     */
    suspend fun fetchUtxos(
        tAddresses: List<String>,
        startHeight: BlockHeightUnsafe
    ) = lightWalletClient.fetchUtxos(
        tAddresses = tAddresses,
        startHeight = startHeight
    )

    /**
     * Returns a stream of information about roots of subtrees of the Sapling and Orchard note commitment trees.
     *
     * @return a flow of information about roots of subtrees of the Sapling and Orchard note commitment trees.
     */
    suspend fun getSubtreeRoots(
        startIndex: UInt,
        shieldedProtocol: ShieldedProtocolEnum,
        maxEntries: UInt
    ) = lightWalletClient.getSubtreeRoots(
        startIndex = startIndex,
        shieldedProtocol = shieldedProtocol,
        maxEntries = maxEntries
    )

    /**
     * Returns information about roots of subtrees of the Sapling and Orchard note commitment trees.
     *
     * @return information about roots of subtrees of the Sapling and Orchard note commitment trees.
     */
    suspend fun getTreeState(height: BlockHeightUnsafe) = lightWalletClient.getTreeState(height = height)

    companion object {
        private const val GET_SERVER_INFO_RETRIES = 6
    }
}
