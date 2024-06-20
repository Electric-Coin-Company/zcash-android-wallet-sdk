package cash.z.ecc.android.sdk.model

import android.content.Context
import cash.z.ecc.android.sdk.ext.convertZatoshiToZecString
import cash.z.ecc.android.sdk.ext.convertZecToZatoshi
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.ParseException
import java.util.Locale

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

data class MonetarySeparators(val grouping: Char, val decimal: Char) {
    companion object {
        /**
         * @param locale Preferred Locale for the returned monetary separators. If Locale is not provided, the
         * default one will be used.
         *
         * @return The current localized monetary separators.  Do not cache this value, as it
         * can change if the system Locale changes.
         */
        fun current(locale: Locale? = null): MonetarySeparators {
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
@SuppressWarnings("ReturnCount")
fun Zatoshi.Companion.fromZecString(
    context: Context,
    zecString: String,
    monetarySeparators: MonetarySeparators
): Zatoshi? {
    if (!ZecStringExt.filterConfirm(context, monetarySeparators, zecString)) {
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
            decimalFormat.parse(zecString) as BigDecimal
        } catch (e: NumberFormatException) {
            null
        } catch (e: ParseException) {
            null
        }

    @Suppress("SwallowedException")
    return try {
        bigDecimal.convertZecToZatoshi()
    } catch (e: IllegalArgumentException) {
        null
    }
}
