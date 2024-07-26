package cash.z.ecc.android.sdk.internal

import cash.z.ecc.android.sdk.internal.model.TorClient
import cash.z.ecc.android.sdk.model.FiatCurrency
import cash.z.ecc.android.sdk.model.FiatCurrencyConversion
import cash.z.ecc.android.sdk.model.FiatCurrencyResult
import kotlinx.datetime.Clock
import java.io.File

internal class UsdExchangeRateFetcher(
    private val torDir: File
) {
    suspend operator fun invoke(): FiatCurrencyResult {
        Twig.info { "Bootstrapping Tor client for fetching exchange rates" }

        val tor =
            try {
                TorClient.new(torDir)
            } catch (e: Exception) {
                Twig.error(e) { "Failed to bootstrap Tor client" }
                return FiatCurrencyResult.Error(exception = e, fiatCurrency = FiatCurrency.USD)
            }

        val rate =
            try {
                tor.getExchangeRateUsd()
            } catch (e: Exception) {
                Twig.error(e) { "Failed to fetch exchange rate through Tor" }
                return FiatCurrencyResult.Error(e, FiatCurrency.USD)
            } finally {
                tor.dispose()
            }

        Twig.info { "Latest USD/ZEC exchange rate is $rate" }

        return FiatCurrencyResult.Success(
            currencyConversion = FiatCurrencyConversion(
                fiatCurrency = FiatCurrency.USD,
                priceOfZec = rate.toDouble(),
                timestamp = Clock.System.now()
            )
        )
    }
}
