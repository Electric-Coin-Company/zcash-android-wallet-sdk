package cash.z.ecc.android.sdk.demoapp.demos.getblockrange

import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import cash.z.ecc.android.sdk.demoapp.BaseDemoFragment
import cash.z.ecc.android.sdk.demoapp.R
import cash.z.ecc.android.sdk.demoapp.databinding.FragmentGetBlockRangeBinding
import cash.z.ecc.android.sdk.demoapp.util.mainActivity
import cash.z.ecc.android.sdk.demoapp.util.toRelativeTime
import cash.z.ecc.android.sdk.demoapp.util.withCommas

/**
 * Retrieves a range of compact block from the lightwalletd service and displays basic information
 * about them. This demonstrates the basic ability to connect to the server, request a range of
 * compact block and parse the response. This could be augmented to display metadata about certain
 * block ranges for instance, to find the block with the most shielded transactions in a range.
 */
class GetBlockRangeFragment : BaseDemoFragment<FragmentGetBlockRangeBinding>() {

    private fun setBlockRange(blockRange: IntRange) {
        val start = System.currentTimeMillis()
        val blocks =
            lightwalletService?.getBlockRange(blockRange)
        val fetchDelta = System.currentTimeMillis() - start

        // Note: This is a demo so we won't worry about iterating efficiently over these blocks

        binding.textInfo.text = Html.fromHtml(blocks?.run {
            val count = size
            val emptyCount = count { it.vtxCount == 0 }
            val maxTxs = maxByOrNull { it.vtxCount }
            val maxIns = maxByOrNull { block ->
                block.vtxList.maxOfOrNull { it.spendsCount } ?: -1
            }
            val maxInTx = maxIns?.vtxList?.maxByOrNull { it.spendsCount }
            val maxOuts = maxByOrNull { block ->
                block.vtxList.maxOfOrNull { it.outputsCount } ?: -1
            }
            val maxOutTx = maxOuts?.vtxList?.maxByOrNull { it.outputsCount }
            val txCount = sumBy { it.vtxCount }
            val outCount = sumBy { block -> block.vtxList.sumBy { it.outputsCount } }
            val inCount = sumBy { block -> block.vtxList.sumBy { it.spendsCount } }

            val processTime = System.currentTimeMillis() - start - fetchDelta
            """
                <b>total blocks:</b> ${count.withCommas()}
                <br/><b>fetch time:</b> ${if(fetchDelta > 1000) "%.2f sec".format(fetchDelta/1000.0) else "%d ms".format(fetchDelta)}
                <br/><b>process time:</b> ${if(processTime > 1000) "%.2f sec".format(processTime/1000.0) else "%d ms".format(processTime)}
                <br/><b>block time range:</b> ${first().time.toRelativeTime()}<br/>&nbsp;&nbsp to ${last().time.toRelativeTime()}
                <br/><b>total empty blocks:</b> ${emptyCount.withCommas()}
                <br/><b>total TXs:</b> ${txCount.withCommas()}
                <br/><b>total outputs:</b> ${outCount.withCommas()}
                <br/><b>total inputs:</b> ${inCount.withCommas()}
                <br/><b>avg TXs/block:</b> ${"%.1f".format(txCount/count.toDouble())}
                <br/><b>avg TXs (excluding empty blocks):</b> ${"%.1f".format(txCount.toDouble()/(count - emptyCount))}
                <br/><b>avg OUTs [per block / per TX]:</b> ${"%.1f / %.1f".format(outCount.toDouble()/(count - emptyCount), outCount.toDouble()/txCount)}
                <br/><b>avg INs [per block / per TX]:</b> ${"%.1f / %.1f".format(inCount.toDouble()/(count - emptyCount), inCount.toDouble()/txCount)}
                <br/><b>most shielded TXs:</b> ${if(maxTxs==null) "none" else "${maxTxs.vtxCount} in block ${maxTxs.height.withCommas()}"}
                <br/><b>most shielded INs:</b> ${if(maxInTx==null) "none" else "${maxInTx.spendsCount} in block ${maxIns?.height.withCommas()} at tx index ${maxInTx.index}"}
                <br/><b>most shielded OUTs:</b> ${if(maxOutTx==null) "none" else "${maxOutTx?.outputsCount} in block ${maxOuts?.height.withCommas()} at tx index ${maxOutTx?.index}"}
            """.trimIndent()
        } ?: "No blocks found in that range.")
    }

    private fun onApply(_unused: View) {
        val start = binding.textStartHeight.text.toString().toInt()
        val end = binding.textEndHeight.text.toString().toInt()
        if (start <= end) {
            try {
                with(binding.buttonApply) {
                    isEnabled = false
                    setText(R.string.loading)
                    binding.textInfo.setText(R.string.loading)
                    post {
                        setBlockRange(start..end)
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
