package cash.z.ecc.android.sdk.internal.model

import cash.z.ecc.android.sdk.fixture.ScanRangeFixture
import cash.z.ecc.android.sdk.internal.ext.isNotEmpty
import cash.z.ecc.android.sdk.internal.ext.length
import cash.z.ecc.android.sdk.model.ZcashNetwork
import kotlin.test.Test
import kotlin.test.assertTrue

class ScanRangeTest {
    @Test
    fun get_suggest_scan_range_priority_test() {
        val scanRange = ScanRangeFixture.new(
            priority = SuggestScanRangePriority.Verify.priority
        )
        assertTrue {
            scanRange.getSuggestScanRangePriority() == SuggestScanRangePriority.Verify
        }
    }

    @Test
    fun scan_range_boundaries_test() {
        val scanRange = ScanRangeFixture.new(
            range = ZcashNetwork.Testnet.saplingActivationHeight..ZcashNetwork.Testnet.saplingActivationHeight + 9
        )
        assertTrue { scanRange.range.isNotEmpty() }
        assertTrue { scanRange.range.length() == 10L }
    }
}
