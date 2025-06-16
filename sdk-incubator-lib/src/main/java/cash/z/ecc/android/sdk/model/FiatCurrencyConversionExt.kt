package cash.z.ecc.android.sdk.model

import cash.z.ecc.android.sdk.ext.convertUsdToZec
import cash.z.ecc.android.sdk.ext.convertZecToZatoshi
import java.math.BigDecimal

fun FiatCurrencyConversion.toZatoshi(amount: BigDecimal): Zatoshi? {
    @Suppress("SwallowedException")
    return try {
        amount
            .convertUsdToZec(BigDecimal(priceOfZec))
            .convertZecToZatoshi()
    } catch (e: IllegalArgumentException) {
        null
    }
}
