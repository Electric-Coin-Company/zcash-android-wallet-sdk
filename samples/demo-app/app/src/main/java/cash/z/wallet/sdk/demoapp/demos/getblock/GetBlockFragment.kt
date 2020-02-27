package cash.z.wallet.sdk.demoapp.demos.getblock

import android.view.LayoutInflater
import android.view.View
import cash.z.wallet.sdk.demoapp.App
import cash.z.wallet.sdk.demoapp.BaseDemoFragment
import cash.z.wallet.sdk.demoapp.databinding.FragmentGetBlockBinding
import cash.z.wallet.sdk.service.LightWalletGrpcService
import cash.z.wallet.sdk.service.LightWalletService

/**
 * Retrieves a compact block from the lightwalletd service and displays basic information about it.
 * This demonstrates the basic ability to connect to the server, request a compact block and parse
 * the response.
 */
class GetBlockFragment : BaseDemoFragment<FragmentGetBlockBinding>() {
    private val host = App.instance.defaultConfig.host
    private val port = App.instance.defaultConfig.port

    private lateinit var lightwalletService: LightWalletService

    override fun inflateBinding(layoutInflater: LayoutInflater): FragmentGetBlockBinding =
        FragmentGetBlockBinding.inflate(layoutInflater)

    override fun resetInBackground() {
        lightwalletService = LightWalletGrpcService(App.instance, host, port)
    }

    override fun onResetComplete() {
        binding.buttonApply.setOnClickListener(::onApply)
        onApply(binding.textBlockHeight)
    }

    private fun onApply(_unused: View) {
        setBlockHeight(binding.textBlockHeight.text.toString().toInt())
    }

    private fun setBlockHeight(blockHeight: Int) {
        val blocks =
            lightwalletService.getBlockRange(blockHeight..blockHeight)
        val block = blocks.firstOrNull()
        binding.textInfo.text = """
                block height: ${block?.height}
                block vtxCount: ${block?.vtxCount}
                block time: ${block?.time}
            """.trimIndent()
    }

    override fun onClear() {
        lightwalletService.shutdown()
    }
}