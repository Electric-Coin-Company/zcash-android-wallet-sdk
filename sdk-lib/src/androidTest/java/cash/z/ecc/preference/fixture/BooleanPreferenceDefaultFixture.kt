package cash.z.ecc.preference.fixture

import cash.z.ecc.android.sdk.internal.storage.preference.model.entry.BooleanPreferenceDefault
import cash.z.ecc.android.sdk.internal.storage.preference.model.entry.PreferenceKey

object BooleanPreferenceDefaultFixture {
    val KEY = PreferenceKey("some_boolean_key") // $NON-NLS

    fun newTrue() = BooleanPreferenceDefault(KEY, true)

    fun newFalse() = BooleanPreferenceDefault(KEY, false)
}
