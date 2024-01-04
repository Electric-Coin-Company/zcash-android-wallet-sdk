package co.electriccoin.lightwallet.client.internal

import android.content.Context
import cash.z.wallet.sdk.internal.rpc.Darkside
import cash.z.wallet.sdk.internal.rpc.Darkside.DarksideTransactionsURL
import cash.z.wallet.sdk.internal.rpc.DarksideStreamerGrpc
import cash.z.wallet.sdk.internal.rpc.Service
import co.electriccoin.lightwallet.client.model.BlockHeightUnsafe
import co.electriccoin.lightwallet.client.model.LightWalletEndpoint
import io.grpc.ManagedChannel
import io.grpc.stub.StreamObserver
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/*
 * This class is under the internal package, but is itself not restricted with internal visibility.
 *
 * This allows the class to be used for some automated tests in other modules.
 */
class DarksideApi private constructor(
    private val channel: ManagedChannel,
    private val singleRequestTimeout: Duration = 10.seconds
) {
    companion object {
        internal fun new(
            channelFactory: ChannelFactory,
            lightWalletEndpoint: LightWalletEndpoint
        ) = DarksideApi(channelFactory.newChannel(lightWalletEndpoint))
    }

    //
    // Service APIs
    //

    fun reset(
        saplingActivationHeight: BlockHeightUnsafe,
        branchId: String = "e9ff75a6", // Canopy,
        chainName: String = "darksidemainnet"
    ) = apply {
        Darkside.DarksideMetaState.newBuilder()
            .setBranchID(branchId)
            .setChainName(chainName)
            .setSaplingActivation(saplingActivationHeight.value.toInt())
            .build().let { request ->
                createStub().reset(request)
            }
    }

    fun stageBlocks(url: String) =
        apply {
            createStub().stageBlocks(url.toUrl())
        }

    fun stageTransactions(
        url: String,
        targetHeight: BlockHeightUnsafe
    ) = apply {
        createStub().stageTransactions(
            DarksideTransactionsURL.newBuilder().setHeight(targetHeight.value.toInt()).setUrl(url).build()
        )
    }

    fun stageEmptyBlocks(
        startHeight: BlockHeightUnsafe,
        count: Int = 10,
        nonce: Int = Random.nextInt()
    ) = apply {
        createStub().stageBlocksCreate(
            Darkside.DarksideEmptyBlocks.newBuilder().setHeight(startHeight.value.toInt()).setCount(count)
                .setNonce(nonce).build()
        )
    }

    fun stageTransactions(
        txs: Iterator<Service.RawTransaction>?,
        tipHeight: BlockHeightUnsafe
    ) {
        if (txs == null) {
            return
        }
        val response = EmptyResponse()
        createStreamingStub().stageTransactionsStream(response).apply {
            txs.forEach {
                // apply the tipHeight because the passed in txs might not know their destination
                // height (if they were created via SendTransaction)
                onNext(
                    it.newBuilderForType().setData(it.data).setHeight(tipHeight.value).build()
                )
            }
            onCompleted()
        }
        response.await()
    }

    fun applyBlocks(tipHeight: BlockHeightUnsafe) {
        createStub().applyStaged(tipHeight.toHeight())
    }

    fun getSentTransactions(): MutableIterator<Service.RawTransaction>? {
        return createStub().getIncomingTransactions(Service.Empty.newBuilder().build())
    }
//    fun setMetaState(
//        branchId: String = "2bb40e60", // Blossom,
//        chainName: String = "darkside",
//        saplingActivationHeight: Int = 419200
//    ): DarksideApi = apply {
//        createStub().setMetaState(
//            Darkside.DarksideMetaState.newBuilder()
//                .setBranchID(branchId)
//                .setChainName(chainName)
//                .setSaplingActivation(saplingActivationHeight)
//                .build()
//        )
//    }

//    fun setLatestHeight(latestHeight: Int) = setState(latestHeight, reorgHeight)
//
//    fun setReorgHeight(reorgHeight: Int)
//            = setState(latestHeight.coerceAtLeast(reorgHeight), reorgHeight)
//
//    fun setState(latestHeight: Int = -1, reorgHeight: Int = latestHeight): DarksideApi {
//        this.latestHeight = latestHeight
//        this.reorgHeight = reorgHeight
//        // change this service to accept ints as heights, like everywhere else
//        createStub().darksideSetState(
//            Darkside.DarksideState.newBuilder()
//                .setLatestHeight(latestHeight.toLong())
//                .setReorgHeight(reorgHeight.toLong())
//                .build()
//        )
//        return this
//    }

    private fun createStub(): DarksideStreamerGrpc.DarksideStreamerBlockingStub =
        DarksideStreamerGrpc
            .newBlockingStub(channel)
            .withDeadlineAfter(singleRequestTimeout.inWholeSeconds, TimeUnit.SECONDS)

    private fun createStreamingStub(): DarksideStreamerGrpc.DarksideStreamerStub =
        DarksideStreamerGrpc
            .newStub(channel)
            .withDeadlineAfter(singleRequestTimeout.inWholeSeconds, TimeUnit.SECONDS)

    private fun String.toUrl() = Darkside.DarksideBlocksURL.newBuilder().setUrl(this).build()

    class EmptyResponse : StreamObserver<Service.Empty> {
        companion object {
            private val DEFAULT_DELAY = 20.milliseconds
        }

        var completed = false
        var error: Throwable? = null

        override fun onNext(value: Service.Empty?) {
            // No implementation
        }

        override fun onError(t: Throwable?) {
            error = t
            completed = true
        }

        override fun onCompleted() {
            completed = true
        }

        fun await() {
            while (!completed) {
                Thread.sleep(DEFAULT_DELAY.inWholeSeconds)
            }
            if (error != null) {
                error("Server responded with an error: $error caused by ${error?.cause}")
            }
        }
    }
}

private fun BlockHeightUnsafe.toHeight() = Darkside.DarksideHeight.newBuilder().setHeight(this.value.toInt()).build()

fun DarksideApi.Companion.new(
    context: Context,
    lightWalletEndpoint: LightWalletEndpoint
) = DarksideApi.new(AndroidChannelFactory(context), lightWalletEndpoint)
