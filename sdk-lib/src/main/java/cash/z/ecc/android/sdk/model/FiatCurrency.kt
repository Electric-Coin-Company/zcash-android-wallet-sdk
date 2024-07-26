package cash.z.ecc.android.sdk.model

import android.icu.util.Currency

data class FiatCurrency(val code: String) {

    val symbol: String
        get() = Currency.getInstance(code).symbol

    init {
        check(isAlpha3Code(code))
    }

    companion object Factory {
        val USD = FiatCurrency("USD")

        /**
         * Checks whether [code] complies with ISO 4217 3-letter code.
         */
        fun isAlpha3Code(code: String): Boolean {
            if (code.length != 3) {
                return false
            } else {
                for (i in 0..2) {
                    val ch = code[i]
                    if (ch < 'A' || (ch in '['..'`') || ch > 'z') {
                        return false
                    }
                }
            }
            return true
        }
    }
}
