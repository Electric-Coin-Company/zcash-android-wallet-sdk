package cash.z.ecc.android.sdk.service

import android.content.Context
import cash.z.ecc.android.sdk.R
import cash.z.ecc.android.sdk.annotation.OpenForTesting
import cash.z.ecc.android.sdk.exception.LightWalletException
import cash.z.ecc.android.sdk.ext.twig
import cash.z.ecc.android.sdk.type.ZcashNetwork
import cash.z.wallet.sdk.rpc.CompactFormats
import cash.z.wallet.sdk.rpc.CompactTxStreamerGrpc
import cash.z.wallet.sdk.rpc.Service
import com.google.protobuf.ByteString
import io.grpc.Channel
import io.grpc.ConnectivityState
import io.grpc.ManagedChannel
import io.grpc.android.AndroidChannelBuilder
import java.util.concurrent.TimeUnit

/**
 * Implementation of LightwalletService using gRPC for requests to lightwalletd.
 *
 * @property channel the channel to use for communicating with the lightwalletd server.
 * @property singleRequestTimeoutSec the timeout to use for non-streaming requests. When a new stub
 * is created, it will use a deadline that is after the given duration from now.
 * @property streamingRequestTimeoutSec the timeout to use for streaming requests. When a new stub
 * is created for streaming requests, it will use a deadline that is after the given duration from
 * now.
 */
@OpenForTesting
class LightWalletGrpcService private constructor(
    var channel: ManagedChannel,
    private val singleRequestTimeoutSec: Long = 10L,
    private val streamingRequestTimeoutSec: Long = 90L
) : LightWalletService {

    lateinit var connectionInfo: ConnectionInfo

    constructor(
        appContext: Context,
        network: ZcashNetwork,
        usePlaintext: Boolean =
            appContext.resources.getBoolean(R.bool.lightwalletd_allow_very_insecure_connections)
    ) : this(appContext, network.defaultHost, network.defaultPort, usePlaintext)

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
        port: Int = ZcashNetwork.Mainnet.defaultPort,
        usePlaintext: Boolean =
            appContext.resources.getBoolean(R.bool.lightwalletd_allow_very_insecure_connections)
    ) : this(createDefaultChannel(appContext, host, port, usePlaintext)) {
        connectionInfo = ConnectionInfo(appContext.applicationContext, host, port, usePlaintext)
    }

    /* LightWalletService implementation */

    override fun getBlockRange(heightRange: IntRange): List<CompactFormats.CompactBlock> {
        if (heightRange.isEmpty()) return listOf()

        return requireChannel().createStub(streamingRequestTimeoutSec)
            .getBlockRange(heightRange.toBlockRange()).toList()
    }

    override fun getLatestBlockHeight(): Int {
        return requireChannel().createStub(singleRequestTimeoutSec)
            .getLatestBlock(Service.ChainSpec.newBuilder().build()).height.toInt()
    }

    override fun getServerInfo(): Service.LightdInfo {
        return requireChannel().createStub(singleRequestTimeoutSec)
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
        startHeight: Int
    ): List<Service.GetAddressUtxosReply> {
        val result = requireChannel().createStub().getAddressUtxos(
            Service.GetAddressUtxosArg.newBuilder().setAddress(tAddress)
                .setStartHeight(startHeight.toLong()).build()
        )
        return result.addressUtxosList
    }

    override fun getTAddressTransactions(
        tAddress: String,
        blockHeightRange: IntRange
    ): List<Service.RawTransaction> {
        if (blockHeightRange.isEmpty() || tAddress.isBlank()) return listOf()

        val result = requireChannel().createStub().getTaddressTxids(
            Service.TransparentAddressBlockFilter.newBuilder().setAddress(tAddress)
                .setRange(blockHeightRange.toBlockRange()).build()
        )
        return result.toList()
    }

    override fun reconnect() {
        twig(
            "closing existing channel and then reconnecting to ${connectionInfo.host}:" +
                "${connectionInfo.port}?usePlaintext=${connectionInfo.usePlaintext}"
        )
        channel.shutdown()
        channel = createDefaultChannel(
            connectionInfo.appContext,
            connectionInfo.host,
            connectionInfo.port,
            connectionInfo.usePlaintext
        )
    }

    // test code
    var stateCount = 0
    var state: ConnectivityState? = null
    private fun requireChannel(): ManagedChannel {
        state = channel.getState(false).let { new ->
            if (state == new) stateCount++ else stateCount = 0
            new
        }
        channel.resetConnectBackoff()
        twig("getting channel isShutdown: ${channel.isShutdown}  isTerminated: ${channel.isTerminated} getState: $state stateCount: $stateCount", -1)
        return channel
    }

    //
    // Utilities
    //

    private fun Channel.createStub(timeoutSec: Long = 60L) = CompactTxStreamerGrpc
        .newBlockingStub(this)
        .withDeadlineAfter(timeoutSec, TimeUnit.SECONDS)

    private inline fun Int.toBlockHeight(): Service.BlockID =
        Service.BlockID.newBuilder().setHeight(this.toLong()).build()

    private inline fun IntRange.toBlockRange(): Service.BlockRange =
        Service.BlockRange.newBuilder()
            .setStart(first.toBlockHeight())
            .setEnd(last.toBlockHeight())
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

    inner class ConnectionInfo(
        val appContext: Context,
        val host: String,
        val port: Int,
        val usePlaintext: Boolean
    ) {
        override fun toString(): String {
            return "$host:$port?usePlaintext=$usePlaintext"
        }
    }

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
                .enableFullStreamDecompression()
                .apply {
                    if (usePlaintext) {
                        if (!appContext.resources.getBoolean(
                                R.bool.lightwalletd_allow_very_insecure_connections
                            )
                        ) throw LightWalletException.InsecureConnection
                        usePlaintext()
                    } else {
                        useTransportSecurity()
                    }
                }
                .build()
        }
    }
}
