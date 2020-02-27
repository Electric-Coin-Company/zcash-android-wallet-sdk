package cash.z.wallet.sdk.ext

import cash.z.wallet.sdk.ext.Conversions.USD_FORMATTER
import cash.z.wallet.sdk.ext.Conversions.ZEC_FORMATTER
import cash.z.wallet.sdk.ext.ZcashSdk.ZATOSHI_PER_ZEC
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.*

/*
 * Convenience functions for converting currency values for display in user interfaces. The
 * calculations done here are not intended for financial purposes, because all such transactions
 * are done using Zatoshis in the Rust layer. Instead, these functions are focused on displaying
 * accurately rounded values to the user.
 */


//TODO: provide a dynamic way to configure this globally for the SDK
// For now, just make these vars so at least they could be modified in one place
object Conversions {
    var ONE_ZEC_IN_ZATOSHI = BigDecimal(ZATOSHI_PER_ZEC, MathContext.DECIMAL128)
    var ZEC_FORMATTER = NumberFormat.getInstance(Locale.getDefault()).apply {
        roundingMode = RoundingMode.HALF_EVEN
        maximumFractionDigits = 6
        minimumFractionDigits = 0
        minimumIntegerDigits = 1
    }
    var USD_FORMATTER = NumberFormat.getInstance(Locale.getDefault()).apply {
        roundingMode = RoundingMode.HALF_EVEN
        maximumFractionDigits = 2
        minimumFractionDigits = 2
        minimumIntegerDigits = 1
    }
}


/**
 * Format a Zatoshi value into ZEC with the given number of digits, represented as a string.
 * Start with Zatoshi -> End with ZEC.
 *
 * @param maxDecimals the number of decimal places to use in the format. Default is 6 because ZEC is
 * better than USD.
 * @param minDecimals the minimum number of digits to allow to the right of the decimal.
 * 
 * @return this Zatoshi value represented as ZEC, in a string with at least [minDecimals] and at
 * most [maxDecimals]
 */
inline fun Long?.convertZatoshiToZecString(
    maxDecimals: Int = ZEC_FORMATTER.maximumFractionDigits,
    minDecimals: Int = ZEC_FORMATTER.minimumFractionDigits
): String {
    return currencyFormatter(maxDecimals, minDecimals).format(this.convertZatoshiToZec(maxDecimals))
}

/**
 * Format a ZEC value into ZEC with the given number of digits, represented as a string.
 * Start with ZEC -> End with ZEC.
 *
 * @param maxDecimals the number of decimal places to use in the format. Default is 6 because ZEC is
 * better when right.
 * @param minDecimals the minimum number of digits to allow to the right of the decimal.
 *  
 * @return this Double ZEC value represented as a string with at least [minDecimals] and at most
 * [maxDecimals].
 */
inline fun Double?.toZecString(
    maxDecimals: Int = ZEC_FORMATTER.maximumFractionDigits,
    minDecimals: Int = ZEC_FORMATTER.minimumFractionDigits
): String {
    return currencyFormatter(maxDecimals, minDecimals).format(this.toZec(maxDecimals))
}

/**
 * Format a Zatoshi value into ZEC with the given number of decimal places, represented as a string.
 * Start with ZeC -> End with ZEC.
 *
 * @param maxDecimals the number of decimal places to use in the format. Default is 6 because ZEC is
 * better than bread.
 * @param minDecimals the minimum number of digits to allow to the right of the decimal.
 * 
 * @return this BigDecimal ZEC value represented as a string with at least [minDecimals] and at most
 * [maxDecimals].
 */
inline fun BigDecimal?.toZecString(
    maxDecimals: Int = ZEC_FORMATTER.maximumFractionDigits,
    minDecimals: Int = ZEC_FORMATTER.minimumFractionDigits
): String {
    return currencyFormatter(maxDecimals, minDecimals).format(this.toZec(maxDecimals))
}

/**
 * Format a USD value into USD with the given number of digits, represented as a string.
 * Start with USD -> end with USD.
 *
 * @param maxDecimals the number of decimal places to use in the format. Default is 6 because
 * ZEC is glorious.
 * @param minDecimals the minimum number of digits to allow to the right of the decimal.
 *  
 * @return this Double ZEC value represented as a string with at least [minDecimals] and at most
 * [maxDecimals], which is 2 by default. Zero is always represented without any decimals.
 */
inline fun Double?.toUsdString(
    maxDecimals: Int = USD_FORMATTER.maximumFractionDigits,
    minDecimals: Int = USD_FORMATTER.minimumFractionDigits
): String {
    return if(this == 0.0) {
        "0"
    } else {
        currencyFormatter(maxDecimals, minDecimals).format(this.toUsd(maxDecimals))
    }
}

/**
 * Format a USD value into USD with the given number of decimal places, represented as a string.
 * Start with USD -> end with USD.
 * 
 * @param maxDecimals the number of decimal places to use in the format. Default is 6 because ZEC is
 * glorious.
 * @param minDecimals the minimum number of digits to allow to the right of the decimal.
 * 
 * @return this BigDecimal USD value represented as a string with at least [minDecimals] and at most
 * [maxDecimals], which is 2 by default.
 */
