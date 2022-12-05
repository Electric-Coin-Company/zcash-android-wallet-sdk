package cash.z.wallet.sdk.sample.demoapp.preference.fixture

import cash.z.ecc.android.sdk.demoapp.preference.model.entry.IntegerPreferenceDefault
import cash.z.ecc.android.sdk.demoapp.preference.model.entry.Key

object IntegerPreferenceDefaultFixture {
    val KEY = Key("some_string_key") // $NON-NLS
    const val DEFAULT_VALUE = 123
    fun new(key: Key = KEY, value: Int = DEFAULT_VALUE) = IntegerPreferenceDefault(key, value)
}
