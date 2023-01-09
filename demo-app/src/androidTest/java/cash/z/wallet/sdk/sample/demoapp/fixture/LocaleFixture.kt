package cash.z.wallet.sdk.sample.demoapp.fixture

import cash.z.ecc.android.sdk.demoapp.model.Locale

object LocaleFixture {
    const val LANGUAGE = "en"
    const val COUNTRY = "US"
    val VARIANT: String? = null

    fun new(
        language: String = LANGUAGE,
        country: String? = COUNTRY,
        variant: String? = VARIANT
    ) = Locale(language, country, variant)
}
