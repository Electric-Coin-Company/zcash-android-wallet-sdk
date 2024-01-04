package cash.z.ecc.android.sdk.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ZcashNetworkTest {
    @Test
    fun is_mainnet_succeed_test() {
        assertTrue { ZcashNetwork.Mainnet.isMainnet() }
    }

    @Test
    fun is_mainnet_fail_test() {
        assertFalse { ZcashNetwork.Testnet.isMainnet() }
    }

    @Test
    fun is_testnet_succeed_test() {
        assertTrue { ZcashNetwork.Testnet.isTestnet() }
    }

    @Test
    fun is_testnet_fail_test() {
        assertFalse { ZcashNetwork.Mainnet.isTestnet() }
    }
}
