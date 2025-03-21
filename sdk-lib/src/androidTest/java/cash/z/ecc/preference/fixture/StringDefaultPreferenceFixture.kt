package cash.z.ecc.preference.fixture

import cash.z.ecc.android.sdk.internal.storage.preference.model.entry.PreferenceKey
import cash.z.ecc.android.sdk.internal.storage.preference.model.entry.StringPreferenceDefault

object StringDefaultPreferenceFixture {
    val KEY = PreferenceKey("some_string_key") // $NON-NLS
    const val DEFAULT_VALUE = "some_default_value" // $NON-NLS

    fun new(
        key: PreferenceKey = KEY,
        value: String = DEFAULT_VALUE
    ) = StringPreferenceDefault(key, value)
}
