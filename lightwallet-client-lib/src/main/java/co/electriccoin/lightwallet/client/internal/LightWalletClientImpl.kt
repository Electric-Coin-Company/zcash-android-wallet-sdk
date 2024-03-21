package co.electriccoin.lightwallet.client.internal

import cash.z.wallet.sdk.internal.rpc.CompactTxStreamerGrpcKt
import cash.z.wallet.sdk.internal.rpc.Service
import co.electriccoin.lightwallet.client.LightWalletClient
import co.electriccoin.lightwallet.client.ext.BenchmarkingExt
import co.electriccoin.lightwallet.client.fixture.BenchmarkingBlockRangeFixture
import co.electriccoin.lightwallet.client.model.BlockHeightUnsafe
import co.electriccoin.lightwallet.client.model.CompactBlockUnsafe
import co.electriccoin.lightwallet.client.model.GetAddressUtxosReplyUnsafe
import co.electriccoin.lightwallet.client.model.LightWalletEndpoint
import co.electriccoin.lightwallet.client.model.LightWalletEndpointInfoUnsafe
import co.electriccoin.lightwallet.client.model.RawTransactionUnsafe
import co.electriccoin.lightwallet.client.model.Response
import co.electriccoin.lightwallet.client.model.SendResponseUnsafe
import co.electriccoin.lightwallet.client.model.ShieldedProtocolEnum
import co.electriccoin.lightwallet.client.model.SubtreeRootUnsafe
import co.electriccoin.lightwallet.client.model.TreeStateUnsafe
import com.google.protobuf.ByteString
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ConnectivityState
import io.grpc.ManagedChannel
import io.grpc.StatusException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Implementation of LightWalletClient using gRPC for requests to lightwalletd.
 *
 * @property singleRequestTimeout the timeout to use for non-streaming requests. When a new stub
 * is created, it will use a deadline that is after the given duration from now.
 * @property streamingRequestTimeout the timeout to use for streaming requests. When a new stub
 * is created for streaming requests, it will use a deadline that is after the given duration from
 * now.
 */
