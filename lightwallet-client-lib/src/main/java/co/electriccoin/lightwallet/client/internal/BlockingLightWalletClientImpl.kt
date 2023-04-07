package co.electriccoin.lightwallet.client.internal

import cash.z.wallet.sdk.internal.rpc.CompactTxStreamerGrpc
import cash.z.wallet.sdk.internal.rpc.Service
import co.electriccoin.lightwallet.client.BlockingLightWalletClient
import co.electriccoin.lightwallet.client.ext.BenchmarkingExt
import co.electriccoin.lightwallet.client.fixture.BenchmarkingBlockRangeFixture
import co.electriccoin.lightwallet.client.model.BlockHeightUnsafe
import co.electriccoin.lightwallet.client.model.CompactBlockUnsafe
import co.electriccoin.lightwallet.client.model.LightWalletEndpoint
import co.electriccoin.lightwallet.client.model.LightWalletEndpointInfoUnsafe
import co.electriccoin.lightwallet.client.model.RawTransactionUnsafe
import co.electriccoin.lightwallet.client.model.Response
import co.electriccoin.lightwallet.client.model.SendResponseUnsafe
import com.google.protobuf.ByteString
import io.grpc.Channel
import io.grpc.ConnectivityState
import io.grpc.ManagedChannel
import io.grpc.StatusException
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Implementation of BlockingLightWalletClient using gRPC for requests to lightwalletd.
 *
 * @property singleRequestTimeout the timeout to use for non-streaming requests. When a new stub
 * is created, it will use a deadline that is after the given duration from now.
 * @property streamingRequestTimeout the timeout to use for streaming requests. When a new stub
 * is created for streaming requests, it will use a deadline that is after the given duration from
 * now.
 */
