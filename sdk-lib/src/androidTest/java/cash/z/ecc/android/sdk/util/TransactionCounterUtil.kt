package cash.z.ecc.android.sdk.util

import androidx.test.platform.app.InstrumentationRegistry
import cash.z.ecc.android.sdk.internal.TroubleshootingTwig
import cash.z.ecc.android.sdk.internal.Twig
import cash.z.ecc.android.sdk.internal.model.from
import cash.z.ecc.android.sdk.internal.twig
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.Mainnet
import cash.z.ecc.android.sdk.model.ZcashNetwork
import co.electriccoin.lightwallet.client.BlockingLightWalletClient
import co.electriccoin.lightwallet.client.model.BlockHeightUnsafe
import co.electriccoin.lightwallet.client.model.LightWalletEndpoint
import co.electriccoin.lightwallet.client.new
import org.junit.Ignore
import org.junit.Test

class TransactionCounterUtil {

    private val network = ZcashNetwork.Mainnet
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val service = BlockingLightWalletClient.new(context, LightWalletEndpoint.Mainnet)

    init {
        Twig.plant(TroubleshootingTwig())
    }

    @Test
    @Ignore("This test is broken")
    fun testBlockSize() {
        val sizes = mutableMapOf<Int, Int>()
        service.getBlockRange(
            BlockHeightUnsafe.from(
                BlockHeight.new(
                    ZcashNetwork.Mainnet,
                    900_000
                )
            )..BlockHeightUnsafe.from(
                BlockHeight.new(
                    ZcashNetwork.Mainnet,
                    910_000
                )
            )
        ).forEach { b ->
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
        service.getBlockRange(
            BlockHeightUnsafe.from(
                BlockHeight.new(
                    ZcashNetwork.Mainnet,
                    900_000
                )
            )..BlockHeightUnsafe.from(
                BlockHeight.new(
                    ZcashNetwork.Mainnet,
                    950_000
                )
            )
        ).forEach { b ->
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
