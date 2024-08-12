package cash.z.ecc.android.sdk.internal

import android.content.Context
import cash.z.ecc.android.sdk.internal.model.ext.toBlockHeight
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.FastestServersResult
import cash.z.ecc.android.sdk.model.ZcashNetwork
import co.electriccoin.lightwallet.client.LightWalletClient
import co.electriccoin.lightwallet.client.model.BlockHeightUnsafe
import co.electriccoin.lightwallet.client.model.LightWalletEndpoint
import co.electriccoin.lightwallet.client.model.LightWalletEndpointInfoUnsafe
import co.electriccoin.lightwallet.client.model.Response
import co.electriccoin.lightwallet.client.new
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

internal class FastestServerFetcher(
    private val backend: TypesafeBackend,
    private val network: ZcashNetwork
) {
    suspend operator fun invoke(
        context: Context,
        servers: List<LightWalletEndpoint>,
    ): Flow<FastestServersResult> {
        return flow {
            emit(FastestServersResult.Measuring)

            val serversByRpcMeanLatency =
                servers
                    .parallelMapNotNull {
                        validateServerEndpointAndMeasure(context, it)
                    }
                    .sortedBy {
                        it.meanDuration
                    }
                    .mapIndexedNotNull { index, result ->
                        if (index <= K - 1 || result.meanDuration <= LATENCY_THRESHOLD) {
                            Twig.debug { "Fastest Server: '${result.endpoint}' VALIDATED by SORTING by RPC latency" }
                            result
                        } else {
                            Twig.debug { "Fastest Server: '${result.endpoint}' RULED OUT by SORTING by RPC latency" }
                            null
                        }
                    }

            Twig.debug {
                "Fastest Server: '${serversByRpcMeanLatency.map { it.endpoint }}' VALIDATED by MEASURING RPC latency"
            }

            emit(FastestServersResult.Validating(serversByRpcMeanLatency.map { it.endpoint }.take(K)))

            val serversByGetBlockRangeTimeout =
                serversByRpcMeanLatency
                    .asFlow()
                    .mapNotNull { result ->
                        val didTimeOut =
                            withTimeoutOrNull(FETCH_THRESHOLD) {
                                runCatching {
                                    val to = result.remoteInfo.blockHeightUnsafe
                                    val from = BlockHeightUnsafe((to.value - N).coerceAtLeast(0))
                                    result.lightWalletClient.getBlockRange(from..to)
                                }.getOrNull()
                            } == null

                        if (didTimeOut) {
                            Twig.debug { "Fastest Server: '${result.endpoint}' RULED OUT by getBlockRange timeout" }
                            null
                        } else {
                            Twig.debug { "Fastest Server: '${result.endpoint}' VALIDATED by getBlockRange timeout" }
                            result
                        }
                    }
                    .map { it.endpoint }
                    .take(K)
                    .toList()

            Twig.debug { "Fastest Server: '$serversByGetBlockRangeTimeout' VALIDATED by getBlockRange timeout" }

            emit(FastestServersResult.Done(serversByGetBlockRangeTimeout))
        }.flowOn(Dispatchers.Default)
    }

    @Suppress("LongMethod", "ReturnCount")
    private suspend fun validateServerEndpointAndMeasure(
        context: Context,
        endpoint: LightWalletEndpoint
    ): ValidateServerResult? {
        fun logRuledOut(
            reason: String,
            throwable: Throwable? = null
        ) {
            val message =
                "Fastest Server: Server '$endpoint' RULED OUT during validating and measuring RPC " +
                    "latency. Reason: $reason"

            if (throwable != null) {
                Twig.debug(throwable) { message }
            } else {
                Twig.debug { message }
            }
        }

        val lightWalletClient =
            LightWalletClient.new(
                context = context,
                lightWalletEndpoint = endpoint,
                singleRequestTimeout = 5.seconds
            )

        val remoteInfo: LightWalletEndpointInfoUnsafe
        val getServerInfoDuration =
            measureTime {
                remoteInfo =
                    when (val response = lightWalletClient.getServerInfo()) {
                        is Response.Success -> response.result
                        is Response.Failure -> {
                            logRuledOut("getServerInfo failed", response.toThrowable())
                            return null
                        }
                    }
            }

        // Check network type
        if (!remoteInfo.matchingNetwork(network.networkName)) {
            logRuledOut("matchingNetwork failed")
            return null
        }

        // Check sapling activation height
        runCatching {
            val remoteSaplingActivationHeight = remoteInfo.saplingActivationHeightUnsafe.toBlockHeight(network)
            if (network.saplingActivationHeight != remoteSaplingActivationHeight) {
                logRuledOut("invalid saplingActivationHeight")
                return null
            }
        }.getOrElse {
            logRuledOut("saplingActivationHeight failed", it)
            return null
        }

        val currentChainTip: BlockHeight
        val getLatestBlockHeightDuration =
            measureTime {
                currentChainTip =
                    when (val response = lightWalletClient.getLatestBlockHeight()) {
                        is Response.Success -> {
                            runCatching { response.result.toBlockHeight(network) }.getOrElse {
                                logRuledOut("toBlockHeight failed", it)
                                return null
                            }
                        }

                        is Response.Failure -> {
                            logRuledOut("getLatestBlockHeight failed", response.toThrowable())
                            return null
                        }
                    }
            }

        val sdkBranchId =
            runCatching {
                "%x".format(
                    Locale.ROOT,
                    backend.getBranchIdForHeight(currentChainTip)
                )
            }.getOrElse {
                logRuledOut("getBranchIdForHeight failed", it)
                return null
            }

        if (!remoteInfo.consensusBranchId.equals(sdkBranchId, true)) {
            logRuledOut("consensusBranchId does not match")
            return null
        }

        if (remoteInfo.estimatedHeight >= remoteInfo.blockHeightUnsafe.value + N) {
            logRuledOut("estimatedHeight does not match")
            return null
        }

        Twig.debug { "Fastest Server: Server '$endpoint' VALIDATED during validating and measuring RPC latency" }

        return ValidateServerResult(
            remoteInfo = remoteInfo,
            lightWalletClient = lightWalletClient,
            endpoint = endpoint,
            getServerInfoDuration = getServerInfoDuration,
            getLatestBlockHeightDuration = getLatestBlockHeightDuration
        )
    }

    private suspend inline fun <T, R> Iterable<T>.parallelMapNotNull(crossinline block: suspend (T) -> R?): List<R> =
        map { coroutineScope { async { block(it) } } }
            .awaitAll()
            .filterNotNull()
}

data class ValidateServerResult(
    val remoteInfo: LightWalletEndpointInfoUnsafe,
    val lightWalletClient: LightWalletClient,
    val endpoint: LightWalletEndpoint,
    val getServerInfoDuration: Duration,
    val getLatestBlockHeightDuration: Duration,
) {
    val meanDuration = (getServerInfoDuration + getLatestBlockHeightDuration) / 2
}

/**
 * Amount of fastest servers to return.
 */
private const val K = 3

/**
 * Latest N amount of blocks.
 */
private const val N = 100

/**
 * Threshold for mean RPC call latency.
 */
private val LATENCY_THRESHOLD = 300.milliseconds

/**
 * Threshold for getBlockRange RPC call latency of latest [N] blocks.
 */
private val FETCH_THRESHOLD = 60.seconds
