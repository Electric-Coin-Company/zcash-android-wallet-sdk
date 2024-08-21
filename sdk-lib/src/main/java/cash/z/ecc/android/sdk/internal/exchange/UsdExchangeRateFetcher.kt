package cash.z.ecc.android.sdk.internal.exchange

import cash.z.ecc.android.sdk.internal.Twig
import cash.z.ecc.android.sdk.internal.model.TorClient
import cash.z.ecc.android.sdk.model.FetchFiatCurrencyResult
import cash.z.ecc.android.sdk.model.FiatCurrencyConversion
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import java.io.File

internal class UsdExchangeRateFetcher(torDir: File) {
    private val torHolder = TorClientHolder(torDir)

    @Suppress("TooGenericExceptionCaught", "ReturnCount")
    suspend operator fun invoke(): FetchFiatCurrencyResult {
        return retry {
            val rate =
                try {
                    Twig.info { "[USD] Fetch start" }
                    torHolder().getExchangeRateUsd()
                } catch (e: Exception) {
                    Twig.error(e) { "[USD] Fetch failed" }
                    return FetchFiatCurrencyResult.Error(e)
                }

            Twig.debug { "[USD] Fetch success: $rate" }

            FetchFiatCurrencyResult.Success(
                currencyConversion =
                    FiatCurrencyConversion(
                        priceOfZec = rate.toDouble(),
                        timestamp = Clock.System.now()
                    )
            )
        }
    }

    suspend fun dispose() {
        torHolder.dispose()
    }

    /**
     * Retry with geometric order.
     */
    @Suppress("TooGenericExceptionCaught", "ReturnCount", "SwallowedException")
    private suspend inline fun <T> retry(
        times: Int = 3,
        initialDelay: Long = 1000,
        multiplier: Double = 2.0,
        block: () -> T,
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

private class TorClientHolder(private val torDir: File) {
    private val mutex = Mutex()
    private var torClient: TorClient? = null

    suspend operator fun invoke(): TorClient =
        mutex.withLock {
            if (torClient == null) {
                torClient = TorClient.new(torDir)
            }
            return torClient!!
        }

    suspend fun dispose() =
        mutex.withLock {
            torClient?.dispose()
        }
}
