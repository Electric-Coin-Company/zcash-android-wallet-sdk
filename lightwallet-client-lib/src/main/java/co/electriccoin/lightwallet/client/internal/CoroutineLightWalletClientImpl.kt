package co.electriccoin.lightwallet.client.internal

import cash.z.wallet.sdk.internal.rpc.CompactFormats
import cash.z.wallet.sdk.internal.rpc.CompactTxStreamerGrpcKt
import cash.z.wallet.sdk.internal.rpc.Service
import co.electriccoin.lightwallet.client.BlockingLightWalletClient
import co.electriccoin.lightwallet.client.CoroutineLightWalletClient
import co.electriccoin.lightwallet.client.model.BlockHeightUnsafe
import co.electriccoin.lightwallet.client.model.LightWalletEndpoint
import co.electriccoin.lightwallet.client.model.LightWalletEndpointInfoUnsafe
import co.electriccoin.lightwallet.client.model.Response
import com.google.protobuf.ByteString
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ConnectivityState
import io.grpc.ManagedChannel
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
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
internal class CoroutineLightWalletClientImpl private constructor(
    private val channelFactory: ChannelFactory,
    private val lightWalletEndpoint: LightWalletEndpoint,
    private val singleRequestTimeout: Duration = 10.seconds,
    private val streamingRequestTimeout: Duration = 90.seconds
) : CoroutineLightWalletClient {

    private var channel = channelFactory.newChannel(lightWalletEndpoint)

    /* LightWalletService implementation */

    override fun getBlockRange(heightRange: ClosedRange<BlockHeightUnsafe>): Flow<CompactFormats.CompactBlock> {
        if (heightRange.isEmpty()) {
            return emptyFlow()
        }

        return requireChannel().createStub(streamingRequestTimeout)
            .getBlockRange(heightRange.toBlockRange())
    }

    override suspend fun getLatestBlockHeight(): BlockHeightUnsafe {
        return BlockHeightUnsafe(
            requireChannel().createStub(singleRequestTimeout)
                .getLatestBlock(Service.ChainSpec.newBuilder().build()).height
        )
    }

    @Suppress("SwallowedException")
    override suspend fun getServerInfo(): Response<LightWalletEndpointInfoUnsafe> {
        return try {
            val lightdInfo = requireChannel().createStub(singleRequestTimeout)
                .getLightdInfo(Service.Empty.newBuilder().build())

            val lightwalletEndpointInfo = LightWalletEndpointInfoUnsafe.new(lightdInfo)

            Response.Success(lightwalletEndpointInfo)
        } catch (e: StatusRuntimeException) {
            Response.Failure.Server()
        }
    }

    override suspend fun submitTransaction(spendTransaction: ByteArray): Service.SendResponse {
        if (spendTransaction.isEmpty()) {
            return Service.SendResponse.newBuilder().setErrorCode(BlockingLightWalletClient.DEFAULT_ERROR_CODE)
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
        channel.shutdown()
    }

    override suspend fun fetchTransaction(txId: ByteArray): Service.RawTransaction? {
        if (txId.isEmpty()) return null

        return requireChannel().createStub().getTransaction(
            Service.TxFilter.newBuilder().setHash(ByteString.copyFrom(txId)).build()
        )
    }

    override suspend fun fetchUtxos(
        tAddress: String,
        startHeight: BlockHeightUnsafe
    ): List<Service.GetAddressUtxosReply> {
        val result = requireChannel().createStub().getAddressUtxos(
            Service.GetAddressUtxosArg.newBuilder().setAddress(tAddress)
                .setStartHeight(startHeight.value).build()
        )
        return result.addressUtxosList
    }

    override fun getTAddressTransactions(
        tAddress: String,
        blockHeightRange: ClosedRange<BlockHeightUnsafe>
    ): Flow<Service.RawTransaction> {
        if (blockHeightRange.isEmpty() || tAddress.isBlank()) return emptyFlow()

        return requireChannel().createStub().getTaddressTxids(
            Service.TransparentAddressBlockFilter.newBuilder().setAddress(tAddress)
                .setRange(blockHeightRange.toBlockRange()).build()
        )
    }

    override fun reconnect() {
        channel.shutdown()
        channel = channelFactory.newChannel(lightWalletEndpoint)
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
        // twig(
        //     "getting channel isShutdown: ${channel.isShutdown}  " +
        //         "isTerminated: ${channel.isTerminated} " +
        //         "getState: $state stateCount: $stateCount",
        //     -1
        // )
        return channel
    }

    companion object {
        fun new(
            channelFactory: ChannelFactory,
            lightWalletEndpoint: LightWalletEndpoint
        ): CoroutineLightWalletClientImpl {
            return CoroutineLightWalletClientImpl(channelFactory, lightWalletEndpoint)
        }
    }
}

private fun Channel.createStub(timeoutSec: Duration = 60.seconds) =
    CompactTxStreamerGrpcKt.CompactTxStreamerCoroutineStub(this, CallOptions.DEFAULT)
        .withDeadlineAfter(timeoutSec.inWholeSeconds, TimeUnit.SECONDS)

private fun BlockHeightUnsafe.toBlockHeight(): Service.BlockID =
    Service.BlockID.newBuilder().setHeight(value).build()

private fun ClosedRange<BlockHeightUnsafe>.toBlockRange(): Service.BlockRange =
    Service.BlockRange.newBuilder()
        .setStart(start.toBlockHeight())
        .setEnd(endInclusive.toBlockHeight())
        .build()
