package cash.z.wallet.sdk.sample.demoapp.preference

import androidx.test.filters.SmallTest
import cash.z.ecc.android.sdk.demoapp.preference.EncryptedPreferenceKeys
import cash.z.ecc.android.sdk.demoapp.preference.PersistableWalletPreferenceDefault
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import kotlin.reflect.full.memberProperties

class EncryptedPreferenceKeysTest {
    // This test is primary to prevent copy-paste errors in preference keys
    @SmallTest
    @Test
    fun key_values_unique() {
        val fieldValueSet = mutableSetOf<String>()

        EncryptedPreferenceKeys::class
            .memberProperties
            .map { it.getter.call(EncryptedPreferenceKeys) }
            .map { it as PersistableWalletPreferenceDefault }
            .map { it.key }
            .forEach {
                assertThat("Duplicate key $it", fieldValueSet.contains(it.key), equalTo(false))

                fieldValueSet.add(it.key)
            }
    }
}
