package cash.z.ecc.android.sdk.internal.storage.preference

import android.content.Context
import cash.z.ecc.android.sdk.internal.storage.preference.api.PreferenceProvider

internal class EncryptedPreferenceProvider(
    private val context: Context
) : PreferenceHolder() {
    override suspend fun create(): PreferenceProvider =
        AndroidPreferenceProvider.newEncrypted(context, "cash.z.ecc.android.sdk.encrypted")
}
