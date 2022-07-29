package cash.z.ecc.android.sdk.internal.service

import android.content.Context
import cash.z.ecc.android.sdk.annotation.OpenForTesting
import cash.z.ecc.android.sdk.internal.twig
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.LightwalletdServer
import cash.z.wallet.sdk.rpc.CompactFormats
import cash.z.wallet.sdk.rpc.CompactTxStreamerGrpc
import cash.z.wallet.sdk.rpc.Service
import com.google.protobuf.ByteString
import io.grpc.Channel
import io.grpc.ConnectivityState
import io.grpc.ManagedChannel
import io.grpc.android.AndroidChannelBuilder
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Implementation of LightwalletService using gRPC for requests to lightwalletd.
 *
 * @property channel the channel to use for communicating with the lightwalletd server.
 * @property singleRequestTimeout the timeout to use for non-streaming requests. When a new stub
 * is created, it will use a deadline that is after the given duration from now.
 * @property streamingRequestTimeout the timeout to use for streaming requests. When a new stub
 * is created for streaming requests, it will use a deadline that is after the given duration from
 * now.
 */
@OpenForTesting
class LightWalletGrpcService private constructor(
    context: Context,
    private val lightwalletdServer: LightwalletdServer,
    var channel: ManagedChannel,
    private val singleRequestTimeout: Duration = 10.seconds,
    private val streamingRequestTimeout: Duration = 90.seconds
) : LightWalletService {

    private val applicationContext = context.applicationContext

    /* LightWalletService implementation */

    override fun getBlockRange(heightRange: ClosedRange<BlockHeight>): Sequence<CompactFormats.CompactBlock> {
        if (heightRange.isEmpty()) {
            return emptySequence()
        }

        return requireChannel().createStub(streamingRequestTimeout)
            .getBlockRange(heightRange.toBlockRange()).iterator().asSequence()
    }

    override fun getLatestBlockHeight(): BlockHeight {
        return BlockHeight(
            requireChannel().createStub(singleRequestTimeout)
                .getLatestBlock(Service.ChainSpec.newBuilder().build()).height
        )
    }

    override fun getServerInfo(): Service.LightdInfo {
        return requireChannel().createStub(singleRequestTimeout)
            .getLightdInfo(Service.Empty.newBuilder().build())
    }

    override fun submitTransaction(spendTransaction: ByteArray): Service.SendResponse {
        if (spendTransaction.isEmpty()) {
            return Service.SendResponse.newBuilder().setErrorCode(3000)
                .setErrorMessage(
                    "ERROR: failed to submit transaction because it was empty" +
                        " so this request was ignored on the client-side."
                )
                .build()
        }
        val request =
            Service.RawTransaction.newBuilder().setData(ByteString.copyFrom(spendTransaction))
                .build()
        return requireChannel().createStub().sendTransaction(request)
    }

    override fun shutdown() {
        twig("Shutting down channel")
        channel.shutdown()
    }

    override fun fetchTransaction(txId: ByteArray): Service.RawTransaction? {
        if (txId.isEmpty()) return null

        return requireChannel().createStub().getTransaction(
            Service.TxFilter.newBuilder().setHash(ByteString.copyFrom(txId)).build()
        )
    }

    override fun fetchUtxos(
        tAddress: String,
        startHeight: BlockHeight
    ): List<Service.GetAddressUtxosReply> {
        val result = requireChannel().createStub().getAddressUtxos(
            Service.GetAddressUtxosArg.newBuilder().setAddress(tAddress)
                .setStartHeight(startHeight.value).build()
        )
        return result.addressUtxosList
    }

    override fun getTAddressTransactions(
        tAddress: String,
        blockHeightRange: ClosedRange<BlockHeight>
    ): List<Service.RawTransaction> {
        if (blockHeightRange.isEmpty() || tAddress.isBlank()) return listOf()

        val result = requireChannel().createStub().getTaddressTxids(
            Service.TransparentAddressBlockFilter.newBuilder().setAddress(tAddress)
                .setRange(blockHeightRange.toBlockRange()).build()
        )
        return result.toList()
    }

    override fun reconnect() {
        twig("closing existing channel and then reconnecting")
        channel.shutdown()
        channel = createDefaultChannel(applicationContext, lightwalletdServer)
    }

    // test code
    internal var stateCount = 0
    internal var state: ConnectivityState? = null
    private fun requireChannel(): ManagedChannel {
        state = channel.getState(false).let { new ->
            if (state == new) stateCount++ else stateCount = 0
            new
        }
        channel.resetConnectBackoff()
        twig(
            "getting channel isShutdown: ${channel.isShutdown}  " +
                "isTerminated: ${channel.isTerminated} " +
                "getState: $state stateCount: $stateCount",
            -1
        )
        return channel
    }

    companion object {
        fun new(context: Context, lightwalletdServer: LightwalletdServer): LightWalletGrpcService {
            val channel = createDefaultChannel(context, lightwalletdServer)

            return LightWalletGrpcService(context, lightwalletdServer, channel)
        }
    }
}

/**
 * Convenience function for creating the default channel to be used for all connections. It
 * is important that this channel can handle transitioning from WiFi to Cellular connections
 * and is properly setup to support TLS, when required.
 */
private fun createDefaultChannel(
    appContext: Context,
    lightwalletdServer: LightwalletdServer
): ManagedChannel {
    twig(
        "Creating channel that will connect to" +
            "${lightwalletdServer.host}:${lightwalletdServer.port}/?usePlaintext=${!lightwalletdServer.isSecure}"
    )
    return AndroidChannelBuilder
        .forAddress(lightwalletdServer.host, lightwalletdServer.port)
        .context(appContext)
        .enableFullStreamDecompression()
        .apply {
            if (lightwalletdServer.isSecure) {
                useTransportSecurity()
            } else {
                twig("WARNING: Using insecure channel")
                usePlaintext()
            }
        }
        .build()
}

private fun Channel.createStub(timeoutSec: Duration = 60.seconds) = CompactTxStreamerGrpc
    .newBlockingStub(this)
    .withDeadlineAfter(timeoutSec.inWholeSeconds, TimeUnit.SECONDS)

private fun BlockHeight.toBlockHeight(): Service.BlockID =
    Service.BlockID.newBuilder().setHeight(value).build()

private fun ClosedRange<BlockHeight>.toBlockRange(): Service.BlockRange =
    Service.BlockRange.newBuilder()
        .setStart(start.toBlockHeight())
        .setEnd(endInclusive.toBlockHeight())
        .build()

/**
 * This function effectively parses streaming responses. Each call to next(), on the iterators
 * returned from grpc, triggers a network call.
 */
private fun <T> Iterator<T>.toList(): List<T> =
    mutableListOf<T>().apply {
        while (hasNext()) {
            this@apply += next()
        }
    }
