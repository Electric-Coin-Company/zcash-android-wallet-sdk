package cash.z.wallet.sdk.data

import java.lang.IllegalStateException
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class SampleSpendingKeyProvider(private val seedValue: String) : ReadWriteProperty<Any?, String> {
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): String {
        // dynamically generating keys, based on seed is out of scope for this sample
        if(seedValue != "dummyseed") throw IllegalStateException("This sample key provider only supports the dummy seed")
        return "secret-extended-key-test1q0f0urnmqqqqpqxlree5urprcmg9pdgvr2c88qhm862etv65eu84r9zwannpz4g88299xyhv7wf9xkecag653jlwwwyxrymfraqsnz8qfgds70qjammscxxyl7s7p9xz9w906epdpy8ztsjd7ez7phcd5vj7syx68sjskqs8j9lef2uuacghsh8puuvsy9u25pfvcdznta33qe6xh5lrlnhdkgymnpdug4jm6tpf803cad6tqa9c0ewq9l03fqxatevm97jmuv8u0ccxjews5"
    }
}