@Suppress("TooManyFunctions")
internal class BlockingLightWalletClientImpl private constructor(
    private val channelFactory: ChannelFactory,
    private val lightWalletEndpoint: LightWalletEndpoint,
    private val singleRequestTimeout: Duration = 10.seconds,
    private val streamingRequestTimeout: Duration = 90.seconds
) : BlockingLightWalletClient {

    private var channel = channelFactory.newChannel(lightWalletEndpoint)

    override fun getBlockRange(heightRange: ClosedRange<BlockHeightUnsafe>): Response<Sequence<CompactBlockUnsafe>> {
        require(!heightRange.isEmpty()) {
            "${Constants.ILLEGAL_ARGUMENT_EXCEPTION_MESSAGE} range: $heightRange." // NON-NLS
        }

        return try {
            val response = requireChannel()
                .createStub(streamingRequestTimeout)
                .getBlockRange(heightRange.toBlockRange()).asSequence().map {
                    CompactBlockUnsafe.new(it)
                }

            Response.Success(response)
        } catch (e: StatusException) {
            GrpcStatusResolver.resolveFailureFromStatus(e)
        }
    }

    override fun getLatestBlockHeight(): Response<BlockHeightUnsafe> {
        return try {
            if (BenchmarkingExt.isBenchmarking()) {
                // We inject a benchmark test blocks range at this point to process only a restricted range of blocks
                // for a more reliable benchmark results.
                Response.Success(BlockHeightUnsafe(BenchmarkingBlockRangeFixture.new().endInclusive))
            } else {
                val response = requireChannel().createStub(singleRequestTimeout)
                    .getLatestBlock(Service.ChainSpec.newBuilder().build())

                val blockHeight = BlockHeightUnsafe(response.height)

                Response.Success(blockHeight)
            }
        } catch (e: StatusException) {
            GrpcStatusResolver.resolveFailureFromStatus(e)
        }
    }

    @Suppress("SwallowedException")
    override fun getServerInfo(): Response<LightWalletEndpointInfoUnsafe> {
        return try {
            val lightdInfo = requireChannel().createStub(singleRequestTimeout)
                .getLightdInfo(Service.Empty.newBuilder().build())

            val lightwalletEndpointInfo = LightWalletEndpointInfoUnsafe.new(lightdInfo)

            Response.Success(lightwalletEndpointInfo)
        } catch (e: StatusException) {
            GrpcStatusResolver.resolveFailureFromStatus(e)
        }
    }

    override fun submitTransaction(spendTransaction: ByteArray): Response<SendResponseUnsafe> {
        require(spendTransaction.isNotEmpty()) {
            "${Constants.ILLEGAL_ARGUMENT_EXCEPTION_MESSAGE} Failed to submit transaction because it was empty, so " +
                "this request was ignored on the client-side." // NON-NLS
        }
        return try {
            val request =
                Service.RawTransaction.newBuilder().setData(ByteString.copyFrom(spendTransaction))
                    .build()
            val response = requireChannel().createStub().sendTransaction(request)

            val sendResponse = SendResponseUnsafe.new(response)

            Response.Success(sendResponse)
        } catch (e: StatusException) {
            GrpcStatusResolver.resolveFailureFromStatus(e)
        }
    }

    override fun fetchTransaction(txId: ByteArray): Response<RawTransactionUnsafe> {
        require(txId.isNotEmpty()) {
            "${Constants.ILLEGAL_ARGUMENT_EXCEPTION_MESSAGE} Failed to start fetching the transaction with null " +
                "transaction ID, so this request was ignored on the client-side." // NON-NLS
        }
        return try {
            val request = Service.TxFilter.newBuilder().setHash(ByteString.copyFrom(txId)).build()

            val response = requireChannel().createStub().getTransaction(request)

            val transactionResponse = RawTransactionUnsafe.new(response)

            Response.Success(transactionResponse)
        } catch (e: StatusException) {
            GrpcStatusResolver.resolveFailureFromStatus(e)
        }
    }

    override fun fetchUtxos(
        tAddresses: List<String>,
        startHeight: BlockHeightUnsafe
    ): Sequence<Service.GetAddressUtxosReply> {
        require(tAddresses.isNotEmpty() && tAddresses.all { it.isNotBlank() }) {
            "${Constants.ILLEGAL_ARGUMENT_EXCEPTION_MESSAGE} array of addresses contains invalid item." // NON-NLS
        }

        val builder = Service.GetAddressUtxosArg.newBuilder()

        // build the request with the different addresses
        tAddresses.forEachIndexed { index, tAddress ->
            builder.setAddresses(index, tAddress)
        }

        builder.startHeight = startHeight.value

        val result = requireChannel().createStub().getAddressUtxos(
            builder.build()
        )

        return result.addressUtxosList.asSequence()
    }

    override fun getTAddressTransactions(
        tAddress: String,
        blockHeightRange: ClosedRange<BlockHeightUnsafe>
    ): Sequence<Service.RawTransaction> {
        require(!blockHeightRange.isEmpty() && tAddress.isNotBlank()) {
            "${Constants.ILLEGAL_ARGUMENT_EXCEPTION_MESSAGE} range: $blockHeightRange, address: $tAddress." // NON-NLS
        }

        return requireChannel().createStub().getTaddressTxids(
            Service.TransparentAddressBlockFilter.newBuilder().setAddress(tAddress)
                .setRange(blockHeightRange.toBlockRange()).build()
        ).iterator().asSequence()
    }

    override fun shutdown() {
        channel.shutdown()
    }

    override fun reconnect() {
        channel.shutdown()
        channel = channelFactory.newChannel(lightWalletEndpoint)
    }

    // These make the implementation of BlockingLightWalletClientImpl not thread-safe.
    private var stateCount = 0
    private var state: ConnectivityState? = null
    private fun requireChannel(): ManagedChannel {
        state = channel.getState(false).let { new ->
            if (state == new) stateCount++ else stateCount = 0
            new
        }
        channel.resetConnectBackoff()
        return channel
    }

    companion object {
        fun new(
            channelFactory: ChannelFactory,
            lightWalletEndpoint: LightWalletEndpoint
        ): BlockingLightWalletClient {
            return BlockingLightWalletClientImpl(channelFactory, lightWalletEndpoint)
        }
    }
}

private fun Channel.createStub(timeoutSec: Duration = 60.seconds) =
    CompactTxStreamerGrpc.newBlockingStub(this)
        .withDeadlineAfter(timeoutSec.inWholeSeconds, TimeUnit.SECONDS)

private fun BlockHeightUnsafe.toBlockHeight(): Service.BlockID =
    Service.BlockID.newBuilder().setHeight(value).build()

private fun ClosedRange<BlockHeightUnsafe>.toBlockRange(): Service.BlockRange =
    Service.BlockRange.newBuilder()
        .setStart(start.toBlockHeight())
        .setEnd(endInclusive.toBlockHeight())
        .build()
