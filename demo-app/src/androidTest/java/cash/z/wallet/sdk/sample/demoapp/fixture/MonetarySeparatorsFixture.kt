package cash.z.wallet.sdk.sample.demoapp.fixture

import cash.z.ecc.android.sdk.demoapp.model.MonetarySeparators

object MonetarySeparatorsFixture {
    const val US_GROUPING_SEPARATOR = ','
    const val US_DECIMAL_SEPARATOR = '.'

    fun new(
        grouping: Char = US_GROUPING_SEPARATOR,
        decimal: Char = US_DECIMAL_SEPARATOR
    ) = MonetarySeparators(grouping, decimal)
}
