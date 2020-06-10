package cash.z.ecc.android.sdk.integration

import cash.z.ecc.android.sdk.ext.ScopedTest
import cash.z.ecc.android.sdk.ext.twig
import cash.z.ecc.android.sdk.util.DarksideTestCoordinator
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

class ReorgSmallTest : ScopedTest() {

    private val targetHeight = 663250
    private val hashBeforeReorg = "09ec0d5de30d290bc5a2318fbf6a2427a81c7db4790ce0e341a96aeac77108b9"
    private val hashAfterReorg = "tbd"

    @Before
    fun setup() {
        sithLord.await()
    }

    @Test
    fun testBeforeReorg_latestBlockHash() = timeout(30_000L) {
        validator.validateBlockHash(targetHeight, hashBeforeReorg)
    }

    @Test
    fun testAfterReorg_callbackTriggered() = timeout(30_000L) {
        hadReorg = false
//        sithLord.triggerSmallReorg()
        sithLord.await()

        twig("checking whether a reorg happened (spoiler: ${if (hadReorg) "yep" else "nope"})")
        assertTrue(hadReorg)
    }

    @Test
    fun testAfterReorg_latestBlockHash() = timeout(30_000L) {
        validator.validateBlockHash(targetHeight, hashAfterReorg)
    }

    companion object {

        private val sithLord = DarksideTestCoordinator("192.168.1.134")
        private val validator = sithLord.validator
        private var hadReorg = false

        @BeforeClass
        @JvmStatic
        fun startOnce() {
            sithLord.enterTheDarkside()
            validator.onReorg { _, _ ->
                hadReorg = true
            }
            sithLord.synchronizer.start(classScope)
        }
    }

}

