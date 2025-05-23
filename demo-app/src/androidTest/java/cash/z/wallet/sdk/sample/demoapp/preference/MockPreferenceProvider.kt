package cash.z.wallet.sdk.sample.demoapp.preference

import cash.z.ecc.android.sdk.demoapp.preference.api.PreferenceProvider
import cash.z.ecc.android.sdk.demoapp.preference.model.entry.Key
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * @param mutableMapFactory Emits a new mutable map.  Thread safety depends on the factory implementation.
 */
class MockPreferenceProvider(
    mutableMapFactory: () -> MutableMap<String, String?> = { mutableMapOf() }
) : PreferenceProvider {
    private val map = mutableMapFactory()

    override suspend fun getString(key: Key) = map[key.key]

    // For the mock implementation, does not support observability of changes
    override fun observe(key: Key): Flow<Unit> = flowOf(Unit)

    override suspend fun hasKey(key: Key) = map.containsKey(key.key)

    override suspend fun putString(
        key: Key,
        value: String?
    ) {
        map[key.key] = value
    }
}
