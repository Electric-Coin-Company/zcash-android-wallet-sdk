package cash.z.wallet.sdk.sample.demoapp.model

import androidx.test.filters.SmallTest
import cash.z.ecc.android.sdk.demoapp.model.SeedPhrase
import cash.z.wallet.sdk.sample.demoapp.fixture.SeedPhraseFixture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SeedPhraseTest {
    @Test
    @SmallTest
    fun split_and_join() {
        val seedPhrase = SeedPhrase.new(SeedPhraseFixture.SEED_PHRASE)

        assertEquals(SeedPhraseFixture.SEED_PHRASE, seedPhrase.joinToString())
    }

    @Test
    @SmallTest
    fun security() {
        val seedPhrase = SeedPhraseFixture.new()
        seedPhrase.split.forEach {
            assertFalse(seedPhrase.toString().contains(it))
        }
    }
}
