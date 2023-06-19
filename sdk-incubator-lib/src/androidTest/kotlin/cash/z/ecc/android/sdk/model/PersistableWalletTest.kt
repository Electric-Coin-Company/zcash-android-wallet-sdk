package cash.z.ecc.android.sdk.model

import androidx.test.filters.SmallTest
import cash.z.ecc.android.sdk.count
import cash.z.ecc.android.sdk.fixture.PersistableWalletFixture
import cash.z.ecc.android.sdk.fixture.SeedPhraseFixture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PersistableWalletTest {
    @Test
    @SmallTest
    fun serialize() {
        val persistableWallet = PersistableWalletFixture.new()

        val jsonObject = persistableWallet.toJson()
        assertEquals(4, jsonObject.keys().count())
        assertTrue(jsonObject.has(PersistableWallet.KEY_VERSION))
        assertTrue(jsonObject.has(PersistableWallet.KEY_NETWORK_ID))
        assertTrue(jsonObject.has(PersistableWallet.KEY_SEED_PHRASE))
        assertTrue(jsonObject.has(PersistableWallet.KEY_BIRTHDAY))

        assertEquals(1, jsonObject.getInt(PersistableWallet.KEY_VERSION))
        assertEquals(ZcashNetwork.Testnet.id, jsonObject.getInt(PersistableWallet.KEY_NETWORK_ID))
        assertEquals(
            PersistableWalletFixture.SEED_PHRASE.joinToString(),
            jsonObject.getString(PersistableWallet.KEY_SEED_PHRASE)
        )

        // Birthday serialization is tested in a separate file
    }

    @Test
    @SmallTest
    fun round_trip() {
        val persistableWallet = PersistableWalletFixture.new()

        val deserialized = PersistableWallet.from(persistableWallet.toJson())

        assertEquals(persistableWallet, deserialized)
        assertFalse(persistableWallet === deserialized)
    }

    @Test
    @SmallTest
    fun toString_security() {
        val actual = PersistableWalletFixture.new().toString()

        assertFalse(actual.contains(SeedPhraseFixture.SEED_PHRASE))
    }
}
