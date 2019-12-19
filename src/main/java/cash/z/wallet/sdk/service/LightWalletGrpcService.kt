package cash.z.wallet.sdk.service

import android.content.Context
import cash.z.wallet.sdk.R
import cash.z.wallet.sdk.exception.LightwalletException
import cash.z.wallet.sdk.ext.ZcashSdk.DEFAULT_LIGHTWALLETD_PORT
import cash.z.wallet.sdk.ext.twig
import cash.z.wallet.sdk.rpc.CompactFormats
import cash.z.wallet.sdk.rpc.CompactTxStreamerGrpc
import cash.z.wallet.sdk.rpc.Service
import com.google.protobuf.ByteString
import io.grpc.Channel
import io.grpc.ManagedChannel
import io.grpc.android.AndroidChannelBuilder
import java.util.concurrent.TimeUnit

/**
 * Implementation of LightwalletService using gRPC for requests to lightwalletd.
 * 
 * @param channel the channel to use for communicating with the lightwalletd server.
 * @param singleRequestTimeoutSec the timeout to use for non-streaming requests. When a new stub is
 * created, it will use a deadline that is after the given duration from now.
 * @param streamingRequestTimeoutSec the timeout to use for streaming requests. When a new stub is
 * created for streaming requests, it will use a deadline that is after the given duration from now.
 */
class LightWalletGrpcService private constructor(
    private val channel: ManagedChannel,
    private val singleRequestTimeoutSec: Long = 10L,
    private val streamingRequestTimeoutSec: Long = 90L
) : LightWalletService {

    constructor(
        appContext: Context,
        host: String,
        port: Int = DEFAULT_LIGHTWALLETD_PORT,
        usePlaintext: Boolean = !appContext.resources.getBoolean(R.bool.is_mainnet)
    ) : this(createDefaultChannel(appContext, host, port, usePlaintext))

    /* LightWalletService implementation */

    /**
     * Blocking call to download all blocks in the given range.
     *
     * @param heightRange the inclusive range of block heights to download.
     * @return a list of compact blocks for the given range
     */
    override fun getBlockRange(heightRange: IntRange): List<CompactFormats.CompactBlock> {
        channel.resetConnectBackoff()
        return channel.createStub(streamingRequestTimeoutSec).getBlockRange(heightRange.toBlockRange()).toList()
    }

    override fun getLatestBlockHeight(): Int {
        channel.resetConnectBackoff()
        return channel.createStub(singleRequestTimeoutSec).getLatestBlock(Service.ChainSpec.newBuilder().build()).height.toInt()
    }

    override fun submitTransaction(spendTransaction: ByteArray): Service.SendResponse {
        channel.resetConnectBackoff()
        val request = Service.RawTransaction.newBuilder().setData(ByteString.copyFrom(spendTransaction)).build()
        return channel.createStub().sendTransaction(request)
    }

    override fun shutdown() {
        channel.shutdownNow()
    }

    //
    // Utilities
    //

    private fun Channel.createStub(timeoutSec: Long = 60L): CompactTxStreamerGrpc.CompactTxStreamerBlockingStub =
        CompactTxStreamerGrpc
            .newBlockingStub(this)
            .withDeadlineAfter(timeoutSec, TimeUnit.SECONDS)

    private inline fun Int.toBlockHeight(): Service.BlockID = Service.BlockID.newBuilder().setHeight(this.toLong()).build()

    private inline fun IntRange.toBlockRange(): Service.BlockRange =
        Service.BlockRange.newBuilder()
            .setStart(first.toBlockHeight())
            .setEnd(last.toBlockHeight())
            .build()

    private fun Iterator<CompactFormats.CompactBlock>.toList(): List<CompactFormats.CompactBlock> =
        mutableListOf<CompactFormats.CompactBlock>().apply {
            while (hasNext()) {
                this@apply += next()
            }
        }

    companion object {
        fun createDefaultChannel(
            appContext: Context,
            host: String,
            port: Int,
            usePlaintext: Boolean
        ): ManagedChannel {
            twig("Creating connection to $host:$port")
            return AndroidChannelBuilder
                .forAddress(host, port)
                .context(appContext)
                .apply {
                    if (usePlaintext) {
                        if (!appContext.resources.getBoolean(R.bool.lightwalletd_allow_very_insecure_connections)) throw LightwalletException.InsecureConnection
                        usePlaintext()
                    } else {
                        useTransportSecurity()
                    }
                }
                .build()
        }
    }
}