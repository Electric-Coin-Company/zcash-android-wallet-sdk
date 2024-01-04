package cash.z.ecc.android.sdk.demoapp.preference.model.entry

import cash.z.ecc.android.sdk.demoapp.preference.api.PreferenceProvider

data class StringPreferenceDefault(
    override val key: Key,
    private val defaultValue: String
) : PreferenceDefault<String> {
    override suspend fun getValue(preferenceProvider: PreferenceProvider) =
        preferenceProvider.getString(key)
            ?: defaultValue

    override suspend fun putValue(
        preferenceProvider: PreferenceProvider,
        newValue: String
    ) {
        preferenceProvider.putString(key, newValue)
    }
}
