package cash.z.wallet.sdk.ext

import java.util.*

/**
 * Format a Zatoshi value into Zec with the given number of decimal places.
 *
 * @param decimalPlaces the number of decimal places to use in the format. Default is 3 because Zec is better than Usd.
 */
inline fun Long?.toZec(decimalPlaces: Int = 3): String {
    val amount = (this ?: 0L)/100000000.0
    return String.format(Locale.getDefault(), "%,.${decimalPlaces}f", amount)
}

/**
 * Format a double as a dollar amount.
 *
 * @param includeSymbol whether or not to include the $ symbol
 */
inline fun Double?.toUsd(includeSymbol: Boolean = true): String {
    val amount = this ?: 0.0
    val symbol = if (includeSymbol) "$" else ""
    return if (amount < 0) {
        String.format(Locale.getDefault(), "-$symbol%,.2f", Math.abs(amount))
    } else {
        String.format(Locale.getDefault(), "$symbol%,.2f", amount)
    }
}

internal inline fun String.masked(): String = if (startsWith("ztest")) "****${takeLast(4)}" else "***masked***"