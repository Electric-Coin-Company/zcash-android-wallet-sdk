package cash.z.ecc.android.sdk.demoapp.demos.getblockrange

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import cash.z.ecc.android.sdk.demoapp.BaseDemoFragment
import cash.z.ecc.android.sdk.demoapp.R
import cash.z.ecc.android.sdk.demoapp.databinding.FragmentGetBlockRangeBinding
import cash.z.ecc.android.sdk.demoapp.ext.requireApplicationContext
import cash.z.ecc.android.sdk.demoapp.util.fromResources
import cash.z.ecc.android.sdk.demoapp.util.mainActivity
import cash.z.ecc.android.sdk.model.ZcashNetwork
import kotlin.math.max

/**
 * Retrieves a range of compact block from the lightwalletd server and displays basic information
 * about them. This demonstrates the basic ability to connect to the server, request a range of
 * compact block and parse the response. This could be augmented to display metadata about certain
 * block ranges for instance, to find the block with the most shielded transactions in a range.
 */
class GetBlockRangeFragment : BaseDemoFragment<FragmentGetBlockRangeBinding>() {
    // TODO [#973]: Eliminate old UI demo-app
    // TODO [#973]: https://github.com/zcash/zcash-android-wallet-sdk/issues/973
    @Suppress("MaxLineLength", "MagicNumber", "UNUSED_PARAMETER")
    /*
    private fun setBlockRange(blockRange: ClosedRange<BlockHeight>) {
        val start = System.currentTimeMillis()

        val range = BlockHeightUnsafe(blockRange.start.value)..BlockHeightUnsafe(blockRange.endInclusive.value)

        val response = lightWalletClient?.getBlockRange(range)

        val blocks = when (response) {
            is Response.Success -> {
                Twig.debug { "Get blocks: ${response.result} for range: $range succeeded." }
                response.result
            }
            else -> {
                Twig.debug { "Get blocks for range: $range failed with: $response." }
                null
            }
        }

        val fetchDelta = System.currentTimeMillis() - start

        // Note: This is a demo so we won't worry about iterating efficiently over these blocks
        // Note: Converting the blocks sequence to a list can consume a lot of memory and may
        // cause OOM.
        binding.textInfo.text = HtmlCompat.fromHtml(
            blocks?.toList()?.run {
                val count = size
                val processTime = System.currentTimeMillis() - start - fetchDelta
                var totalSaplingOutputCount: UInt = 0u
                var totalOrchardOutputCount: UInt = 0u
                forEach {
                    totalSaplingOutputCount += it.saplingOutputsCount
                    totalOrchardOutputCount += it.orchardOutputsCount
                }

                @Suppress("MaxLineLength", "MagicNumber")
                """
                <b>total blocks:</b> ${count.withCommas()}
                <br/><b>fetch time:</b> ${if (fetchDelta > 1000) "%.2f sec".format(fetchDelta / 1000.0) else "%d ms".format(fetchDelta)}
                <br/><b>process time:</b> ${if (processTime > 1000) "%.2f sec".format(processTime / 1000.0) else "%d ms".format(processTime)}
                <br/><b>block time range:</b> ${first().time.toRelativeTime(requireApplicationContext())}<br/>&nbsp;&nbsp to ${last().time.toRelativeTime(requireApplicationContext())}
                <br/><b>total sapling outputs:</b> $totalSaplingOutputCount
                <br/><b>total orchard outputs:</b> $totalOrchardOutputCount
                """.trimIndent()
            } ?: "No blocks found in that range.",
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
    }
     */

    private fun onApply(unused: View) {
        val network = ZcashNetwork.fromResources(requireApplicationContext())
        val start =
            max(
                binding.textStartHeight.text
                    .toString()
                    .toLongOrNull()
                    ?: network.saplingActivationHeight.value,
                network.saplingActivationHeight.value
            )
        val end =
            max(
                binding.textEndHeight.text
                    .toString()
                    .toLongOrNull()
                    ?: network.saplingActivationHeight.value,
                network.saplingActivationHeight.value
            )
        if (start <= end) {
            @Suppress("TooGenericExceptionCaught")
            try {
                with(binding.buttonApply) {
                    isEnabled = false
                    setText(R.string.loading)
                    binding.textInfo.setText(R.string.loading)
                    post {
                        // TODO [#973]: Eliminate old UI demo-app
                        // TODO [#973]: https://github.com/zcash/zcash-android-wallet-sdk/issues/973
                        // setBlockRange(
                        //     BlockHeight.new(network, start)..BlockHeight.new(
                        //         network,
                        //         end
                        //     )
                        // )
                        isEnabled = true
                        setText(R.string.apply)
                    }
                }
            } catch (t: Throwable) {
                setError(t.toString())
            }
        } else {
            setError("Invalid range")
        }
        mainActivity()?.hideKeyboard()
    }

    private fun setError(message: String) {
        binding.textInfo.text = "Error: $message"
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
    }

    //
    // Base Fragment overrides
    //

    override fun inflateBinding(layoutInflater: LayoutInflater): FragmentGetBlockRangeBinding =
        FragmentGetBlockRangeBinding.inflate(layoutInflater)

    override fun onActionButtonClicked() {
        super.onActionButtonClicked()
    }
}
