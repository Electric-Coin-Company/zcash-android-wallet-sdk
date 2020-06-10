package cash.z.ecc.android.sdk.demoapp.demos.getblockrange

import android.view.LayoutInflater
import android.view.View
import cash.z.ecc.android.sdk.demoapp.App
import cash.z.ecc.android.sdk.demoapp.BaseDemoFragment
import cash.z.ecc.android.sdk.demoapp.databinding.FragmentGetBlockRangeBinding
import cash.z.ecc.android.sdk.service.LightWalletGrpcService
import cash.z.ecc.android.sdk.service.LightWalletService

/**
 * Retrieves a range of compact block from the lightwalletd service and displays basic information
 * about them. This demonstrates the basic ability to connect to the server, request a range of 
 * compact block and parse the response. This could be augmented to display metadata about certain
 * block ranges for instance, to find the block with the most shielded transactions in a range.
 */
class GetBlockRangeFragment : BaseDemoFragment<FragmentGetBlockRangeBinding>() {

    private val host = App.instance.defaultConfig.host
    private val port = App.instance.defaultConfig.port

    private lateinit var lightwalletService: LightWalletService
    
    override fun inflateBinding(layoutInflater: LayoutInflater): FragmentGetBlockRangeBinding =
        FragmentGetBlockRangeBinding.inflate(layoutInflater)

    override fun resetInBackground() {
        lightwalletService = LightWalletGrpcService(App.instance, host, port)
    }

    override fun onResetComplete() {
        binding.buttonApply.setOnClickListener(::onApply)
        onApply(binding.textInfo)
    }

    override fun onClear() {
        lightwalletService.shutdown()
    }

    private fun onApply(_unused: View) {
        val start = binding.textStartHeight.text.toString().toInt()
        val end = binding.textEndHeight.text.toString().toInt()
        if (start <= end) {
            setBlockRange(start..end)
        } else {
            setError("Invalid range")
        }
    }

    // TODO: iterate on this demo to show all the blocks in a recyclerview showing block heights and vtx count
    private fun setBlockRange(blockRange: IntRange) {
        val blocks =
            lightwalletService.getBlockRange(blockRange)
        val block = blocks.firstOrNull()
        binding.textInfo.text = """
                block height: ${block?.height}
                block vtxCount: ${block?.vtxCount}
                block time: ${block?.time}
            """.trimIndent()
    }

    private fun setError(message: String) {
        binding.textInfo.text = "Error: $message"
    }
}
