package cash.z.ecc.android.sdk.fixture

import cash.z.ecc.android.sdk.model.FiatCurrencyConversion
import kotlinx.datetime.Instant
import kotlinx.datetime.toInstant

object CurrencyConversionFixture {
    val TIMESTAMP = "2022-07-08T11:51:44Z".toInstant()
    const val PRICE_OF_ZEC = 54.98

    fun new(
        timestamp: Instant = TIMESTAMP,
        priceOfZec: Double = PRICE_OF_ZEC
    ) = FiatCurrencyConversion(timestamp, priceOfZec)
}
