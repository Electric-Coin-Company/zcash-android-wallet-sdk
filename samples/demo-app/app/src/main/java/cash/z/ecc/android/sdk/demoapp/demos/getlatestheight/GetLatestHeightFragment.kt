package cash.z.ecc.android.sdk.demoapp.demos.getlatestheight

import android.view.LayoutInflater
import android.view.View
import cash.z.ecc.android.sdk.demoapp.App
import cash.z.ecc.android.sdk.demoapp.BaseDemoFragment
import cash.z.ecc.android.sdk.demoapp.databinding.FragmentGetLatestHeightBinding
import cash.z.ecc.android.sdk.service.LightWalletGrpcService
import cash.z.ecc.android.sdk.service.LightWalletService

/**
 * Retrieves the latest block height from the lightwalletd server. This is the simplest test for
 * connectivity with the server. Modify the `host` and the `port` to check the SDK's ability to
 * communicate with a given lightwalletd instance.
 */
class GetLatestHeightFragment : BaseDemoFragment<FragmentGetLatestHeightBinding>() {
    private val host = App.instance.defaultConfig.host
    private val port = App.instance.defaultConfig.port

    private lateinit var lightwalletService: LightWalletService

    override fun inflateBinding(layoutInflater: LayoutInflater): FragmentGetLatestHeightBinding =
        FragmentGetLatestHeightBinding.inflate(layoutInflater)

    override fun resetInBackground() {
        lightwalletService = LightWalletGrpcService(App.instance, host, port)
    }

    override fun onResetComplete() {
        binding.textInfo.text = lightwalletService.getLatestBlockHeight().toString()
    }

    override fun onClear() {
        lightwalletService.shutdown()
    }

    override fun onActionButtonClicked() {
        toast("Refreshed!")
        onResetComplete()
    }
}
