package cash.z.wallet.sdk.demoapp.demos.getlatestheight

import android.view.LayoutInflater
import android.view.View
import cash.z.wallet.sdk.demoapp.App
import cash.z.wallet.sdk.demoapp.BaseDemoFragment
import cash.z.wallet.sdk.demoapp.databinding.FragmentGetLatestHeightBinding
import cash.z.wallet.sdk.service.LightWalletGrpcService
import cash.z.wallet.sdk.service.LightWalletService

class GetLatestHeightFragment : BaseDemoFragment<FragmentGetLatestHeightBinding>() {
    private val host = App.instance.defaultConfig.host

    private lateinit var lightwalletService: LightWalletService

    override fun inflateBinding(layoutInflater: LayoutInflater): FragmentGetLatestHeightBinding =
        FragmentGetLatestHeightBinding.inflate(layoutInflater)

    override fun resetInBackground() {
        lightwalletService = LightWalletGrpcService(App.instance, host)
    }

    override fun onResetComplete() {
        binding.textInfo.text = lightwalletService.getLatestBlockHeight().toString()
    }

    override fun onClear() {
        lightwalletService.shutdown()
    }
}