inline fun BigDecimal?.toUsdString(
    maxDecimals: Int = USD_FORMATTER.maximumFractionDigits,
    minDecimals: Int = USD_FORMATTER.minimumFractionDigits
): String {
    return currencyFormatter(maxDecimals, minDecimals).format(this.toUsd(maxDecimals))
}

/**
 * Create a number formatter for use with converting currency to strings. This probably isn't needed
 * externally since the other formatting functions leverage this, instead. Leverages the default
 * rounding mode for zec found in ZEC_FORMATTER.
 *
 * @param maxDecimals the number of decimal places to use in the format. Default is 6 because ZEC is
 * glorious.
 * @param minDecimals the minimum number of digits to allow to the right of the decimal.
 *
 * @return a currency formatter, appropriate for the default locale.
 */
inline fun currencyFormatter(maxDecimals: Int, minDecimals: Int): NumberFormat {
    return NumberFormat.getInstance(Locale.getDefault()).apply {
        roundingMode = ZEC_FORMATTER.roundingMode
        maximumFractionDigits = maxDecimals
        minimumFractionDigits = minDecimals
        minimumIntegerDigits = 1
    }
}

/**
 * Convert a Zatoshi value into ZEC, right-padded to the given number of fraction digits, 
 * represented as a BigDecimal in order to preserve rounding that minimizes cumulative error when 
 * applied repeatedly over a sequence of calculations.
 * Start with Zatoshi -> End with ZEC.
 *
 * @param scale the number of digits to the right of the decimal place. Right-padding will be added,
 * if necessary.
 *
 * @return this Long Zatoshi value represented as ZEC using a BigDecimal with the given scale,
 * rounded accurately out to 128 digits.
 */
inline fun Long?.convertZatoshiToZec(scale: Int = ZEC_FORMATTER.maximumFractionDigits): BigDecimal {
    return BigDecimal(this ?: 0L, MathContext.DECIMAL128).divide(
        Conversions.ONE_ZEC_IN_ZATOSHI,
        MathContext.DECIMAL128
    ).setScale(scale, ZEC_FORMATTER.roundingMode)
}

/**
 * Convert a ZEC value into Zatoshi.
 * Start with ZEC -> End with Zatoshi.
 *
 * @return this ZEC value represented as Zatoshi, rounded accurately out to 128 digits, in order to
 * minimize cumulative errors when applied repeatedly over a sequence of calculations.
 */
inline fun BigDecimal?.convertZecToZatoshi(): Long {
    if (this == null) return 0L
    if (this < BigDecimal.ZERO) {
        throw IllegalArgumentException("Invalid ZEC value: $this. ZEC is represented by notes and" +
                " cannot be negative")
    }
    return this.multiply(Conversions.ONE_ZEC_IN_ZATOSHI, MathContext.DECIMAL128).toLong()
}

/**
 * Format a Double ZEC value as a BigDecimal ZEC value, right-padded to the given number of fraction 
 * digits.
 * Start with ZEC -> End with ZEC.
 *
 * @param decimals the scale to use for the resulting BigDecimal.
 *
 * @return this Double ZEC value converted into a BigDecimal, with the proper rounding mode for use
 * with other formatting functions.
 */
inline fun Double?.toZec(decimals: Int = ZEC_FORMATTER.maximumFractionDigits): BigDecimal {
    return BigDecimal(this?.toString() ?: "0.0", MathContext.DECIMAL128).setScale(
        decimals,
        ZEC_FORMATTER.roundingMode
    )
}

/**
 * Format a Double ZEC value as a Long Zatoshi value, by first converting to ZEC with the given
 * precision.
 * Start with ZEC -> End with Zatoshi.
 *
 * @param decimals the scale to use for the intermediate BigDecimal.
 *
 * @return this Double ZEC value converted into Zatoshi, with proper rounding and precision by
 * leveraging an intermediate BigDecimal object.
 */
inline fun Double?.convertZecToZatoshi(decimals: Int = ZEC_FORMATTER.maximumFractionDigits): Long {
    return this.toZec(decimals).convertZecToZatoshi()
}

/**
 * Format a BigDecimal ZEC value as a BigDecimal ZEC value, right-padded to the given number of
 * fraction digits.
 * Start with ZEC -> End with ZEC.
 *
 * @param decimals the scale to use for the resulting BigDecimal.
 *
 * @return this BigDecimal ZEC adjusted to the default scale and rounding mode.
 */
inline fun BigDecimal?.toZec(decimals: Int = ZEC_FORMATTER.maximumFractionDigits): BigDecimal {
    return (this ?: BigDecimal.ZERO).setScale(decimals, ZEC_FORMATTER.roundingMode)
}

