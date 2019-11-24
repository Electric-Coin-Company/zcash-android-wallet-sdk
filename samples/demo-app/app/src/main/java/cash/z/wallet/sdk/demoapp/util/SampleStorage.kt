package cash.z.wallet.sdk.demoapp.util

import android.content.Context
import cash.z.wallet.sdk.demoapp.App


@Deprecated(
    message = "Do not use this! It is insecure and only intended for demo purposes to " +
            "show how to bridge to an existing key storage mechanism. Instead, use the Android " +
            "Keystore system or a 3rd party library that leverages it."
)
class SampleStorage {

    private val prefs = 
        App.instance.getSharedPreferences("ExtremelyInsecureStorage", Context.MODE_PRIVATE)
    
    fun saveSensitiveString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun loadSensitiveString(key: String): String? = prefs.getString(key, null)

    fun saveSensitiveBytes(key: String, value: ByteArray) {
        saveSensitiveString(key, value.toString(Charsets.ISO_8859_1))
    }

    fun loadSensitiveBytes(key: String): ByteArray? =
        prefs.getString(key, null)?.toByteArray(Charsets.ISO_8859_1)
}

/**
 * Simple demonstration of how to take existing code that securely stores data and bridge it into
 * the SDK. This class delegates to the storage object. For demo purposes, we're using an insecure
 * SampleStorage implementation but this can easily be swapped for a truly secure storage solution.
 */
class SampleStorageBridge() {
    private val delegate = SampleStorage()

    /**
     * Just a sugar method to help with being explicit in sample code. We want to show developers
     * our intention that they write simple bridges to secure storage components.
     */
    fun securelyStoreSeed(seed: ByteArray): SampleStorageBridge {
        delegate.saveSensitiveBytes(KEY_SEED, seed)
        return this
    }

    /**
     * Just a sugar method to help with being explicit in sample code. We want to show developers
     * our intention that they write simple bridges to secure storage components.
     */
    fun securelyStorePrivateKey(key: String): SampleStorageBridge {
        delegate.saveSensitiveString(KEY_PK, key)
        return this
    }

     val seed: ByteArray get() = delegate.loadSensitiveBytes(KEY_SEED)!!
     val key get() = delegate.loadSensitiveString(KEY_PK)!!

    companion object {
        private const val KEY_SEED = "cash.z.wallet.sdk.demoapp.SEED"
        private const val KEY_PK = "cash.z.wallet.sdk.demoapp.PK"
    }
}