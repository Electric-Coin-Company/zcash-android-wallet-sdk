package cash.z.ecc.android.sdk.internal

import cash.z.ecc.android.sdk.ext.toHex
import cash.z.ecc.android.sdk.fixture.WalletFixture
import cash.z.ecc.android.sdk.model.Account
import cash.z.ecc.android.sdk.model.ZcashNetwork
import cash.z.ecc.android.sdk.tool.DerivationTool
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.assertEquals

class DerivationToolImplTest {
    private val seedPhrase = WalletFixture.Alice.seedPhrase
    private val network = ZcashNetwork.Mainnet
    private val account = Account.DEFAULT

    @OptIn(ExperimentalEncodingApi::class)
    @Test
    fun testAliceDerivedArbitraryAccountKeyInBase64() =
        runTest {
            val key =
                DerivationTool.getInstance().deriveArbitraryAccountKey(
                    contextString = CONTEXT.toByteArray(),
                    seed = seedPhrase.toByteArray(),
                    network = network,
                    account = account,
                )
            assertEquals("byyNHiMfj8N2tiCHc4Mv/0ts0IuUqDPe99MvW8B03IY=", Base64.encode(key))
        }

    @OptIn(ExperimentalEncodingApi::class)
    @Test
    fun testAliceDerivedArbitraryWalletKeyInBase64() =
        runTest {
            val key =
                DerivationTool.getInstance().deriveArbitraryWalletKey(
                    contextString = CONTEXT.toByteArray(),
                    seed = seedPhrase.toByteArray(),
                )
            assertEquals("1xm/qCXZqXgiRFT1IVk97+5gv9BE5AjkOVWHwYU9RYQ=", Base64.encode(key),)
        }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testVectorDerivedArbitraryWalletKey() =
        runTest {
            val secretKey =
                DerivationTool.getInstance().deriveArbitraryWalletKey(
                    contextString = "5a63617368207465737420766563746f7273".hexToByteArray(),
                    seed = "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f".hexToByteArray(),
                )
            assertEquals("e9da8806409dc3c3ebd1fc2a71c879c13dd7aa93ede803bf1a83414b9d3b158a", secretKey.toHex())
        }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testVectorDerivedArbitraryAccountKey() =
        runTest {
            val secretKey =
                DerivationTool.getInstance().deriveArbitraryAccountKey(
                    contextString = "5a63617368207465737420766563746f7273".hexToByteArray(),
                    seed = "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f".hexToByteArray(),
                    network = network,
                    account = account,
                )
            assertEquals("bf60078362a09234fcbc6bf6c8a87bde9fc73776bf93f37adbcc439a85574a9a", secretKey.toHex())
        }
}

private const val CONTEXT = "ZashiAddressBookEncryptionV1"
