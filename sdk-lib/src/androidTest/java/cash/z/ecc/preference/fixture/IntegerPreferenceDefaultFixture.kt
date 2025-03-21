package cash.z.ecc.preference.fixture

import cash.z.ecc.android.sdk.internal.storage.preference.model.entry.IntegerPreferenceDefault
import cash.z.ecc.android.sdk.internal.storage.preference.model.entry.PreferenceKey

object IntegerPreferenceDefaultFixture {
    val KEY = PreferenceKey("some_string_key") // $NON-NLS
    const val DEFAULT_VALUE = 123

    fun new(
        key: PreferenceKey = KEY,
        value: Int = DEFAULT_VALUE
    ) = IntegerPreferenceDefault(key, value)
}
