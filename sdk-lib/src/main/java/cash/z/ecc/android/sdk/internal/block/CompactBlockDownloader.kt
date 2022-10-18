package cash.z.ecc.android.sdk.internal.block
import cash.z.ecc.android.sdk.exception.LightWalletException
import cash.z.ecc.android.sdk.internal.ext.retryUpTo
import cash.z.ecc.android.sdk.internal.ext.tryWarn
import cash.z.ecc.android.sdk.internal.repository.CompactBlockRepository
import cash.z.ecc.android.sdk.internal.service.LightWalletService
import cash.z.ecc.android.sdk.internal.twig
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.wallet.sdk.rpc.Service
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Serves as a source of compact blocks received from the light wallet server. Once started, it will use the given
 * lightwallet service to request all the appropriate blocks and compact block store to persist them. By delegating to
 * these dependencies, the downloader remains agnostic to the particular implementation of how to retrieve and store
 * data; although, by default the SDK uses gRPC and SQL.
 *
 * @property lightWalletService the service used for requesting compact blocks
 * @property compactBlockRepository responsible for persisting the compact blocks that are received
 */
open class CompactBlockDownloader private constructor(val compactBlockRepository: CompactBlockRepository) {

    lateinit var lightWalletService: LightWalletService
        private set

    constructor(
        lightWalletService: LightWalletService,
        compactBlockRepository: CompactBlockRepository
    ) : this(compactBlockRepository) {
        this.lightWalletService = lightWalletService
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
        val result = lightWalletService.getBlockRange(heightRange)
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
        lightWalletService.getLatestBlockHeight()

    /**
     * Return the latest block height that has been persisted into the [CompactBlockRepository].
     *
     * @return the latest block height that has been persisted.
     */
    suspend fun getLastDownloadedHeight() =
        compactBlockRepository.getLatestHeight()

    suspend fun getServerInfo(): Service.LightdInfo = withContext<Service.LightdInfo>(IO) {
        lateinit var result: Service.LightdInfo
        try {
            result = lightWalletService.getServerInfo()
        } catch (e: StatusRuntimeException) {
            retryUpTo(GET_SERVER_INFO_RETRIES) {
                twig("WARNING: reconnecting to service in response to failure (retry #${it + 1}): $e")
                lightWalletService.reconnect()
                result = lightWalletService.getServerInfo()
            }
        }
        result
    }

    suspend fun changeService(
        newService: LightWalletService,
        errorHandler: (Throwable) -> Unit = { throw it }
    ) = withContext(IO) {
        @Suppress("TooGenericExceptionCaught")
        try {
            val existing = lightWalletService.getServerInfo()
            val new = newService.getServerInfo()
            val nonMatching = existing.essentialPropertyDiff(new)

            if (nonMatching.size > 0) {
                errorHandler(
                    LightWalletException.ChangeServerException.ChainInfoNotMatching(
                        nonMatching.joinToString(),
                        existing,
                        new
                    )
                )
            }

            gracefullyShutdown(lightWalletService)
            lightWalletService = newService
        } catch (s: StatusRuntimeException) {
            errorHandler(LightWalletException.ChangeServerException.StatusException(s.status))
        } catch (t: Throwable) {
            errorHandler(t)
        }
    }

    /**
     * Stop this downloader and cleanup any resources being used.
     */
    suspend fun stop() {
        withContext(Dispatchers.IO) {
            lightWalletService.shutdown()
        }
        compactBlockRepository.close()
    }

    /**
     * Fetch the details of a known transaction.
     *
     * @return the full transaction info.
     */
    fun fetchTransaction(txId: ByteArray) = lightWalletService.fetchTransaction(txId)

    //
    // Convenience functions
    //

    private suspend fun CoroutineScope.gracefullyShutdown(service: LightWalletService) = launch {
        @Suppress("MagicNumber")
        delay(2_000L)
        tryWarn("Warning: error while shutting down service") {
            service.shutdown()
        }
    }

    /**
     * Return a list of critical properties that do not match.
     */
    private fun Service.LightdInfo.essentialPropertyDiff(other: Service.LightdInfo) =
        mutableListOf<String>().also {
            if (!consensusBranchId.equals(other.consensusBranchId, true)) {
                it.add("consensusBranchId")
            }
            if (saplingActivationHeight != other.saplingActivationHeight) {
                it.add("saplingActivationHeight")
            }
            if (!chainName.equals(other.chainName, true)) {
                it.add("chainName")
            }
        }

    companion object {
        private const val GET_SERVER_INFO_RETRIES = 6
    }
}
