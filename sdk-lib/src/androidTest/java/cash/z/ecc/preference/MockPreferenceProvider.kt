package cash.z.ecc.preference

import cash.z.ecc.android.sdk.internal.storage.preference.api.PreferenceProvider
import cash.z.ecc.android.sdk.internal.storage.preference.model.entry.PreferenceKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * @param mutableMapFactory Emits a new mutable map.  Thread safety depends on the factory implementation.
 */
class MockPreferenceProvider(
    mutableMapFactory: () -> MutableMap<String, String?> = { mutableMapOf() }
) : PreferenceProvider {
    private val map = mutableMapFactory()

    override suspend fun getString(key: PreferenceKey) = map[key.key]

    // For the mock implementation, does not support observability of changes
    override fun observe(key: PreferenceKey): Flow<Unit> = flowOf(Unit)

    override suspend fun hasKey(key: PreferenceKey) = map.containsKey(key.key)

    override suspend fun putString(
        key: PreferenceKey,
        value: String?
    ) {
        map[key.key] = value
    }
}
