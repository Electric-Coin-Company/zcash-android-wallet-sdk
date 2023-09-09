package cash.z.ecc.android.sdk.fixture

import cash.z.ecc.android.sdk.internal.model.ScanProgress

object ScanProgressFixture {
    internal const val DEFAULT_NUMERATOR = 50L
    internal const val DEFAULT_DENOMINATOR = 100L

    internal fun new(
        numerator: Long = DEFAULT_NUMERATOR,
        denominator: Long = DEFAULT_DENOMINATOR
    ) = ScanProgress(numerator, denominator)
}
