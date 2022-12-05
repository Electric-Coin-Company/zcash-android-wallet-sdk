package cash.z.wallet.sdk.sample.demoapp.model

import androidx.test.filters.SmallTest
import cash.z.ecc.android.sdk.demoapp.model.Locale
import cash.z.ecc.android.sdk.demoapp.model.toJavaLocale
import cash.z.ecc.android.sdk.demoapp.model.toKotlinLocale
import org.junit.Test
import kotlin.test.assertEquals

class LocaleTest {
    @Test
    @SmallTest
    fun toKotlinLocale() {
        val javaLocale = java.util.Locale.forLanguageTag("en-US")

        val kotlinLocale = javaLocale.toKotlinLocale()
        assertEquals("en", kotlinLocale.language)
        assertEquals("US", kotlinLocale.region)
        assertEquals(null, kotlinLocale.variant)
    }

    @Test
    @SmallTest
    fun toJavaLocale() {
        val kotlinLocale = Locale("en", "US", null)
        val javaLocale = kotlinLocale.toJavaLocale()
        assertEquals("en-US", javaLocale.toLanguageTag())
    }
}
