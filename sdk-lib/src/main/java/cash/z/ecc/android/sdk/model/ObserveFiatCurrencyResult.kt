package cash.z.ecc.android.sdk.model

data class ObserveFiatCurrencyResult(
    val isLoading: Boolean = false,
    val currencyConversion: FiatCurrencyConversion? = null,
)
