package cash.z.ecc.android.sdk.model

import org.junit.Test
import kotlin.test.assertTrue

class LightWalletEndpointTest {
    @Test
    fun requireSecureMainnet() {
        assertTrue(LightWalletEndpoint.Mainnet.isSecure)
    }

    @Test
    fun requireSecureTestnet() {
        assertTrue(LightWalletEndpoint.Testnet.isSecure)
    }
}
