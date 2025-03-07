package cash.z.ecc.android.sdk.model

import android.content.Context
import android.icu.math.BigDecimal
import android.icu.text.DecimalFormat
import android.icu.text.NumberFormat
import cash.z.ecc.android.sdk.ext.convertUsdToZec
import cash.z.ecc.android.sdk.ext.convertZecToZatoshi
import java.text.ParseException

fun FiatCurrencyConversion.toZatoshi(
    context: Context,
    value: String,
    locale: Locale = Locale.getDefault(),
): Zatoshi? {
    if (!ZecStringExt.filterConfirm(
            context = context,
            separators = MonetarySeparators.current(locale.toJavaLocale()),
            zecString = value
        )
    ) {
        return null
    }

    val decimalFormat =
        DecimalFormat.getInstance(locale.toJavaLocale(), NumberFormat.NUMBERSTYLE).apply {
            // TODO [#343]: https://github.com/zcash/secant-android-wallet/issues/343
            roundingMode = BigDecimal.ROUND_HALF_EVEN // aka Bankers rounding
            maximumFractionDigits = FRACTION_DIGITS
            minimumFractionDigits = FRACTION_DIGITS
        }

    val doubleValue =
        try {
            decimalFormat.parse(value).toDouble()
        } catch (e: ParseException) {
            null
        }

    @Suppress("SwallowedException")
    return try {
        doubleValue
            .convertUsdToZec(priceOfZec)
            .convertZecToZatoshi()
    } catch (e: IllegalArgumentException) {
        null
    }
}
