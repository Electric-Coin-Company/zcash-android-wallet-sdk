package cash.z.ecc.android.sdk.fixture

import cash.z.ecc.android.sdk.internal.model.ScanRange
import cash.z.ecc.android.sdk.internal.model.SuggestScanRangePriority
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.ZcashNetwork

object ScanRangeFixture {
    internal val DEFAULT_CLOSED_RANGE =
        ZcashNetwork.Testnet.saplingActivationHeight..ZcashNetwork.Testnet.saplingActivationHeight + 9
    internal val DEFAULT_PRIORITY = SuggestScanRangePriority.Verify.priority

    internal fun new(
        range: ClosedRange<BlockHeight> = DEFAULT_CLOSED_RANGE,
        priority: Long = DEFAULT_PRIORITY
    ) = ScanRange(range, priority)
}
