package cash.z.ecc.android.sdk.internal.ext

import android.content.Context
import android.os.Build
import android.provider.Settings
import cash.z.ecc.android.sdk.internal.Twig
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

@Deprecated(message = InsecureWarning.MESSAGE)
class SampleSpendingKeyProvider(
    private val seedValue: String
) : ReadWriteProperty<Any?, String> {
    override fun setValue(
        thisRef: Any?,
        property: KProperty<*>,
        value: String
    ) {
        Twig.debug { "Set value called on property: $property, with value: $value." }
    }

    override fun getValue(
        thisRef: Any?,
        property: KProperty<*>
    ): String {
        // dynamically generating keys, based on seed is out of scope for this sample
        if (seedValue != "dummyseed") {
            error("This sample provider only supports the dummy seed")
        }
        return "secret-extended-key-test1q0f0urnmqqqqpqxlree5urprcmg9pdgvr2c88qhm862etv65eu84r9zwannpz4g88299xyhv7wf9" +
            "xkecag653jlwwwyxrymfraqsnz8qfgds70qjammscxxyl7s7p9xz9w906epdpy8ztsjd7ez7phcd5vj7syx68sjskqs8j9lef2uu" +
            "acghsh8puuvsy9u25pfvcdznta33qe6xh5lrlnhdkgymnpdug4jm6tpf803cad6tqa9c0ewq9l03fqxatevm97jmuv8u0ccxjews5"
    }
}

@Deprecated(message = InsecureWarning.MESSAGE)
class SampleSeedProvider(
    val seed: ByteArray
) : ReadOnlyProperty<Any?, ByteArray> {
    constructor(seedValue: String) : this(seedValue.toByteArray())

    override fun getValue(
        thisRef: Any?,
        property: KProperty<*>
    ): ByteArray = seed
}

@Deprecated(message = InsecureWarning.MESSAGE)
class BlockingSeedProvider(
    val seed: ByteArray,
    val delay: Long = 5000L
) : ReadOnlyProperty<Any?, ByteArray> {
    constructor(seedValue: String, delayMillis: Long = 5000L) : this(seedValue.toByteArray(), delayMillis)

    override fun getValue(
        thisRef: Any?,
        property: KProperty<*>
    ): ByteArray {
        Thread.sleep(delay)
        return seed
    }
}

@Deprecated(message = InsecureWarning.MESSAGE)
class SimpleProvider<T>(
    var value: T
) : ReadWriteProperty<Any?, T> {
    override fun getValue(
        thisRef: Any?,
        property: KProperty<*>
    ): T = value

    override fun setValue(
        thisRef: Any?,
        property: KProperty<*>,
        value: T
    ) {
        this.value = value
    }
}

@Deprecated(message = InsecureWarning.MESSAGE)
class BlockingProvider<T>(
    var value: T,
    val delay: Long = 5000L
) : ReadWriteProperty<Any?, T> {
    override fun getValue(
        thisRef: Any?,
        property: KProperty<*>
    ): T {
        Thread.sleep(delay)
        return value
    }

    override fun setValue(
        thisRef: Any?,
        property: KProperty<*>,
        value: T
    ) {
        Thread.sleep(delay)
        this.value = value
    }
}
//
// @Deprecated(message = InsecureWarning.message)
// class SampleKeyManager(val sampleSeed: ByteArray) : Wallet.KeyManager {
//    override lateinit var key: String
//    override val seed: ByteArray get() = sampleSeed
// }

/**
 * This is intentionally insecure. Wallet makers have told us storing keys is their specialty so we don't put a lot of
 * energy here. A true implementation would create a key using user interaction, perhaps with a password they know that
 * is never stored, along with requiring user authentication for key use (i.e. fingerprint/PIN/pattern/etc). From there,
 * one of these approaches might be helpful to store the key securely:
 *
 * https://developer.android.com/training/articles/keystore.html
 * https://github.com/scottyab/AESCrypt-Android/blob/master/aescrypt/src/main/java/com/scottyab/aescrypt/AESCrypt.java
 * https://github.com/iamMehedi/Secured-Preference-Store
 */
@Suppress("HardwareIds", "UtilityClassWithPublicConstructor")
@Deprecated(message = InsecureWarning.MESSAGE)
class SeedGenerator {
    companion object {
        @Deprecated(message = InsecureWarning.MESSAGE)
        fun getDeviceId(appContext: Context): String {
            val id =
                Build.FINGERPRINT + Settings.Secure.getString(appContext.contentResolver, Settings.Secure.ANDROID_ID)
            return id.replace("\\W".toRegex(), "_")
        }
    }
}

internal object InsecureWarning {
    const val MESSAGE =
        "Do not use this because it is insecure and only intended for test code and samples. " +
            "Instead, use the Android Keystore system or a 3rd party library that leverages it."
}
