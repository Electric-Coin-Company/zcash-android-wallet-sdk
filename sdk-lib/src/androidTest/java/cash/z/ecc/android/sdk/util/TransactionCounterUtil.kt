package cash.z.ecc.android.sdk.util

import androidx.test.platform.app.InstrumentationRegistry
import cash.z.ecc.android.sdk.fixture.LightWalletEndpointFixture
import cash.z.ecc.android.sdk.internal.Twig
import cash.z.ecc.android.sdk.internal.model.ext.from
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.ZcashNetwork
import co.electriccoin.lightwallet.client.LightWalletClient
import co.electriccoin.lightwallet.client.model.BlockHeightUnsafe
import co.electriccoin.lightwallet.client.model.LightWalletEndpoint
import co.electriccoin.lightwallet.client.new
import org.junit.Ignore
import org.junit.Test

class TransactionCounterUtil {
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val endpoint: LightWalletEndpoint = LightWalletEndpointFixture.newEndpointForNetwork(ZcashNetwork.Mainnet)
    private val lightWalletClient = LightWalletClient.new(context, endpoint)

    @Test
    @Ignore("This test is broken")
    fun testBlockSize() {
        val sizes = mutableMapOf<Int, Int>()

        @Suppress("ktlint:standard:multiline-expression-wrapping")
        lightWalletClient.getBlockRange(
            BlockHeightUnsafe.from(
                BlockHeight.new(900_000L)
            )..BlockHeightUnsafe.from(
                BlockHeight.new(910_000L)
            )
        )
        /*
        Fixme: work with flow instead of sequence
        assert(response is Response.Success)

        Fixme: serializedSize is not available anymore
        (response as Response.Success).result.forEach { compactBlock ->
            twig("h: ${compactBlock.header.size}")
            val s = compactBlock.serializedSize
            sizes[s] = (sizes[s] ?: 0) + 1
        }
         */

        Twig.debug { "sizes: ${sizes.toSortedMap()}" }
    }

    @Test
    @Ignore("This test is broken")
    fun testCountTransactions() {
        val txCounts = mutableMapOf<Int, Int>()
        val outputCounts = mutableMapOf<Int, Int>()
        var totalOutputs = 0
        var totalTxs = 0

        @Suppress("ktlint:standard:multiline-expression-wrapping")
        lightWalletClient.getBlockRange(
            BlockHeightUnsafe.from(
                BlockHeight.new(900_000L)
            )..BlockHeightUnsafe.from(
                BlockHeight.new(950_000L)
            )
        )
        /*
        Fixme: work with flow instead of sequence
        assert(response is Response.Success) { "Server communication failed." }

        Fixme: vtx is not available anymore. Once we'll decide to enable this test again, we could compare a known
             outputs count for the selected blocks range with the calculated outputs count from synced the blocks
        (response as Response.Success).result.forEach { compactBlock ->
            compactBlock.vtx.map { it.outputs.size }.forEach { oCount ->
                outputCounts[oCount] = (outputCounts[oCount] ?: 0) + oCount.coerceAtLeast(1)
                totalOutputs += oCount
            }
            compactBlock.vtx.size.let { count ->
                txCounts[count] = (txCounts[count] ?: 0) + count.coerceAtLeast(1)
                totalTxs += count
            }
        }
         */
        Twig.debug { "txs: $txCounts" }
        Twig.debug { "outputs: $outputCounts" }
        Twig.debug { "total: $totalTxs  $totalOutputs" }
    }
}
