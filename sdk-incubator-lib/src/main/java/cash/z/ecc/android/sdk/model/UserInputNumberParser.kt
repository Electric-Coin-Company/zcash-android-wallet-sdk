package cash.z.ecc.android.sdk.model

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object UserInputNumberParser {

    /**
     * Normalizes [input] string by replacing grouping separators by decimal separators derived from [locale].
     */
    fun normalizeInput(input: String, locale: Locale): String {
        val symbols = DecimalFormatSymbols(locale)
        return input.replace(symbols.groupingSeparator, symbols.decimalSeparator)
    }

    /**
     * Convert user's [input] to a number of type [BigDecimal]. Decimal separator is derived from [locale].
     *
     * @return [BigDecimal] if [input] is a valid number representation, null otherwise.
     */
    fun toBigDecimalOrNull(input: String, locale: Locale): BigDecimal? {
        val symbols = DecimalFormatSymbols(locale)

        if (!isValidNumericWithOptionalDecimalSeparator(input = input, symbols = symbols)) return null

        val decimalFormat = DecimalFormat().apply {
            this.decimalFormatSymbols = symbols
            this.isParseBigDecimal = true
        }

        return try {
            when (val parsedNumber = decimalFormat.parse(input)) {
                null -> null
                is BigDecimal -> parsedNumber
                is Int -> BigDecimal(parsedNumber)
                is Float -> BigDecimal(parsedNumber.toDouble())
                is Double -> BigDecimal(parsedNumber)
                is Long -> BigDecimal(parsedNumber)
                is Short -> BigDecimal(parsedNumber.toLong())
                else -> BigDecimal(parsedNumber.toString())
            }
        } catch (e: NumberFormatException) {
            return null
        }
    }

    private fun isValidNumericWithOptionalDecimalSeparator(input: String, symbols: DecimalFormatSymbols): Boolean {
        val decimalSeparator = when (val separator = symbols.decimalSeparator) {
            '.' -> "\\."
            else -> separator.toString()
        }
        val regex =
            Regex("^(?:\\d+$decimalSeparator?\\d*|$decimalSeparator\\d+)$")
        return regex.matches(input)
    }
}
