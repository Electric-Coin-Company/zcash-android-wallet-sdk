package cash.z.ecc.android.sdk.block.processor

import cash.z.ecc.android.sdk.exception.CompactBlockProcessorException
import cash.z.ecc.android.sdk.exception.CompactBlockProcessorException.EnhanceTransactionError.EnhanceTxDecryptError
import cash.z.ecc.android.sdk.exception.CompactBlockProcessorException.EnhanceTransactionError.EnhanceTxDownloadError
import cash.z.ecc.android.sdk.exception.CompactBlockProcessorException.EnhanceTransactionError.EnhanceTxSetStatusError
import cash.z.ecc.android.sdk.internal.Twig
import cash.z.ecc.android.sdk.internal.TypesafeBackend
import cash.z.ecc.android.sdk.internal.block.CompactBlockDownloader
import cash.z.ecc.android.sdk.internal.ext.retryUpToAndThrow
import cash.z.ecc.android.sdk.internal.metrics.withTraceScope
import cash.z.ecc.android.sdk.internal.model.TransactionDataRequest
import cash.z.ecc.android.sdk.internal.model.TransactionStatus
import cash.z.ecc.android.sdk.internal.model.ext.toTransactionStatus
import cash.z.ecc.android.sdk.internal.repository.DerivedDataRepository
import cash.z.ecc.android.sdk.model.RawTransaction
import cash.z.ecc.android.sdk.model.TransactionId
import co.electriccoin.lightwallet.client.ServiceMode
import co.electriccoin.lightwallet.client.model.RawTransactionUnsafe
import co.electriccoin.lightwallet.client.model.Response
import co.electriccoin.lightwallet.client.util.Disposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal interface TransactionEnhancementProcessor : Disposable {
    fun start()

    fun stop()

    companion object Factory {
        fun new(
            backend: TypesafeBackend,
            downloader: CompactBlockDownloader,
            derivedDataRepository: DerivedDataRepository,
        ): TransactionEnhancementProcessor =
            TransactionEnhancementProcessorImpl(
                backend = backend,
                downloader = downloader,
                derivedDataRepository = derivedDataRepository
            )
    }
}

