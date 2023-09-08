package cash.z.ecc.android.sdk.darkside.reorgs

import androidx.test.ext.junit.runners.AndroidJUnit4
import cash.z.ecc.android.sdk.darkside.test.DarksideTestCoordinator
import cash.z.ecc.android.sdk.darkside.test.ScopedTest
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.ZcashNetwork
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReorgSmallTest : ScopedTest() {

    private val targetHeight = BlockHeight.new(
        ZcashNetwork.Mainnet,
        663250
    )
    private val hashBeforeReorg = "09ec0d5de30d290bc5a2318fbf6a2427a81c7db4790ce0e341a96aeac77108b9"
    private val hashAfterReorg = "tbd"

    @Before
    fun setup() {
        // sithLord.await()
    }

    @Test
    @Ignore("Temporarily disabled")
    fun testBeforeReorg_latestBlockHash() = timeout(30_000L) {
        // validator.validateBlockHash(targetHeight, hashBeforeReorg)
    }

    @Test
    @Ignore("Temporarily disabled")
    fun testAfterReorg_callbackTriggered() = timeout(30_000L) {
        hadReorg = false
//        sithLord.triggerSmallReorg()
//         sithLord.await()
        assertTrue(hadReorg)
    }

    @Test
    @Ignore("Temporarily disabled")
    fun testAfterReorg_latestBlockHash() = timeout(30_000L) {
        // validator.validateBlockHash(targetHeight, hashAfterReorg)
    }

    companion object {

        private val sithLord = DarksideTestCoordinator()
        private val validator = sithLord.validator
        private var hadReorg = false

        @BeforeClass
        @JvmStatic
        fun startOnce() {
            sithLord.enterTheDarkside()
            validator.onReorg { _, _ ->
                hadReorg = true
            }
        }
    }
}
