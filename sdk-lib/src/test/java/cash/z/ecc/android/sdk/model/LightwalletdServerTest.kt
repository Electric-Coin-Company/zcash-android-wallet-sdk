package cash.z.ecc.android.sdk.model

import org.junit.Test
import kotlin.test.assertTrue

class LightwalletdServerTest {
    @Test
    fun requireSecureMainnet() {
        assertTrue(LightwalletdServer.Mainnet.isSecure)
    }

    @Test
    fun requireSecureTestnet() {
        assertTrue(LightwalletdServer.Testnet.isSecure)
    }
}
