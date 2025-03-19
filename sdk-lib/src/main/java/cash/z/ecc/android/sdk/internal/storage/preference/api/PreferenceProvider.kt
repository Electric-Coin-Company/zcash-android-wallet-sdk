package cash.z.ecc.android.sdk.internal.storage.preference.api

import cash.z.ecc.android.sdk.internal.storage.preference.model.entry.PreferenceKey
import kotlinx.coroutines.flow.Flow

interface PreferenceProvider {
    suspend fun hasKey(key: PreferenceKey): Boolean

    suspend fun putString(
        key: PreferenceKey,
        value: String?
    )

    suspend fun getString(key: PreferenceKey): String?

    /**
     * @return Flow to observe potential changes to the value associated with the key in the preferences.
     * Consumers of the flow will need to then query the value and determine whether it has changed.
     */
    fun observe(key: PreferenceKey): Flow<Unit>

    suspend fun clearPreferences(): Boolean
}
