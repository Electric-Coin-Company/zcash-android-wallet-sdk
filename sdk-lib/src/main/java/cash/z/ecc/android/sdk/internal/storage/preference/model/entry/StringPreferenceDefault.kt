package cash.z.ecc.android.sdk.internal.storage.preference.model.entry

import cash.z.ecc.android.sdk.internal.storage.preference.api.PreferenceProvider

internal data class StringPreferenceDefault(
    override val key: PreferenceKey,
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
