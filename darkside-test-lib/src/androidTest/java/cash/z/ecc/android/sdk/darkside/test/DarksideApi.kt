package cash.z.ecc.android.sdk.darkside.test

import android.content.Context
import cash.z.ecc.android.sdk.R
import cash.z.ecc.android.sdk.internal.service.LightWalletGrpcService
import cash.z.ecc.android.sdk.internal.twig
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.Darkside
import cash.z.ecc.android.sdk.model.LightWalletEndpoint
import cash.z.ecc.android.sdk.model.ZcashNetwork
import cash.z.wallet.sdk.rpc.Darkside
import cash.z.wallet.sdk.rpc.Darkside.DarksideTransactionsURL
import cash.z.wallet.sdk.rpc.DarksideStreamerGrpc
import cash.z.wallet.sdk.rpc.Service
import io.grpc.ManagedChannel
import io.grpc.stub.StreamObserver
import java.lang.RuntimeException
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class DarksideApi(
    private val channel: ManagedChannel,
    private val singleRequestTimeoutSec: Long = 10L
) {

    constructor(
        appContext: Context,
        lightWalletEndpoint: LightWalletEndpoint
    ) : this(
        LightWalletGrpcService.createDefaultChannel(
            appContext,
            lightWalletEndpoint
        )
    )

    //
    // Service APIs
    //

    fun reset(
        saplingActivationHeight: BlockHeight = ZcashNetwork.Mainnet.saplingActivationHeight,
        branchId: String = "e9ff75a6", // Canopy,
        chainName: String = "darkside${ZcashNetwork.Mainnet.networkName}"
    ) = apply {
        twig("resetting darksidewalletd with saplingActivation=$saplingActivationHeight branchId=$branchId chainName=$chainName")
        Darkside.DarksideMetaState.newBuilder()
            .setBranchID(branchId)
            .setChainName(chainName)
            .setSaplingActivation(saplingActivationHeight.value.toInt())
            .build().let { request ->
                createStub().reset(request)
            }
    }

    fun stageBlocks(url: String) = apply {
        twig("staging blocks url=$url")
        createStub().stageBlocks(url.toUrl())
    }

    fun stageTransactions(url: String, targetHeight: BlockHeight) = apply {
        twig("staging transaction at height=$targetHeight from url=$url")
        createStub().stageTransactions(
            DarksideTransactionsURL.newBuilder().setHeight(targetHeight.value).setUrl(url).build()
        )
    }

    fun stageEmptyBlocks(startHeight: BlockHeight, count: Int = 10, nonce: Int = Random.nextInt()) = apply {
        twig("staging $count empty blocks starting at $startHeight with nonce $nonce")
        createStub().stageBlocksCreate(
            Darkside.DarksideEmptyBlocks.newBuilder().setHeight(startHeight.value).setCount(count).setNonce(nonce).build()
        )
    }

    fun stageTransactions(txs: Iterator<Service.RawTransaction>?, tipHeight: BlockHeight) {
        if (txs == null) {
            twig("no transactions to stage")
            return
        }
        twig("staging transaction at height=$tipHeight")
        val response = EmptyResponse()
        createStreamingStub().stageTransactionsStream(response).apply {
            txs.forEach {
                twig("stageTransactions: onNext calling!!!")
                onNext(it.newBuilderForType().setData(it.data).setHeight(tipHeight.value).build()) // apply the tipHeight because the passed in txs might not know their destination height (if they were created via SendTransaction)
                twig("stageTransactions: onNext called")
            }
            twig("stageTransactions: onCompleted calling!!!")
            onCompleted()
            twig("stageTransactions: onCompleted called")
        }
        response.await()
    }

    fun applyBlocks(tipHeight: BlockHeight) {
        twig("applying blocks up to tipHeight=$tipHeight")
        createStub().applyStaged(tipHeight.toHeight())
    }

    fun getSentTransactions(): MutableIterator<Service.RawTransaction>? {
        twig("grabbing sent transactions...")
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
//        // TODO: change this service to accept ints as heights, like everywhere else
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
            .withDeadlineAfter(singleRequestTimeoutSec, TimeUnit.SECONDS)

    private fun createStreamingStub(): DarksideStreamerGrpc.DarksideStreamerStub =
        DarksideStreamerGrpc
            .newStub(channel)
            .withDeadlineAfter(singleRequestTimeoutSec, TimeUnit.SECONDS)

    private fun String.toUrl() = Darkside.DarksideBlocksURL.newBuilder().setUrl(this).build()
    private fun BlockHeight.toHeight() = Darkside.DarksideHeight.newBuilder().setHeight(this.value).build()

    class EmptyResponse : StreamObserver<Service.Empty> {
        var completed = false
        var error: Throwable? = null
        override fun onNext(value: Service.Empty?) {
            twig("<><><><><><><><> EMPTY RESPONSE: ONNEXT CALLED!!!!")
        }

        override fun onError(t: Throwable?) {
            twig("<><><><><><><><> EMPTY RESPONSE: ONERROR CALLED!!!!")
            error = t
            completed = true
        }

        override fun onCompleted() {
            twig("<><><><><><><><> EMPTY RESPONSE: ONCOMPLETED CALLED!!!")
            completed = true
        }

        fun await() {
            while (!completed) {
                twig("awaiting server response...")
                Thread.sleep(20L)
            }
            if (error != null) throw RuntimeException("Server responded with an error: $error caused by ${error?.cause}")
        }
    }
}
