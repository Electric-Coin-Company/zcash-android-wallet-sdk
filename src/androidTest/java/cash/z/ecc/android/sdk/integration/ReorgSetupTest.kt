package cash.z.ecc.android.sdk.integration

import cash.z.ecc.android.sdk.ext.ScopedTest
import cash.z.ecc.android.sdk.util.DarksideTestCoordinator
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

class ReorgSetupTest : ScopedTest() {

    private val birthdayHeight = 663150
    private val targetHeight = 663250

    @Before
    fun setup() {
        sithLord.await()
    }

    @Test
    fun testBeforeReorg_minHeight() = timeout(30_000L) {
        // validate that we are synced, at least to the birthday height
        validator.validateMinHeightDownloaded(birthdayHeight)
    }

    @Test
    fun testBeforeReorg_maxHeight() = timeout(30_000L) {
        // validate that we are not synced beyond the target height
        validator.validateMaxHeightScanned(targetHeight)
    }

    companion object {

        private val sithLord = DarksideTestCoordinator("192.168.1.134")
        private val validator = sithLord.validator

        @BeforeClass
        @JvmStatic
        fun startOnce() {
            sithLord.enterTheDarkside()
            sithLord.synchronizer.start(classScope)
        }
    }

}

