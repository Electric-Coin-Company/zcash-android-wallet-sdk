package cash.z.ecc.android.sdk.model

import kotlinx.datetime.Instant

/**
 * Represents a snapshot in time of a currency conversion rate.
 *
 * @param fiatCurrency The fiat currency for this conversion.
 * @param timestamp The timestamp when this conversion was obtained. This value is returned by
 * the server so it shouldn't have issues with client-side clock inaccuracy.
 * @param priceOfZec The conversion rate of ZEC to the fiat currency.
 */
data class FiatCurrencyConversion(
    val fiatCurrency: FiatCurrency,
    val timestamp: Instant,
    val priceOfZec: Double
) {
    init {
        require(priceOfZec > 0) { "priceOfZec must be greater than 0" }
        require(priceOfZec.isFinite()) { "priceOfZec must be finite" }
    }

    companion object
}
