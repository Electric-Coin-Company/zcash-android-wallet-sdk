package cash.z.ecc.android.sdk.demoapp.preference.model.entry

import cash.z.ecc.android.sdk.demoapp.preference.api.PreferenceProvider

data class BooleanPreferenceDefault(
    override val key: Key,
    private val defaultValue: Boolean
) : PreferenceDefault<Boolean> {
    @Suppress("SwallowedException")
    override suspend fun getValue(preferenceProvider: PreferenceProvider) =
        preferenceProvider.getString(key)?.let {
            try {
                it.toBooleanStrict()
            } catch (e: IllegalArgumentException) {
                // TODO [#32]: Log coercion failure instead of just silently returning default
                // TODO [#32]: https://github.com/zcash/zcash-android-wallet-sdk/issues/32
                defaultValue
            }
        } ?: defaultValue

    override suspend fun putValue(
        preferenceProvider: PreferenceProvider,
        newValue: Boolean
    ) {
        preferenceProvider.putString(key, newValue.toString())
    }
}
