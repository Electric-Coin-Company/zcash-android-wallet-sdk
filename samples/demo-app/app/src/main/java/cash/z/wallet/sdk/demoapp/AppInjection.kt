package cash.z.wallet.sdk.demoapp

import cash.z.wallet.sdk.jni.RustBackend
import cash.z.wallet.sdk.jni.RustBackendWelding
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * This file represents the dependencies that are specific to this demo. Normally, a dependency
 * injection framework like Dagger would provide these objects. For the sake of simplicity, we // TODO finish explaining
 */
object AppInjection {

    fun provideRustBackend(prefix: String): RustBackendWelding {
        return RustBackend.create(App.instance, "${prefix}_Cache.db", "${prefix}_Data.db")
    }
    
    /**
     * A sample class that pretends to securely accept a value, store it and return it later. In
     * practice, a wallet maker may have a way of securely storing data.
     */
    class Vault(var value: String = "") : ReadWriteProperty<Any?, String> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): String = value
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
            this.value = value
        }
    }
}

