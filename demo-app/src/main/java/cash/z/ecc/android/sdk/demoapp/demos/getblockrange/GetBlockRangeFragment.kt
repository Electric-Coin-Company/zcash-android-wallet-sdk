package cash.z.ecc.android.sdk.demoapp.demos.getblockrange

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.text.HtmlCompat
import cash.z.ecc.android.sdk.demoapp.BaseDemoFragment
import cash.z.ecc.android.sdk.demoapp.R
import cash.z.ecc.android.sdk.demoapp.databinding.FragmentGetBlockRangeBinding
import cash.z.ecc.android.sdk.demoapp.ext.requireApplicationContext
import cash.z.ecc.android.sdk.demoapp.util.fromResources
import cash.z.ecc.android.sdk.demoapp.util.mainActivity
import cash.z.ecc.android.sdk.demoapp.util.toRelativeTime
import cash.z.ecc.android.sdk.demoapp.util.withCommas
import cash.z.ecc.android.sdk.internal.twig
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.ZcashNetwork
import co.electriccoin.lightwallet.client.model.BlockHeightUnsafe
import co.electriccoin.lightwallet.client.model.Response
import kotlin.math.max

/**
 * Retrieves a range of compact block from the lightwalletd server and displays basic information
 * about them. This demonstrates the basic ability to connect to the server, request a range of
 * compact block and parse the response. This could be augmented to display metadata about certain
 * block ranges for instance, to find the block with the most shielded transactions in a range.
 */
class GetBlockRangeFragment : BaseDemoFragment<FragmentGetBlockRangeBinding>() {

    private fun setBlockRange(blockRange: ClosedRange<BlockHeight>) {
        val start = System.currentTimeMillis()

        val range = BlockHeightUnsafe(blockRange.start.value)..BlockHeightUnsafe(blockRange.endInclusive.value)

        val response = lightWalletClient?.getBlockRange(range)

        val blocks = when (response) {
            is Response.Success -> {
                twig("Get blocks: ${response.result} for range: $range succeeded.")
                response.result
            }
            else -> {
                twig("Get blocks for range: $range failed with: $response.")
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
                val emptyCount = count { it.vtx.isEmpty() }
                val maxTxs = maxByOrNull { it.vtx.size }
                val maxIns = maxByOrNull { block ->
                    block.vtx.maxOfOrNull { it.spends.size } ?: -1
                }
                val maxInTx = maxIns?.vtx?.maxByOrNull { it.spends.size }
                val maxOuts = maxByOrNull { block ->
                    block.vtx.maxOfOrNull { it.outputs.size } ?: -1
                }
                val maxOutTx = maxOuts?.vtx?.maxByOrNull { it.outputs.size }
                val txCount = sumOf { it.vtx.size }
                val outCount = sumOf { block -> block.vtx.sumOf { it.outputs.size } }
                val inCount = sumOf { block -> block.vtx.sumOf { it.spends.size } }

                val processTime = System.currentTimeMillis() - start - fetchDelta
                @Suppress("MaxLineLength", "MagicNumber")
                """
                <b>total blocks:</b> ${count.withCommas()}
                <br/><b>fetch time:</b> ${if (fetchDelta > 1000) "%.2f sec".format(fetchDelta / 1000.0) else "%d ms".format(fetchDelta)}
                <br/><b>process time:</b> ${if (processTime > 1000) "%.2f sec".format(processTime / 1000.0) else "%d ms".format(processTime)}
                <br/><b>block time range:</b> ${first().time.toRelativeTime(requireApplicationContext())}<br/>&nbsp;&nbsp to ${last().time.toRelativeTime(requireApplicationContext())}
                <br/><b>total empty blocks:</b> ${emptyCount.withCommas()}
                <br/><b>total TXs:</b> ${txCount.withCommas()}
                <br/><b>total outputs:</b> ${outCount.withCommas()}
                <br/><b>total inputs:</b> ${inCount.withCommas()}
                <br/><b>avg TXs/block:</b> ${"%.1f".format(txCount / count.toDouble())}
                <br/><b>avg TXs (excluding empty blocks):</b> ${"%.1f".format(txCount.toDouble() / (count - emptyCount))}
                <br/><b>avg OUTs [per block / per TX]:</b> ${"%.1f / %.1f".format(outCount.toDouble() / (count - emptyCount), outCount.toDouble() / txCount)}
                <br/><b>avg INs [per block / per TX]:</b> ${"%.1f / %.1f".format(inCount.toDouble() / (count - emptyCount), inCount.toDouble() / txCount)}
                <br/><b>most shielded TXs:</b> ${if (maxTxs == null) "none" else "${maxTxs.vtx.size} in block ${maxTxs.height.withCommas()}"}
                <br/><b>most shielded INs:</b> ${if (maxInTx == null) "none" else "${maxInTx.spends.size} in block ${maxIns.height.withCommas()} at tx index ${maxInTx.index}"}
                <br/><b>most shielded OUTs:</b> ${if (maxOutTx == null) "none" else "${maxOutTx.outputs.size} in block ${maxOuts.height.withCommas()} at tx index ${maxOutTx.index}"}
                """.trimIndent()
            } ?: "No blocks found in that range.",
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onApply(unused: View) {
        val network = ZcashNetwork.fromResources(requireApplicationContext())
        val start = max(
            binding.textStartHeight.text.toString().toLongOrNull()
                ?: network.saplingActivationHeight.value,
            network.saplingActivationHeight.value
        )
        val end = max(
            binding.textEndHeight.text.toString().toLongOrNull()
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
                        setBlockRange(
                            BlockHeight.new(network, start)..BlockHeight.new(
                                network,
                                end
                            )
                        )
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
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
