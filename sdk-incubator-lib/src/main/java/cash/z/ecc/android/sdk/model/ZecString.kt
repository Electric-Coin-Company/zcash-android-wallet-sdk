package cash.z.ecc.android.sdk.model

import android.content.Context
import android.icu.text.DecimalFormat
import android.icu.text.NumberFormat
import cash.z.ecc.android.sdk.ext.convertZatoshiToZecString
import cash.z.ecc.android.sdk.ext.convertZecToZatoshi
import java.text.DecimalFormatSymbols
import java.text.ParseException

object ZecString {
    fun allowedCharacters(monetarySeparators: MonetarySeparators) =
        buildSet<Char> {
            add('0')
            add('1')
            add('2')
            add('3')
            add('4')
            add('5')
            add('6')
            add('7')
            add('8')
            add('9')
            add(monetarySeparators.decimal)
            if (monetarySeparators.isGroupingValid()) {
                add(monetarySeparators.grouping)
            }
        }
}

data class MonetarySeparators(
    val grouping: Char,
    val decimal: Char
) {
    companion object {
        /**
         * @param locale Preferred Locale for the returned monetary separators. If Locale is not provided, the
         * default one will be used.
         *
         * @return The current localized monetary separators.  Do not cache this value, as it
         * can change if the system Locale changes.
         */
        fun current(locale: java.util.Locale? = null): MonetarySeparators {
            val decimalFormatSymbols =
                locale?.let {
                    DecimalFormatSymbols.getInstance(locale)
                } ?: DecimalFormatSymbols.getInstance()

            return MonetarySeparators(
                decimalFormatSymbols.groupingSeparator,
                decimalFormatSymbols.monetaryDecimalSeparator
            )
        }
    }

    fun isGroupingValid() = this.grouping.isDefined() && this.grouping != this.decimal
}

private const val DECIMALS = 8

// TODO [#412]: https://github.com/zcash/zcash-android-wallet-sdk/issues/412
// The SDK needs to fix the API for currency conversion
fun Zatoshi.toZecString() = convertZatoshiToZecString(DECIMALS, DECIMALS)

const val FRACTION_DIGITS = 2

/*
 * ZEC is our own currency, so there's not going to be an existing localization that matches it perfectly.
 *
 * To ensure consistent behavior regardless of user Locale, use US localization except that we swap out the
 * separator characters based on the user's current Locale.  This should avoid unexpected surprises
 * while also localizing the separator format.
 */

/**
 * @return [zecString] parsed into Zatoshi or null if parsing failed.
 */
fun Zatoshi.Companion.fromZecString(
    context: Context,
    zecString: String,
    locale: Locale = Locale.getDefault(),
): Zatoshi? {
    if (!ZecStringExt.filterConfirm(
            context = context,
            separators = MonetarySeparators.current(locale.toJavaLocale()),
            zecString = zecString
        )
    ) {
        return null
    }

    val decimalFormat =
        DecimalFormat.getInstance(locale.toJavaLocale(), NumberFormat.NUMBERSTYLE).apply {
            // TODO [#343]: https://github.com/zcash/secant-android-wallet/issues/343
            roundingMode = android.icu.math.BigDecimal.ROUND_HALF_EVEN // aka Bankers rounding
            maximumFractionDigits = FRACTION_DIGITS
            minimumFractionDigits = FRACTION_DIGITS
        }

    val doubleValue =
        try {
            decimalFormat.parse(zecString).toDouble()
        } catch (e: ParseException) {
            null
        }

    @Suppress("SwallowedException")
    return try {
        doubleValue.convertZecToZatoshi()
    } catch (e: IllegalArgumentException) {
        null
    }
}
