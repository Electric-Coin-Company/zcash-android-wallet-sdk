package cash.z.ecc.android.sdk.service

import android.content.Context
import cash.z.ecc.android.sdk.R
import cash.z.ecc.android.sdk.exception.LightwalletException
import cash.z.ecc.android.sdk.ext.ZcashSdk.DEFAULT_LIGHTWALLETD_PORT
import cash.z.ecc.android.sdk.ext.twig
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
 * @property channel the channel to use for communicating with the lightwalletd server.
 * @property singleRequestTimeoutSec the timeout to use for non-streaming requests. When a new stub is
 * created, it will use a deadline that is after the given duration from now.
 * @property streamingRequestTimeoutSec the timeout to use for streaming requests. When a new stub is
 * created for streaming requests, it will use a deadline that is after the given duration from now.
 */
class LightWalletGrpcService private constructor(
    var channel: ManagedChannel,
    private val singleRequestTimeoutSec: Long = 10L,
    private val streamingRequestTimeoutSec: Long = 90L
) : LightWalletService {

    //TODO: find a better way to do this, maybe change the constructor to keep the properties
    lateinit var connectionInfo: ConnectionInfo

    /**
     * Construct an instance that corresponds to the given host and port.
     *
     * @param appContext the application context used to check whether TLS is required by this build
     * flavor.
     * @param host the host of the server to use.
     * @param port the port of the server to use.
     * @param usePlaintext whether to use TLS or plaintext for requests. Plaintext is dangerous so
     * it requires jumping through a few more hoops.
     */
    constructor(
        appContext: Context,
        host: String,
        port: Int = DEFAULT_LIGHTWALLETD_PORT,
        usePlaintext: Boolean = appContext.resources.getBoolean(R.bool.lightwalletd_allow_very_insecure_connections)
    ) : this(createDefaultChannel(appContext, host, port, usePlaintext)) {
        connectionInfo = ConnectionInfo(appContext.applicationContext, host, port, usePlaintext)
    }

    /* LightWalletService implementation */

    override fun getBlockRange(heightRange: IntRange): List<CompactFormats.CompactBlock> {
        channel.resetConnectBackoff()
        return channel.createStub(streamingRequestTimeoutSec).getBlockRange(heightRange.toBlockRange()).toList()
    }

    override fun getLatestBlockHeight(): Int {
        channel.resetConnectBackoff()
        return channel.createStub(singleRequestTimeoutSec).getLatestBlock(Service.ChainSpec.newBuilder().build()).height.toInt()
    }

    override fun getServerInfo(): Service.LightdInfo {
        channel.resetConnectBackoff()
        return channel.createStub(singleRequestTimeoutSec).getLightdInfo(Service.Empty.newBuilder().build())
    }
    override fun submitTransaction(spendTransaction: ByteArray): Service.SendResponse {
        channel.resetConnectBackoff()
        val request = Service.RawTransaction.newBuilder().setData(ByteString.copyFrom(spendTransaction)).build()
        return channel.createStub().sendTransaction(request)
    }

    override fun shutdown() {
        channel.shutdown()
    }

    override fun fetchTransaction(txId: ByteArray): Service.RawTransaction? {
        channel.resetConnectBackoff()
        return channel.createStub().getTransaction(Service.TxFilter.newBuilder().setHash(ByteString.copyFrom(txId)).build())
    }

    override fun reconnect() {
        twig("closing existing channel and then reconnecting to" +
                " ${connectionInfo.host}:${connectionInfo.port}?usePlaintext=${connectionInfo.usePlaintext}")
        channel.shutdown()
        channel = createDefaultChannel(
            connectionInfo.appContext,
            connectionInfo.host,
            connectionInfo.port,
            connectionInfo.usePlaintext
        )
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

    inner class ConnectionInfo(
        val appContext: Context,
        val host: String,
        val port: Int,
        val usePlaintext: Boolean
    )

    companion object {
        /**
         * Convenience function for creating the default channel to be used for all connections. It
         * is important that this channel can handle transitioning from WiFi to Cellular connections
         * and is properly setup to support TLS, when required.
         */
        fun createDefaultChannel(
            appContext: Context,
            host: String,
            port: Int,
            usePlaintext: Boolean
        ): ManagedChannel {
            twig("Creating channel that will connect to $host:$port?usePlaintext=$usePlaintext")
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
