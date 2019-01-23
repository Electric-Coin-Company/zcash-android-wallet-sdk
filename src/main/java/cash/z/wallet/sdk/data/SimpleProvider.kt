package cash.z.wallet.sdk.data

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class SimpleProvider<T>(var value: T) : ReadWriteProperty<Any?, T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return value
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
    }
}
