package cash.z.ecc.android.sdk.model

sealed interface FiatCurrencyResult {

    val fiatCurrency: FiatCurrency

    data class Loading(
        override val fiatCurrency: FiatCurrency = FiatCurrency.USD
    ): FiatCurrencyResult

    data class Success(val currencyConversion: FiatCurrencyConversion) : FiatCurrencyResult {
        override val fiatCurrency: FiatCurrency
            get() = currencyConversion.fiatCurrency
    }

    data class Error(
        val exception: Exception,
        override val fiatCurrency: FiatCurrency = FiatCurrency.USD
    ) : FiatCurrencyResult
}
