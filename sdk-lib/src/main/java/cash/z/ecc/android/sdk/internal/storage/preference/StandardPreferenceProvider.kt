package cash.z.ecc.android.sdk.internal.storage.preference

import android.content.Context
import cash.z.ecc.android.sdk.internal.storage.preference.api.PreferenceProvider

internal class StandardPreferenceProvider(
    private val context: Context
) : PreferenceHolder() {
    override suspend fun create(): PreferenceProvider =
        AndroidPreferenceProvider.newStandard(context, "cash.z.ecc.android.sdk")
}
