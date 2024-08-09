package cash.z.ecc.android.sdk.model

import android.content.Context
import cash.z.ecc.android.sdk.ext.convertUsdToZec
import cash.z.ecc.android.sdk.ext.convertZecToZatoshi
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.ParseException
import java.util.Locale

fun FiatCurrencyConversion.toZatoshi(
    context: Context,
    value: String,
    monetarySeparators: MonetarySeparators = MonetarySeparators.current(Locale.getDefault()),
): Zatoshi? {
    if (!ZecStringExt.filterConfirm(context, monetarySeparators, value)) {
        return null
    }

    val symbols =
        DecimalFormatSymbols.getInstance(Locale.US).apply {
            this.decimalSeparator = monetarySeparators.decimal
            if (monetarySeparators.isGroupingValid()) {
                this.groupingSeparator = monetarySeparators.grouping
            }
        }

    val localizedPattern =
        if (monetarySeparators.isGroupingValid()) {
            "#${monetarySeparators.grouping}##0${monetarySeparators.decimal}0#"
        } else {
            "0${monetarySeparators.decimal}0#"
        }

    val decimalFormat =
        DecimalFormat(localizedPattern, symbols).apply {
            isParseBigDecimal = true
            roundingMode = RoundingMode.HALF_EVEN // aka Bankers rounding
        }

    // TODO [#343]: https://github.com/zcash/secant-android-wallet/issues/343
    val bigDecimal =
        try {
            decimalFormat.parse(value) as BigDecimal
        } catch (e: NumberFormatException) {
            null
        } catch (e: ParseException) {
            null
        }

    @Suppress("SwallowedException")
    return try {
        bigDecimal.convertUsdToZec(priceOfZec.toBigDecimal()).toDouble()
            .convertZecToZatoshi()
    } catch (e: IllegalArgumentException) {
        null
    }
}
