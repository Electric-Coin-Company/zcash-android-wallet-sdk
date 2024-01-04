package cash.z.ecc.android.sdk.demoapp.preference

import cash.z.ecc.android.sdk.demoapp.preference.api.PreferenceProvider
import cash.z.ecc.android.sdk.demoapp.preference.model.entry.Key
import cash.z.ecc.android.sdk.demoapp.preference.model.entry.PreferenceDefault
import cash.z.ecc.android.sdk.model.PersistableWallet
import org.json.JSONObject

data class PersistableWalletPreferenceDefault(
    override val key: Key
) : PreferenceDefault<PersistableWallet?> {
    override suspend fun getValue(preferenceProvider: PreferenceProvider) =
        preferenceProvider.getString(key)?.let { PersistableWallet.from(JSONObject(it)) }

    override suspend fun putValue(
        preferenceProvider: PreferenceProvider,
        newValue: PersistableWallet?
    ) = preferenceProvider.putString(key, newValue?.toJson()?.toString())
}
