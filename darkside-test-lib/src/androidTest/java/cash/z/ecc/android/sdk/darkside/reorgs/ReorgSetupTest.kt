package cash.z.ecc.android.sdk.darkside.reorgs

import androidx.test.ext.junit.runners.AndroidJUnit4
import cash.z.ecc.android.sdk.darkside.test.DarksideTestCoordinator
import cash.z.ecc.android.sdk.darkside.test.ScopedTest
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

// TODO [#1224]: Refactor and re-enable disabled darkside tests
// TODO [#1224]: https://github.com/zcash/zcash-android-wallet-sdk/issues/1224
@RunWith(AndroidJUnit4::class)
class ReorgSetupTest : ScopedTest() {

    /*
    private val birthdayHeight = BlockHeight.new(ZcashNetwork.Mainnet, 663150)
    private val targetHeight = BlockHeight.new(ZcashNetwork.Mainnet, 663250)
     */

    @Before
    fun setup() {
        // sithLord.await()
    }

    @Test
    @Ignore("Temporarily disabled")
    fun testBeforeReorg_minHeight() = timeout(30_000L) {
        // validate that we are synced, at least to the birthday height
        // validator.validateMinHeightSynced(birthdayHeight)
    }

    @Test
    @Ignore("Temporarily disabled")
    fun testBeforeReorg_maxHeight() = timeout(30_000L) {
        // validate that we are not synced beyond the target height
        // validator.validateMaxHeightSynced(targetHeight)
    }

    companion object {

        private val sithLord = DarksideTestCoordinator()
        private val validator = sithLord.validator

        @BeforeClass
        @JvmStatic
        fun startOnce() {
            sithLord.enterTheDarkside()
        }
    }
}
