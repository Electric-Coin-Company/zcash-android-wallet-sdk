package cash.z.wallet.sdk.demoapp.util

import android.content.Context
import cash.z.wallet.sdk.demoapp.App
import cash.z.wallet.sdk.secure.Wallet


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
 * the KeyManager interface. This class implements the interface by delegating to the storage
 * object. For demo purposes, we're using an insecure SampleStorage implementation but this can
 * easily be swapped for a true storage solution.
 */
class SampleStorageBridge(): Wallet.KeyManager {
    private val KEY_SEED = "cash.z.wallet.sdk.demoapp.SEED"
    private val KEY_PK = "cash.z.wallet.sdk.demoapp.PK"
    private val delegate = SampleStorage()

    constructor(seed: ByteArray) : this() {
        securelyStoreSeed(seed)
    }

    fun securelyStoreSeed(seed: ByteArray): SampleStorageBridge {
        delegate.saveSensitiveBytes(KEY_SEED, seed)
        return this
    }

    override val seed: ByteArray get() = delegate.loadSensitiveBytes(KEY_SEED)!!
    override var key: String
        get() = delegate.loadSensitiveString(KEY_PK)!!
        set(value) {
            delegate.saveSensitiveString(KEY_PK, value)
        }
}