@Suppress("TooManyFunctions")
internal class LightWalletClientImpl private constructor(
    private val channelFactory: ChannelFactory,
    private val lightWalletEndpoint: LightWalletEndpoint,
    private val singleRequestTimeout: Duration,
    private val streamingRequestTimeout: Duration
) : LightWalletClient {
    private var channel = channelFactory.newChannel(lightWalletEndpoint)

    override fun getBlockRange(heightRange: ClosedRange<BlockHeightUnsafe>): Flow<Response<CompactBlockUnsafe>> {
        require(!heightRange.isEmpty()) {
            "${Constants.ILLEGAL_ARGUMENT_EXCEPTION_MESSAGE_EMPTY} range: $heightRange." // NON-NLS
        }

        return try {
            requireChannel().createStub(streamingRequestTimeout)
                .getBlockRange(heightRange.toBlockRange())
                .map {
                    val response: Response<CompactBlockUnsafe> = Response.Success(CompactBlockUnsafe.new(it))
                    response
                }.catch {
                    val failure: Response.Failure<CompactBlockUnsafe> = GrpcStatusResolver.resolveFailureFromStatus(it)
                    emit(failure)
                }
        } catch (e: StatusException) {
            flowOf(GrpcStatusResolver.resolveFailureFromStatus(e))
        }
    }

    override suspend fun getLatestBlockHeight(): Response<BlockHeightUnsafe> {
        return try {
            if (BenchmarkingExt.isBenchmarking()) {
                // We inject a benchmark test blocks range at this point to process only a restricted range of blocks
                // for a more reliable benchmark results.
                Response.Success(BlockHeightUnsafe(BenchmarkingBlockRangeFixture.new().endInclusive))
            } else {
                val response =
                    requireChannel().createStub(singleRequestTimeout)
                        .getLatestBlock(Service.ChainSpec.newBuilder().build())

                val blockHeight = BlockHeightUnsafe(response.height)

                Response.Success(blockHeight)
            }
        } catch (e: StatusException) {
            GrpcStatusResolver.resolveFailureFromStatus(e)
        }
    }

    override suspend fun getServerInfo(): Response<LightWalletEndpointInfoUnsafe> {
        return try {
            val lightdInfo =
                requireChannel().createStub(singleRequestTimeout)
                    .getLightdInfo(Service.Empty.newBuilder().build())

            val lightwalletEndpointInfo = LightWalletEndpointInfoUnsafe.new(lightdInfo)

            Response.Success(lightwalletEndpointInfo)
        } catch (e: StatusException) {
            GrpcStatusResolver.resolveFailureFromStatus(e)
        }
    }

    override suspend fun submitTransaction(spendTransaction: ByteArray): Response<SendResponseUnsafe> {
        require(spendTransaction.isNotEmpty()) {
            "${Constants.ILLEGAL_ARGUMENT_EXCEPTION_MESSAGE_EMPTY} Failed to submit transaction because it was empty," +
                " so this request was ignored on the client-side." // NON-NLS
        }

        val request =
            Service.RawTransaction.newBuilder()
                .setData(ByteString.copyFrom(spendTransaction))
                .build()

        return try {
            val response = requireChannel().createStub().sendTransaction(request)

            val sendResponse = SendResponseUnsafe.new(response)

            Response.Success(sendResponse)
        } catch (e: StatusException) {
            GrpcStatusResolver.resolveFailureFromStatus(e)
        }
    }

    override suspend fun fetchTransaction(txId: ByteArray): Response<RawTransactionUnsafe> {
        require(txId.isNotEmpty()) {
            "${Constants.ILLEGAL_ARGUMENT_EXCEPTION_MESSAGE_EMPTY} Failed to start fetching the transaction with" +
                " null transaction ID, so this request was ignored on the client-side." // NON-NLS
        }

        val request = Service.TxFilter.newBuilder().setHash(ByteString.copyFrom(txId)).build()

        return try {
            val response = requireChannel().createStub().getTransaction(request)

            val transactionResponse = RawTransactionUnsafe.new(response)

            Response.Success(transactionResponse)
        } catch (e: StatusException) {
            GrpcStatusResolver.resolveFailureFromStatus(e)
        }
    }

    override suspend fun fetchUtxos(
        tAddresses: List<String>,
        startHeight: BlockHeightUnsafe
    ): Flow<Response<GetAddressUtxosReplyUnsafe>> {
        require(tAddresses.isNotEmpty() && tAddresses.all { it.isNotBlank() }) {
            "${Constants.ILLEGAL_ARGUMENT_EXCEPTION_MESSAGE_EMPTY} array of addresses contains invalid item." // NON-NLS
        }

        val getUtxosBuilder = Service.GetAddressUtxosArg.newBuilder()

        // Build the request with the different addresses
        getUtxosBuilder.addAllAddresses(tAddresses)

        val request = getUtxosBuilder.build()

        return try {
            requireChannel().createStub(streamingRequestTimeout)
                .getAddressUtxosStream(request)
                .map {
                    val response: Response<GetAddressUtxosReplyUnsafe> =
                        Response.Success(GetAddressUtxosReplyUnsafe.new(it))
                    response
                }.catch {
                    val failure: Response.Failure<GetAddressUtxosReplyUnsafe> =
                        GrpcStatusResolver.resolveFailureFromStatus(it)
                    emit(failure)
                }
        } catch (e: StatusException) {
            flowOf(GrpcStatusResolver.resolveFailureFromStatus(e))
        }
    }

    override fun getTAddressTransactions(
        tAddress: String,
        blockHeightRange: ClosedRange<BlockHeightUnsafe>
    ): Flow<Response<RawTransactionUnsafe>> {
        require(!blockHeightRange.isEmpty() && tAddress.isNotBlank()) {
            "${Constants.ILLEGAL_ARGUMENT_EXCEPTION_MESSAGE_EMPTY} range: $blockHeightRange, address: " +
                "$tAddress." // NON-NLS
        }

        val request =
            Service.TransparentAddressBlockFilter.newBuilder()
                .setAddress(tAddress)
                .setRange(blockHeightRange.toBlockRange())
                .build()

        return try {
            requireChannel().createStub(streamingRequestTimeout)
                .getTaddressTxids(request)
                .map {
                    val response: Response<RawTransactionUnsafe> = Response.Success(RawTransactionUnsafe.new(it))
                    response
                }.catch {
                    val failure: Response.Failure<RawTransactionUnsafe> =
                        GrpcStatusResolver.resolveFailureFromStatus(it)
                    emit(failure)
                }
        } catch (e: StatusException) {
            flowOf(GrpcStatusResolver.resolveFailureFromStatus(e))
        }
    }

    override fun getSubtreeRoots(
        startIndex: UInt,
        shieldedProtocol: ShieldedProtocolEnum,
        maxEntries: UInt
    ): Flow<Response<SubtreeRootUnsafe>> {
        val getSubtreeRootsArgBuilder = Service.GetSubtreeRootsArg.newBuilder()
        getSubtreeRootsArgBuilder.startIndex = startIndex.toInt()
        getSubtreeRootsArgBuilder.shieldedProtocol = shieldedProtocol.toProtocol()
        getSubtreeRootsArgBuilder.maxEntries = maxEntries.toInt()

        val request = getSubtreeRootsArgBuilder.build()

        return try {
            requireChannel().createStub(streamingRequestTimeout)
                .getSubtreeRoots(request)
                .map {
                    val response: Response<SubtreeRootUnsafe> = Response.Success(SubtreeRootUnsafe.new(it))
                    response
                }.catch {
                    val failure: Response.Failure<SubtreeRootUnsafe> = GrpcStatusResolver.resolveFailureFromStatus(it)
                    emit(failure)
                }
        } catch (e: StatusException) {
            flowOf(GrpcStatusResolver.resolveFailureFromStatus(e))
        }
    }

    override suspend fun getTreeState(height: BlockHeightUnsafe): Response<TreeStateUnsafe> {
        return try {
            val response =
                requireChannel().createStub(singleRequestTimeout)
                    .getTreeState(height.toBlockHeight())

            Response.Success(TreeStateUnsafe.new(response))
        } catch (e: StatusException) {
            GrpcStatusResolver.resolveFailureFromStatus(e)
        }
    }

    override fun shutdown() {
        channel.shutdown()
    }

    override fun reconnect() {
        channel.shutdown()
        channel = channelFactory.newChannel(lightWalletEndpoint)
    }

    // These make the LightWalletClientImpl not thread safe. In the long-term, we should
    // consider making it thread safe.
    private var stateCount = 0
    private var state: ConnectivityState? = null

    private fun requireChannel(): ManagedChannel {
        state =
            channel.getState(false).let { new ->
                if (state == new) stateCount++ else stateCount = 0
                new
            }
        channel.resetConnectBackoff()
        return channel
    }

    companion object {
        fun new(
            channelFactory: ChannelFactory,
            lightWalletEndpoint: LightWalletEndpoint,
            singleRequestTimeout: Duration = 10.seconds,
            streamingRequestTimeout: Duration = 90.seconds
        ): LightWalletClientImpl {
            return LightWalletClientImpl(
                channelFactory = channelFactory,
                lightWalletEndpoint = lightWalletEndpoint,
                singleRequestTimeout = singleRequestTimeout,
                streamingRequestTimeout = streamingRequestTimeout
            )
        }
    }
}

private fun Channel.createStub(timeoutSec: Duration = 60.seconds) =
    CompactTxStreamerGrpcKt.CompactTxStreamerCoroutineStub(this, CallOptions.DEFAULT)
        .withDeadlineAfter(timeoutSec.inWholeSeconds, TimeUnit.SECONDS)

private fun BlockHeightUnsafe.toBlockHeight(): Service.BlockID = Service.BlockID.newBuilder().setHeight(value).build()

private fun ClosedRange<BlockHeightUnsafe>.toBlockRange(): Service.BlockRange =
    Service.BlockRange.newBuilder()
        .setStart(start.toBlockHeight())
        .setEnd(endInclusive.toBlockHeight())
        .build()
