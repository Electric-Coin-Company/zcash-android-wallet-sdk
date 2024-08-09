package cash.z.ecc.android.sdk.fixture

import cash.z.ecc.android.sdk.model.FiatCurrency

object FiatCurrencyFixture {
    const val USD = "USD"

    fun new(code: String = USD) = FiatCurrency(code)
}