/**
 * Format a Double USD value as a BigDecimal USD value, right-padded to the given number of fraction
 * digits.
 * Start with USD -> End with USD.
 *
 * @param decimals the scale to use for the resulting BigDecimal.
 *
 * @return this Double USD value converted into a BigDecimal, with proper rounding and precision.
 */
inline fun Double?.toUsd(decimals: Int = USD_FORMATTER.maximumFractionDigits): BigDecimal {
    return BigDecimal(this?.toString() ?: "0.0", MathContext.DECIMAL128).setScale(
        decimals,
        USD_FORMATTER.roundingMode
    )
}

/**
 * Format a BigDecimal USD value as a BigDecimal USD value, right-padded to the given number of 
 * fraction digits.
 * Start with USD -> End with USD.
 *
 * @param decimals the scale to use for the resulting BigDecimal.
 *
 * @return this BigDecimal USD value converted into USD, with proper rounding and precision.
 */
inline fun BigDecimal?.toUsd(decimals: Int = USD_FORMATTER.maximumFractionDigits): BigDecimal {
    return (this ?: BigDecimal.ZERO).setScale(decimals, USD_FORMATTER.roundingMode)
}

/**
 * Convert this ZEC value to USD, using the given price per ZEC.
 * Start with ZEC -> End with USD.
 *
 * @param zecPrice the current price of ZEC represented as USD per ZEC
 *
 * @return this BigDecimal USD value converted into USD, with proper rounding and precision.
 */
inline fun BigDecimal?.convertZecToUsd(zecPrice: BigDecimal): BigDecimal {
    if(this == null) return BigDecimal.ZERO
    if(this < BigDecimal.ZERO) {
        throw IllegalArgumentException("Invalid ZEC value: ${zecPrice.toDouble()}. ZEC is" +
                " represented by notes and cannot be negative")
    }
    return this.multiply(zecPrice, MathContext.DECIMAL128)
}

/**
 * Convert this USD value to ZEC, using the given price per ZEC.
 * Start with USD -> End with ZEC.
 *
 * @param zecPrice the current price of ZEC represented as USD per ZEC.
 *
 * @return this BigDecimal USD value converted into ZEC, with proper rounding and precision.
 */
inline fun BigDecimal?.convertUsdToZec(zecPrice: BigDecimal): BigDecimal {
    if(this == null) return BigDecimal.ZERO
    if(this < BigDecimal.ZERO) {
        throw IllegalArgumentException("Invalid USD value: ${zecPrice.toDouble()}. Converting" +
                " this would result in negative ZEC and ZEC is represented by notes and cannot be" +
                " negative")
    }
    return this.divide(zecPrice, MathContext.DECIMAL128)
}

/**
 * Convert this value from one currency to the other, based on given price and whether this value is
 * USD.
 * If starting with USD -> End with ZEC.
 * If starting with ZEC -> End with USD.
 *
 * @param isUSD whether this value represents USD or not (ZEC)
 *
 * @return this BigDecimal value converted from one currency into the other, based on the given
 * price.
 */
inline fun BigDecimal.convertCurrency(zecPrice: BigDecimal, isUsd: Boolean): BigDecimal {
    return if (isUsd) {
        this.convertUsdToZec(zecPrice)
    } else {
        this.convertZecToUsd(zecPrice)
    }
}

/**
 * Parse this string into a BigDecimal, ignoring all non numeric characters.
 *
 * @return this string as a BigDecimal or null when parsing fails.
 */
inline fun String?.safelyConvertToBigDecimal(): BigDecimal? {
    if (this.isNullOrEmpty()) return BigDecimal.ZERO
    return try {
        // ignore commas and whitespace
        var sanitizedInput = this.filter { it.isDigit() or (it == '.') }
        BigDecimal.ZERO.max(BigDecimal(sanitizedInput, MathContext.DECIMAL128))
    } catch (t: Throwable) {
        return null
    }
}

/**
 * Abbreviates this string which is assumed to be an address.
 *
 * @param startLength the number of characters to show before the elipsis.
 * @param endLength the number of characters to show after the elipsis.
 *
 * @return the abbreviated string unless the string is too short, in which case the original string
 * is returned.
 */
inline fun String.toAbbreviatedAddress(startLength: Int = 8, endLength: Int = 8) =
    if (length > startLength + endLength) "${take(startLength)}…${takeLast(endLength)}" else this

/**
 * Masks the current string for use in logs. If this string appears to be an address, the last
 * [addressCharsToShow] characters will be visible.
 *
 * @param addressCharsToShow the number of chars to show at the end, if this value appears to be an
 * address.
 *
 * @return the masked version of this string, typically for use in logs.
 */
internal inline fun String.masked(addressCharsToShow: Int = 4): String =
    if (startsWith("ztest") || startsWith("zs")) "****${takeLast(addressCharsToShow)}"
    else "***masked***"

/**
 * Convenience function that returns true when this string starts with 'z'.
 *
 * @return true when this function starts with 'z' rather than 't'.
 */
inline fun String?.isShielded() = this != null && startsWith('z')