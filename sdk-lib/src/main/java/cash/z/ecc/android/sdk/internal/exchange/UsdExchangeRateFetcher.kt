package cash.z.ecc.android.sdk.internal.exchange

import cash.z.ecc.android.sdk.internal.Twig
import cash.z.ecc.android.sdk.internal.model.TorClient
import cash.z.ecc.android.sdk.model.FetchFiatCurrencyResult
import cash.z.ecc.android.sdk.model.FiatCurrencyConversion
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import java.io.File

internal class UsdExchangeRateFetcher(private val torDir: File) {
    @Suppress("TooGenericExceptionCaught", "ReturnCount")
    suspend operator fun invoke(): FetchFiatCurrencyResult =
        retry {
            val tor =
                try {
                    Twig.info { "[USD] Tor client bootstrap" }
                    TorClient.new(torDir)
                } catch (e: Exception) {
                    Twig.error(e) { "[USD] To client bootstrap failed" }
                    return@retry FetchFiatCurrencyResult.Error(exception = e)
                }

            val rate =
                try {
                    Twig.info { "[USD] Fetch start" }
                    tor.getExchangeRateUsd()
                } catch (e: Exception) {
                    Twig.error(e) { "[USD] Fetch failed" }
                    return@retry FetchFiatCurrencyResult.Error(e)
                } finally {
                    tor.dispose()
                }

            Twig.debug { "[USD] Fetch success" }

            return@retry FetchFiatCurrencyResult.Success(
                currencyConversion =
                    FiatCurrencyConversion(
                        priceOfZec = rate.toDouble(),
                        timestamp = Clock.System.now()
                    )
            )
        }

    /**
     * Retry with geometric order.
     */
    @Suppress("TooGenericExceptionCaught", "ReturnCount", "SwallowedException")
    private suspend fun <T> retry(
        times: Int = 3,
        initialDelay: Long = 1000,
        multiplier: Double = 2.0,
        block: suspend () -> T,
    ): T {
        var currentDelay = initialDelay
        repeat(times - 1) {
            try {
                return block()
            } catch (e: Exception) {
                Twig.debug(e) { "[USD] Fetching attempt failed" }
            }
            delay(currentDelay)
            currentDelay = (currentDelay * multiplier).toLong()
        }
        return block() // last attempt
    }
}
