package cash.z.ecc.android.sdk.model

import androidx.test.filters.SmallTest
import cash.z.ecc.android.sdk.count
import cash.z.ecc.android.sdk.fixture.PersistableWalletFixture
import cash.z.ecc.android.sdk.fixture.SeedPhraseFixture
import co.electriccoin.lightwallet.client.model.LightWalletEndpoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PersistableWalletTest {
    @Test
    @SmallTest
    fun serialize() {
        val persistableWallet = PersistableWalletFixture.new()

        val jsonObject = persistableWallet.toJson()
        assertEquals(7, jsonObject.keys().count())
        assertTrue(jsonObject.has(PersistableWallet.KEY_VERSION))
        assertTrue(jsonObject.has(PersistableWallet.KEY_NETWORK_ID))
        assertTrue(jsonObject.has(PersistableWallet.KEY_ENDPOINT_HOST))
        assertTrue(jsonObject.has(PersistableWallet.KEY_ENDPOINT_PORT))
        assertTrue(jsonObject.has(PersistableWallet.KEY_ENDPOINT_IS_SECURE))
        assertTrue(jsonObject.has(PersistableWallet.KEY_SEED_PHRASE))
        assertTrue(jsonObject.has(PersistableWallet.KEY_BIRTHDAY))
        assertTrue(!jsonObject.has(PersistableWallet.KEY_IS_TOR_ENABLED))

        assertEquals(PersistableWallet.VERSION_3, jsonObject.getInt(PersistableWallet.KEY_VERSION))
        assertEquals(ZcashNetwork.Mainnet.id, jsonObject.getInt(PersistableWallet.KEY_NETWORK_ID))
        assertEquals(
            PersistableWalletFixture.SEED_PHRASE.joinToString(),
            jsonObject.getString(PersistableWallet.KEY_SEED_PHRASE)
        )
        assertEquals(PersistableWalletFixture.ENDPOINT.host, jsonObject.getString(PersistableWallet.KEY_ENDPOINT_HOST))
        assertEquals(PersistableWalletFixture.ENDPOINT.port, jsonObject.getInt(PersistableWallet.KEY_ENDPOINT_PORT))
        assertEquals(
            PersistableWalletFixture.ENDPOINT.isSecure,
            jsonObject.getBoolean(PersistableWallet.KEY_ENDPOINT_IS_SECURE)
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

    @Test
    @SmallTest
    fun get_seed_phrase_test() {
        val json = PersistableWalletFixture.new().toJson()
        assertEquals(
            PersistableWalletFixture.SEED_PHRASE.joinToString(),
            PersistableWallet.getSeedPhrase(json)
        )
    }

    @Test
    @SmallTest
    fun get_birthday_test() {
        val json = PersistableWalletFixture.new().toJson()
        assertEquals(
            PersistableWalletFixture.SEED_PHRASE.joinToString(),
            PersistableWallet.getSeedPhrase(json)
        )
    }

    @Test
    @SmallTest
    fun get_network_test() {
        val json = PersistableWalletFixture.new().toJson()
        assertEquals(
            PersistableWalletFixture.BIRTHDAY.value,
            PersistableWallet.getBirthday(json)!!.value
        )
    }

    @Test
    @SmallTest
    fun get_endpoint_test() {
        val json = PersistableWalletFixture.new().toJson()
        assertEquals(
            PersistableWalletFixture.ENDPOINT.host,
            PersistableWallet.getEndpoint(json).host
        )
        assertEquals(
            PersistableWalletFixture.ENDPOINT.port,
            PersistableWallet.getEndpoint(json).port
        )
        assertEquals(
            PersistableWalletFixture.ENDPOINT.isSecure,
            PersistableWallet.getEndpoint(json).isSecure
        )
    }

    @Test
    @SmallTest
    fun version_1_2_migration_test() {
        val json = PersistableWalletFixture.persistVersionOne()
        assertEquals(
            PersistableWallet.VERSION_1,
            PersistableWallet.getVersion(json)
        )

        // Wallet version one deserialized by code supporting version two
        val persistableWallet = PersistableWallet.from(json)
        assertEquals(
            getLightWalletEndpointForNetwork(persistableWallet.network),
            persistableWallet.endpoint
        )
    }

    @Test
    @SmallTest
    fun requireSecureMainnet() {
        assertTrue(LightWalletEndpoint.Mainnet.isSecure)
    }

    @Test
    @SmallTest
    fun requireSecureTestnet() {
        assertTrue(LightWalletEndpoint.Testnet.isSecure)
    }
}
