package cash.z.ecc.android.sdk.model

data class ObserveFiatCurrencyResult(
    val isLoading: Boolean = true,
    val currencyConversion: FiatCurrencyConversion? = null,
)
