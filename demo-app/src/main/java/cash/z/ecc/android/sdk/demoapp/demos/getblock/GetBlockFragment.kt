package cash.z.ecc.android.sdk.demoapp.demos.getblock

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.text.HtmlCompat
import cash.z.ecc.android.sdk.demoapp.BaseDemoFragment
import cash.z.ecc.android.sdk.demoapp.databinding.FragmentGetBlockBinding
import cash.z.ecc.android.sdk.demoapp.ext.requireApplicationContext
import cash.z.ecc.android.sdk.demoapp.util.fromResources
import cash.z.ecc.android.sdk.demoapp.util.mainActivity
import cash.z.ecc.android.sdk.demoapp.util.toHtml
import cash.z.ecc.android.sdk.demoapp.util.toRelativeTime
import cash.z.ecc.android.sdk.demoapp.util.withCommas
import cash.z.ecc.android.sdk.ext.toHex
import cash.z.ecc.android.sdk.internal.twig
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.ZcashNetwork
import co.electriccoin.lightwallet.client.model.BlockHeightUnsafe
import co.electriccoin.lightwallet.client.model.Response
import kotlin.math.min

/**
 * Retrieves a compact block from the lightwalletd server and displays basic information about it.
 * This demonstrates the basic ability to connect to the server, request a compact block and parse
 * the response.
 */
class GetBlockFragment : BaseDemoFragment<FragmentGetBlockBinding>() {

    private fun setBlockHeight(blockHeight: BlockHeight) {
        val response = lightWalletClient?.getBlockRange(
            BlockHeightUnsafe(blockHeight.value)..BlockHeightUnsafe(blockHeight.value)
        )

        val blocks = when (response) {
            is Response.Success -> {
                twig("Get block: ${response.result} for height: $blockHeight succeeded.")
                response.result
            }
            else -> {
                twig("Get block for height: $blockHeight failed with: $response.")
                null
            }
        }

        val block = blocks?.firstOrNull()
        binding.textInfo.visibility = View.VISIBLE
        binding.textInfo.text = HtmlCompat.fromHtml(
            """
                <b>block height:</b> ${block?.height.withCommas()}
                <br/><b>block time:</b> ${block?.time.toRelativeTime(requireApplicationContext())}
                <br/><b>number of shielded TXs:</b> ${block?.vtx?.size}
                <br/><b>hash:</b> ${block?.hash?.toHex()}
                <br/><b>prevHash:</b> ${block?.prevHash?.toHex()}
                ${block?.vtx.toHtml()}
            """.trimIndent(),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onApply(unused: View? = null) {
        val network = ZcashNetwork.fromResources(requireApplicationContext())
        val newHeight = min(
            binding.textBlockHeight.text.toString().toLongOrNull()
                ?: network.saplingActivationHeight.value,
            network.saplingActivationHeight.value
        )

        @Suppress("TooGenericExceptionCaught")
        try {
            setBlockHeight(BlockHeight.new(network, newHeight))
        } catch (t: Throwable) {
            toast("Error: $t")
        }
        mainActivity()?.hideKeyboard()
    }

    private fun loadNext(offset: Int) {
        val nextBlockHeight = (binding.textBlockHeight.text.toString().toIntOrNull() ?: -1) + offset
        binding.textBlockHeight.setText(nextBlockHeight.toString())
        onApply()
    }

    //
    // Android Lifecycle overrides
    //

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.buttonApply.setOnClickListener(::onApply)
        binding.buttonPrevious.setOnClickListener {
            loadNext(-1)
        }
        binding.buttonNext.setOnClickListener {
            loadNext(1)
        }
    }

    //
    // Base Fragment overrides
    //

    override fun inflateBinding(layoutInflater: LayoutInflater): FragmentGetBlockBinding =
        FragmentGetBlockBinding.inflate(layoutInflater)
}
