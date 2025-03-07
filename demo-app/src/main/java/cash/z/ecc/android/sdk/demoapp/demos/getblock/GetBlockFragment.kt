package cash.z.ecc.android.sdk.demoapp.demos.getblock

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import cash.z.ecc.android.sdk.demoapp.BaseDemoFragment
import cash.z.ecc.android.sdk.demoapp.databinding.FragmentGetBlockBinding
import cash.z.ecc.android.sdk.demoapp.util.mainActivity

// TODO [#973]: Eliminate old UI demo-app
// TODO [#973]: https://github.com/zcash/zcash-android-wallet-sdk/issues/973

/**
 * Retrieves a compact block from the lightwalletd server and displays basic information about it.
 * This demonstrates the basic ability to connect to the server, request a compact block and parse
 * the response.
 */
class GetBlockFragment : BaseDemoFragment<FragmentGetBlockBinding>() {
    /*
    var coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)

    private fun setBlockHeight(blockHeight: BlockHeight) {
        lightWalletClient?.getBlockRange(
            BlockHeightUnsafe(blockHeight.value)..BlockHeightUnsafe(blockHeight.value)
        )?.onFirstWith(coroutineScope) { response ->
            when (response) {
                is Response.Success<CompactBlockUnsafe> -> {
                    Twig.debug { "Get block: ${response.result} for height: $blockHeight succeeded." }

                    binding.textInfo.visibility = View.VISIBLE
                    binding.textInfo.text = HtmlCompat.fromHtml(
                        """
                            <b>block height:</b> ${response.result.height.withCommas()}
                            <br/><b>block time:</b> ${response.result.time.toRelativeTime(requireApplicationContext())}
                            <br/><b>number of sapling outputs:</b> ${response.result.saplingOutputsCount}
                            <br/><b>number of orchard outputs:</b> ${response.result.orchardOutputsCount}
                            <br/><b>hash:</b> ${response.result.hash.toHex()}
                        """.trimIndent(),
                        HtmlCompat.FROM_HTML_MODE_LEGACY
                    )
                }
                else -> {
                    Twig.debug { "Get block for height: $blockHeight failed with: $response." }
                    null
                }
            }
        }
    }
     */

    @Suppress("UNUSED_PARAMETER")
    private fun onApply(unused: View? = null) {
        // TODO [#973]: Eliminate old UI demo-app
        // TODO [#973]: https://github.com/zcash/zcash-android-wallet-sdk/issues/973
        // val network = ZcashNetwork.fromResources(requireApplicationContext())
        // val newHeight = min(
        //     binding.textBlockHeight.text.toString().toLongOrNull()
        //         ?: network.saplingActivationHeight.value,
        //     network.saplingActivationHeight.value
        // )

        @Suppress("TooGenericExceptionCaught")
        // try {
        //     setBlockHeight(BlockHeight.new(network, newHeight))
        // } catch (t: Throwable) {
        //     toast("Error: $t")
        // }
        mainActivity()?.hideKeyboard()
    }

    private fun loadNext(offset: Int) {
        val nextBlockHeight =
            (
                binding.textBlockHeight.text
                    .toString()
                    .toIntOrNull() ?: -1
            ) + offset
        binding.textBlockHeight.setText(nextBlockHeight.toString())
        onApply()
    }

    //
    // Android Lifecycle overrides
    //

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
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
    @Suppress("MaxLineLength")
    override fun inflateBinding(layoutInflater: LayoutInflater): FragmentGetBlockBinding = FragmentGetBlockBinding.inflate(layoutInflater)
}
