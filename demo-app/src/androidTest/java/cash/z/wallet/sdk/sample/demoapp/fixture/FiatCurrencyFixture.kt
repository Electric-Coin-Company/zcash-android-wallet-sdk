package cash.z.wallet.sdk.sample.demoapp.fixture

import cash.z.ecc.android.sdk.demoapp.model.FiatCurrency

object FiatCurrencyFixture {
    const val USD = "USD"

    fun new(code: String = USD) = FiatCurrency(code)
}
