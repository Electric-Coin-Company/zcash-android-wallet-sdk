package cash.z.ecc.android.sdk.integration.darkside

import cash.z.ecc.android.sdk.annotation.MaintainedTest
import cash.z.ecc.android.sdk.annotation.TestPurpose.DARKSIDE
import cash.z.ecc.android.sdk.annotation.TestPurpose.REGRESSION
import cash.z.ecc.android.sdk.ext.DarksideTest
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

/**
 * Integration test to run in order to catch any regressions in transparent behavior.
 */
@MaintainedTest(DARKSIDE, REGRESSION)
class TransparentIntegrationTest : DarksideTest() {
    @Before
    fun setup() = runOnce {
        sithLord.await()
    }

    @Test
    @Ignore("This test is broken")
    fun sanityTest() {
        validator.validateTxCount(5)
    }
}
