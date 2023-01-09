package cash.z.wallet.sdk.sample.demoapp.preference.fixture

import cash.z.ecc.android.sdk.demoapp.preference.model.entry.BooleanPreferenceDefault
import cash.z.ecc.android.sdk.demoapp.preference.model.entry.Key

object BooleanPreferenceDefaultFixture {
    val KEY = Key("some_boolean_key") // $NON-NLS
    fun newTrue() = BooleanPreferenceDefault(KEY, true)
    fun newFalse() = BooleanPreferenceDefault(KEY, false)
}
