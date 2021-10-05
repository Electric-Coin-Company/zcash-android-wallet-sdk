package cash.z.ecc.android.sdk.util

import androidx.test.platform.app.InstrumentationRegistry
import cash.z.ecc.android.sdk.ext.TroubleshootingTwig
import cash.z.ecc.android.sdk.ext.Twig
import cash.z.ecc.android.sdk.ext.twig
import cash.z.ecc.android.sdk.service.LightWalletGrpcService
import cash.z.ecc.android.sdk.type.NetworkType
import cash.z.ecc.android.sdk.type.ZcashNetwork
import org.junit.Ignore
import org.junit.Test

class TransactionCounterUtil {

    private val network = NetworkType.Mainnet
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val service = LightWalletGrpcService(context, network)

    init {
        Twig.plant(TroubleshootingTwig())
    }

    @Test
    @Ignore("This test is broken")
    fun testBlockSize() {
        val sizes = mutableMapOf<Int, Int>()
        service.getBlockRange(900_000..910_000).forEach { b ->
            twig("h: ${b.header.size()}")
            val s = b.serializedSize
            sizes[s] = (sizes[s] ?: 0) + 1
        }
        twig("sizes: ${sizes.toSortedMap()}")
    }

    @Test
    @Ignore("This test is broken")
    fun testCountTransactions() {
        val txCounts = mutableMapOf<Int, Int>()
        val outputCounts = mutableMapOf<Int, Int>()
        var totalOutputs = 0
        var totalTxs = 0
        service.getBlockRange(900_000..950_000).forEach { b ->
            b.header.size()
            b.vtxList.map { it.outputsCount }.forEach { oCount ->
                outputCounts[oCount] = (outputCounts[oCount] ?: 0) + oCount.coerceAtLeast(1)
                totalOutputs += oCount
            }
            b.vtxCount.let { count ->
                txCounts[count] = (txCounts[count] ?: 0) + count.coerceAtLeast(1)
                totalTxs += count
            }
        }
        twig("txs: $txCounts")
        twig("outputs: $outputCounts")
        twig("total: $totalTxs  $totalOutputs")
    }
}
/*


 */