private class TransactionEnhancementProcessorImpl(
    private val backend: TypesafeBackend,
    private val downloader: CompactBlockDownloader,
    private val derivedDataRepository: DerivedDataRepository,
) : TransactionEnhancementProcessor {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var job: Job? = null

    override fun start() {
        job =
            scope.launch {
                while (true) {
                    delay(9.seconds + (0..2000).random().milliseconds) // initial delay
                    val requests = getTransactionDataRequests() ?: continue // fetch requests
                    requests
                        .filterIsInstance<TransactionDataRequest.Enhanceable>() // filter for GetStatus & Enhancement
                        .forEach { request ->
                            if (enhanceTransaction(request)) { // enhance transaction
                                derivedDataRepository.invalidate() // enhance transaction
                            }
                        }
                }
            }
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

    private suspend fun enhanceTransaction(
        transactionRequest: TransactionDataRequest.Enhanceable
    ) = withTraceScope("CompactBlockProcessor.enhanceTransaction") {
        Twig.debug { "Starting enhancing transaction: txid: ${transactionRequest.txIdString()}" }
        try {
            val rawTransactionUnsafe = getTransaction(transactionRequest, downloader)

            when (transactionRequest) {
                is TransactionDataRequest.GetStatus ->
                    setTransactionStatus(
                        transactionId = transactionRequest.txid,
                        rawTransactionUnsafe = rawTransactionUnsafe,
                        backend = backend
                    )

                is TransactionDataRequest.Enhancement ->
                    if (rawTransactionUnsafe == null) {
                        setTransactionStatus(
                            transactionId = transactionRequest.txid,
                            backend = backend,
                            rawTransactionUnsafe = null
                        )
                    } else {
                        decryptTransaction(
                            transactionId = transactionRequest.txid,
                            rawTransactionUnsafe = rawTransactionUnsafe,
                            backend = backend
                        )
                    }
            }

            Twig.debug { "Done enhancing transaction: txid: ${transactionRequest.txIdString()}" }
            true
        } catch (_: CompactBlockProcessorException.EnhanceTransactionError) {
            false
        } catch (_: EnhanceTxSetStatusError) {
            false
        } catch (_: EnhanceTxDownloadError) {
            false
        } catch (_: EnhanceTxDecryptError) {
            false
        } catch (_: Exception) {
            false
        }
    }

    @Throws(EnhanceTxSetStatusError::class)
    private suspend fun setTransactionStatus(
        transactionId: TransactionId,
        backend: TypesafeBackend,
        rawTransactionUnsafe: RawTransactionUnsafe?,
    ) = withTraceScope("CompactBlockProcessor.setTransactionStatus") {
        if (rawTransactionUnsafe == null) {
            Twig.debug {
                "Resolving TransactionDataRequest.Enhancement by setting status of " +
                    "transaction. Txid not recognized: ${transactionId.txIdString()}"
            }
        } else {
            Twig.debug {
                "Resolving TransactionDataRequest.GetStatus by setting status of " +
                    "transaction: txid: ${transactionId.txIdString()}"
            }
        }

        val status = rawTransactionUnsafe?.toTransactionStatus() ?: TransactionStatus.TxidNotRecognized
        runCatching {
            backend.setTransactionStatus(transactionId.value.byteArray, status)
        }.onFailure {
            throw EnhanceTxSetStatusError(it)
        }
    }

    @Throws(EnhanceTxDownloadError::class)
    private suspend fun getTransaction(
        transactionRequest: TransactionDataRequest.Enhanceable,
        downloader: CompactBlockDownloader,
    ): RawTransactionUnsafe? =
        withTraceScope("CompactBlockProcessor.fetchTransaction") {
            retryUpToAndThrow(TRANSACTION_FETCH_RETRIES) { failedAttempts ->
                if (failedAttempts == 0) {
                    Twig.debug { "Starting to fetch transaction: txid: ${transactionRequest.txIdString()}" }
                } else {
                    Twig.warn {
                        "Retrying to fetch transaction: txid: ${transactionRequest.txIdString()}" +
                            " after $failedAttempts failure(s)..."
                    }
                }

                when (
                    val response =
                        downloader.fetchTransaction(
                            txId = transactionRequest.txid.value.byteArray,
                            serviceMode = ServiceMode.Group("fetch-${transactionRequest.txIdString()}")
                        )
                ) {
                    is Response.Success -> response.result
                    is Response.Failure ->
                        when {
                            response is Response.Failure.Server.NotFound -> null
                            response.description.orEmpty().contains(NOT_FOUND_MESSAGE_WORKAROUND, true) -> null

                            response.description.orEmpty().contains(NOT_FOUND_MESSAGE_WORKAROUND_2, true) -> null

                            else -> throw EnhanceTxDownloadError(response.toThrowable())
                        }
                }
            }?.also {
                Twig.debug { "Transaction fetched: $it" }
            }
        }

    @Throws(EnhanceTxDecryptError::class)
    private suspend fun decryptTransaction(
        transactionId: TransactionId,
        backend: TypesafeBackend,
        rawTransactionUnsafe: RawTransactionUnsafe,
    ) = withTraceScope("CompactBlockProcessor.decryptTransaction") {
        Twig.debug {
            "Resolving TransactionDataRequest.Enhancement by decrypting and storing " +
                "transaction: txid: ${transactionId.txIdString()}"
        }

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
private const val NOT_FOUND_MESSAGE_WORKAROUND = "Transaction not found"
private const val NOT_FOUND_MESSAGE_WORKAROUND_2 =
    "No such mempool or blockchain transaction. Use gettransaction for wallet transactions."
