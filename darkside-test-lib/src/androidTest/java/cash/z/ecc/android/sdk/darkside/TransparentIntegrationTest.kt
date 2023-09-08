package cash.z.ecc.android.sdk.darkside

import androidx.test.ext.junit.runners.AndroidJUnit4
import cash.z.ecc.android.sdk.darkside.test.DarksideTest
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration test to run in order to catch any regressions in transparent behavior.
 */
// TODO [#1224]: Refactor and re-enable disabled darkside tests
// TODO [#1224]: https://github.com/zcash/zcash-android-wallet-sdk/issues/1224
@RunWith(AndroidJUnit4::class)
class TransparentIntegrationTest : DarksideTest() {
    @Before
    fun setup() = runOnce {
        // sithLord.await()
    }

    @Test
    @Ignore("This test is broken")
    fun sanityTest() {
        validator.validateTxCount(5)
    }
}
