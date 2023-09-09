package cash.z.ecc.android.sdk.internal.model

import cash.z.ecc.android.sdk.fixture.ScanProgressFixture
import kotlin.test.Test
import kotlin.test.assertEquals

class ScanProgressTest {
    @Test
    fun get_valid_ratio_test() {
        val scanProgress = ScanProgressFixture.new()
        assertEquals(
            scanProgress.getSafeRation(),
            ScanProgressFixture.DEFAULT_NUMERATOR.toFloat().div(ScanProgressFixture.DEFAULT_DENOMINATOR)
        )
    }

    @Test
    fun get_fallback_ratio_test() {
        val scanProgress = ScanProgressFixture.new(
            denominator = 0
        )
        assertEquals(0f, scanProgress.getSafeRation())
    }
}
