package cash.z.ecc.android.sdk.util

import android.content.Context
import cash.z.ecc.android.sdk.R
import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.ext.twig
import cash.z.wallet.sdk.rpc.Darkside
import cash.z.wallet.sdk.rpc.Darkside.DarksideTransactionsURL
import cash.z.wallet.sdk.rpc.DarksideStreamerGrpc
import cash.z.ecc.android.sdk.service.LightWalletGrpcService
import io.grpc.ManagedChannel
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class DarksideApi(
    private val channel: ManagedChannel,
    private val singleRequestTimeoutSec: Long = 10L
) {

    constructor(
        appContext: Context,
        host: String,
        port: Int = ZcashSdk.DEFAULT_LIGHTWALLETD_PORT,
        usePlainText: Boolean = appContext.resources.getBoolean(
            R.bool.lightwalletd_allow_very_insecure_connections
        )
    ) : this(
        LightWalletGrpcService.createDefaultChannel(
            appContext,
            host,
            port,
            usePlainText
        )
    )


    //
    // Service APIs
    //

    fun reset(
        saplingActivationHeight: Int = 419200,
        branchId: String = "2bb40e60", // Blossom,
        chainName: String = "darkside"
    ) = apply {
        twig("resetting darksidewalletd with saplingActivation=$saplingActivationHeight branchId=$branchId chainName=$chainName")
        Darkside.DarksideMetaState.newBuilder()
            .setBranchID(branchId)
            .setChainName(chainName)
            .setSaplingActivation(saplingActivationHeight)
            .build().let { request ->
                createStub().reset(request)
            }
    }

    fun stageBlocks(url: String) = apply {
        twig("staging block url=$url")
        createStub().stageBlocks(url.toUrl())
    }

    fun stageTransactions(url: String, targetHeight: Int) = apply {
        twig("staging transaction at height=$targetHeight from url=$url")
        createStub().stageTransactions(
            DarksideTransactionsURL.newBuilder().setHeight(targetHeight).setUrl(url).build()
        )
    }

    fun stageEmptyBlocks(startHeight: Int, count: Int = 10, nonce: Int = Random.nextInt()) = apply {
        createStub().stageBlocksCreate(
            Darkside.DarksideEmptyBlocks.newBuilder().setHeight(startHeight).setCount(count).setNonce(nonce).build()
        )
    }

    fun applyBlocks(tipHeight: Int) {
        twig("applying blocks up to tipHeight=$tipHeight")
        createStub().applyStaged(tipHeight.toHeight())
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

    private fun String.toUrl() = Darkside.DarksideBlocksURL.newBuilder().setUrl(this).build()
    private fun Int.toHeight() = Darkside.DarksideHeight.newBuilder().setHeight(this).build()

}
