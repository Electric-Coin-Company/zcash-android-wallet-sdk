package cash.z.wallet.sdk.data

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class SampleSeedProvider(val seedValue: String) : ReadOnlyProperty<Any?, ByteArray> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): ByteArray {
        return seedValue.toByteArray()
    }
}