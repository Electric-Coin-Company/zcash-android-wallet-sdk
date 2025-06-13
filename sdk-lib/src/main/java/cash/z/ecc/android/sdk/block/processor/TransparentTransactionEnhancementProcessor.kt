package cash.z.ecc.android.sdk.block.processor

import cash.z.ecc.android.sdk.exception.CompactBlockProcessorException
import cash.z.ecc.android.sdk.exception.CompactBlockProcessorException.EnhanceTransactionError.EnhanceTxDecryptError
import cash.z.ecc.android.sdk.exception.CompactBlockProcessorException.EnhanceTransactionError.EnhanceTxDownloadError
import cash.z.ecc.android.sdk.exception.LightWalletException
import cash.z.ecc.android.sdk.internal.Twig
import cash.z.ecc.android.sdk.internal.TypesafeBackend
import cash.z.ecc.android.sdk.internal.block.CompactBlockDownloader
import cash.z.ecc.android.sdk.internal.ext.retryUpToAndThrow
import cash.z.ecc.android.sdk.internal.metrics.withTraceScope
import cash.z.ecc.android.sdk.internal.model.TransactionDataRequest
import cash.z.ecc.android.sdk.model.RawTransaction
import co.electriccoin.lightwallet.client.ServiceMode
import co.electriccoin.lightwallet.client.model.RawTransactionUnsafe
import co.electriccoin.lightwallet.client.util.Disposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal interface TransparentTransactionEnhancementProcessor : Disposable {
    fun start()

    fun stop()

    companion object Factory {
        fun new(
            backend: TypesafeBackend,
            downloader: CompactBlockDownloader
        ): TransparentTransactionEnhancementProcessor =
            TransparentTransactionEnhancementProcessorImpl(
                backend = backend,
                downloader = downloader
            )
    }
}

private class TransparentTransactionEnhancementProcessorImpl(
    private val backend: TypesafeBackend,
    private val downloader: CompactBlockDownloader
) : TransparentTransactionEnhancementProcessor {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val pipeline =
        flow {
            while (true) {
                delay(3.seconds + (0..2000).random().milliseconds) // initial delay
                emit(backend.getMaxScannedHeight())
            }
        }.distinctUntilChanged()

    private var job: Job? = null

    private val semaphore = Mutex()

    override fun start() {
        job =
            pipeline
                .onEach {
                    semaphore.withLock {
                        if (it != null) {
                            getTransactionDataRequests()
                                .orEmpty()
                                .filterIsInstance<TransactionDataRequest.SpendFromAddress>()
                                .forEach {
                                    enhanceTransparentAddressTxId(it)
                                }
                        }
                    }
                }.launchIn(scope)
    }

    override fun stop() {
        job?.cancel()
        job = null
    }

    override suspend fun dispose() {
        try {
            scope.cancel()
        } catch (_: IllegalStateException) {
            // ignored
        }
    }

    private suspend fun enhanceTransparentAddressTxId(
        transactionRequest: TransactionDataRequest.SpendFromAddress
    ) = withTraceScope("TransparentTransactionEnhancementProcessor.enhanceTransparentAddressTxId") {
        Twig.debug { "Starting to get transparent address transactions ids" }

        // TODO [#1564]: Support empty block range end
        if (transactionRequest.endHeight == null) {
            Twig.error { "Unexpected Null " }
            return@withTraceScope
        }

        try {
            // Fetching transactions is done with retries to eliminate a bad network condition
            getTransparentAddressTransactions(
                transactionRequest = transactionRequest,
            ).onEach { rawTransactionUnsafe ->
                decryptTransaction(
                    backend = backend,
                    rawTransactionUnsafe = rawTransactionUnsafe,
                )
            }.onCompletion {
                Twig.verbose { "Done Decrypting and storing of all transaction" }
            }.collect()
        } catch (exception: CompactBlockProcessorException.EnhanceTransactionError) {
            // do nothing
        } catch (e: EnhanceTxDownloadError) {
            // do nothing
        } catch (e: EnhanceTxDecryptError) {
            // do nothing
        } catch (e: Exception) {
            // do nothing
        }
    }

    @Throws(EnhanceTxDownloadError::class, LightWalletException.GetTAddressTransactionsException::class)
    private suspend fun getTransparentAddressTransactions(
        transactionRequest: TransactionDataRequest.SpendFromAddress
    ): Flow<RawTransactionUnsafe> =
        withTraceScope("CompactBlockProcessor.getTransparentAddressTransactions") {
            retryUpToAndThrow(
                retries = TRANSACTION_FETCH_RETRIES,
                exceptionWrapper = { EnhanceTxDownloadError(it) }
            ) { failedAttempts ->
                if (failedAttempts == 0) {
                    Twig.debug { "Starting to get transactions for tAddr: ${transactionRequest.address}" }
                } else {
                    Twig.warn {
                        "Retrying to fetch transactions for tAddr: ${transactionRequest.address}" +
                            " after $failedAttempts failure(s)..."
                    }
                }

                // We can safely assert non-nullability here as we check in the function caller
                // - 1 for the end height because the GRPC request is end-inclusive whereas we use end-exclusive
                // ranges everywhere in the Rust code
                val requestedRange = transactionRequest.startHeight..(transactionRequest.endHeight!! - 1)
                downloader.getTAddressTransactions(
                    transparentAddress = transactionRequest.address,
                    blockHeightRange = requestedRange,
                    serviceMode = ServiceMode.Direct
                )
            }
        }

    @Throws(EnhanceTxDecryptError::class)
    private suspend fun decryptTransaction(
        backend: TypesafeBackend,
        rawTransactionUnsafe: RawTransactionUnsafe,
    ) = withTraceScope("CompactBlockProcessor.decryptTransaction") {
        // Decrypting and storing transaction is run just once, since we consider it more stable
        Twig.verbose { "Decrypting and storing rawTransactionUnsafe" }

        val rawTransaction = RawTransaction.new(rawTransactionUnsafe = rawTransactionUnsafe)

        runCatching {
            backend.decryptAndStoreTransaction(rawTransaction.data, rawTransaction.height)
        }.onFailure {
            throw EnhanceTxDecryptError(rawTransaction.height, it)
        }
    }

    private suspend fun getTransactionDataRequests(): List<TransactionDataRequest>? =
        withTraceScope("CompactBlockProcessor.transactionDataRequests") {
            runCatching { backend.transactionDataRequests() }.getOrNull()
        }
}

private const val TRANSACTION_FETCH_RETRIES = 1
