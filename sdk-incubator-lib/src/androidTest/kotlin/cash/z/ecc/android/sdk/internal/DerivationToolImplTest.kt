package cash.z.ecc.android.sdk.internal

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
    fun testDerivedArbitraryAccountKey() =
        runTest {
            val key =
                DerivationTool.getInstance().deriveArbitraryAccountKey(
                    contextString = CONTEXT.toByteArray(),
                    seed = seedPhrase.toByteArray(),
                    network = network,
                    account = account,
                )
            assertEquals(Base64.encode(key), "byyNHiMfj8N2tiCHc4Mv/0ts0IuUqDPe99MvW8B03IY=")
        }

    @OptIn(ExperimentalEncodingApi::class)
    @Test
    fun testDerivedArbitraryWalletKey() =
        runTest {
            val key =
                DerivationTool.getInstance().deriveArbitraryWalletKey(
                    contextString = CONTEXT.toByteArray(),
                    seed = seedPhrase.toByteArray(),
                )
            assertEquals(Base64.encode(key), "1xm/qCXZqXgiRFT1IVk97+5gv9BE5AjkOVWHwYU9RYQ=")
        }
}

private const val CONTEXT = "ZashiAddressBookEncryptionV1"
