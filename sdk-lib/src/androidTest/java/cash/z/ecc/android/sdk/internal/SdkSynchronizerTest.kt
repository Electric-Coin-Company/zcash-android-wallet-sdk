package cash.z.ecc.android.sdk.internal

import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import cash.z.ecc.android.bip39.Mnemonics
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.fixture.WalletFixture
import cash.z.ecc.android.sdk.model.LightWalletEndpoint
import cash.z.ecc.android.sdk.model.ZcashNetwork
import cash.z.ecc.android.sdk.model.defaultForNetwork
import kotlinx.coroutines.test.runTest
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertFailsWith

class SdkSynchronizerTest {

    @Test
    @SmallTest
    fun cannot_instantiate_in_parallel() = runTest {
        // Random alias so that repeated invocations of this test will have a clean starting state
        val alias = UUID.randomUUID().toString()

        // In the future, inject fake networking component so that it doesn't require hitting the network
        Synchronizer.new(
            InstrumentationRegistry.getInstrumentation().context,
            ZcashNetwork.Mainnet,
            alias,
            LightWalletEndpoint.defaultForNetwork(ZcashNetwork.Mainnet),
            Mnemonics.MnemonicCode(WalletFixture.SEED_PHRASE).toEntropy(),
            birthday = null
        ).use {
            assertFailsWith<IllegalStateException> {
                Synchronizer.new(
                    InstrumentationRegistry.getInstrumentation().context,
                    ZcashNetwork.Mainnet,
                    alias,
                    LightWalletEndpoint.defaultForNetwork(ZcashNetwork.Mainnet),
                    Mnemonics.MnemonicCode(WalletFixture.SEED_PHRASE).toEntropy(),
                    birthday = null
                )
            }
        }
    }

    @Test
    @SmallTest
    fun can_instantiate_in_serial() = runTest {
        // Random alias so that repeated invocations of this test will have a clean starting state
        val alias = UUID.randomUUID().toString()

        // In the future, inject fake networking component so that it doesn't require hitting the network
        Synchronizer.new(
            InstrumentationRegistry.getInstrumentation().context,
            ZcashNetwork.Mainnet,
            alias,
            LightWalletEndpoint.defaultForNetwork(ZcashNetwork.Mainnet),
            Mnemonics.MnemonicCode(WalletFixture.SEED_PHRASE).toEntropy(),
            birthday = null
        ).use {}

        // Second instance should succeed because first one was closed
        Synchronizer.new(
            InstrumentationRegistry.getInstrumentation().context,
            ZcashNetwork.Mainnet,
            alias,
            LightWalletEndpoint.defaultForNetwork(ZcashNetwork.Mainnet),
            Mnemonics.MnemonicCode(WalletFixture.SEED_PHRASE).toEntropy(),
            birthday = null
        ).use {}
    }
}
