package cash.z.ecc.android.sdk.internal

import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import cash.z.ecc.android.bip39.Mnemonics
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.WalletInitMode
import cash.z.ecc.android.sdk.exception.InitializeException
import cash.z.ecc.android.sdk.fixture.AccountCreateSetupFixture
import cash.z.ecc.android.sdk.fixture.LightWalletEndpointFixture
import cash.z.ecc.android.sdk.fixture.WalletFixture
import cash.z.ecc.android.sdk.model.ZcashNetwork
import kotlinx.coroutines.test.runTest
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SdkSynchronizerTest {
    @Test
    @SmallTest
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun cannot_instantiate_in_parallel() =
        runTest {
            // Random alias so that repeated invocations of this test will have a clean starting state
            val alias = UUID.randomUUID().toString()

            // In the future, inject fake networking component so that it doesn't require hitting the network
            Synchronizer
                .new(
                    alias = alias,
                    birthday = null,
                    context = InstrumentationRegistry.getInstrumentation().context,
                    lightWalletEndpoint = LightWalletEndpointFixture.newEndpointForNetwork(ZcashNetwork.Mainnet),
                    setup =
                        AccountCreateSetupFixture.new(
                            seed = Mnemonics.MnemonicCode(WalletFixture.BEN_SEED_PHRASE).toEntropy()
                        ),
                    // Using existing wallet init mode as simplification for the test
                    walletInitMode = WalletInitMode.ExistingWallet,
                    zcashNetwork = ZcashNetwork.Mainnet,
                    isTorEnabled = false,
                ).use {
                    assertFailsWith<IllegalStateException> {
                        Synchronizer.new(
                            alias = alias,
                            birthday = null,
                            context = InstrumentationRegistry.getInstrumentation().context,
                            lightWalletEndpoint =
                                LightWalletEndpointFixture.newEndpointForNetwork(ZcashNetwork.Mainnet),
                            setup =
                                AccountCreateSetupFixture.new(
                                    seed = Mnemonics.MnemonicCode(WalletFixture.BEN_SEED_PHRASE).toEntropy()
                                ),
                            // Using existing wallet init mode as simplification for the test
                            walletInitMode = WalletInitMode.ExistingWallet,
                            zcashNetwork = ZcashNetwork.Mainnet,
                            isTorEnabled = false,
                        )
                    }
                }
        }

    @Test
    @SmallTest
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun can_instantiate_in_serial() =
        runTest {
            // Random alias so that repeated invocations of this test will have a clean starting state
            val alias = UUID.randomUUID().toString()

            // TODO [#1094]: Consider fake SDK sync related components
            // TODO [#1094]: https://github.com/zcash/zcash-android-wallet-sdk/issues/1094
            // In the future, inject fake networking component so that it doesn't require hitting the network
            Synchronizer
                .new(
                    alias = alias,
                    birthday = null,
                    context = InstrumentationRegistry.getInstrumentation().context,
                    lightWalletEndpoint = LightWalletEndpointFixture.newEndpointForNetwork(ZcashNetwork.Mainnet),
                    setup =
                        AccountCreateSetupFixture.new(
                            seed = Mnemonics.MnemonicCode(WalletFixture.BEN_SEED_PHRASE).toEntropy()
                        ),
                    // Using existing wallet init mode as simplification for the test
                    walletInitMode = WalletInitMode.ExistingWallet,
                    zcashNetwork = ZcashNetwork.Mainnet,
                    isTorEnabled = false,
                ).use {}

            // Second instance should succeed because first one was closed
            Synchronizer
                .new(
                    alias = alias,
                    birthday = null,
                    context = InstrumentationRegistry.getInstrumentation().context,
                    lightWalletEndpoint = LightWalletEndpointFixture.newEndpointForNetwork(ZcashNetwork.Mainnet),
                    setup =
                        AccountCreateSetupFixture.new(
                            seed = Mnemonics.MnemonicCode(WalletFixture.BEN_SEED_PHRASE).toEntropy()
                        ),
                    // Using existing wallet init mode as simplification for the test
                    walletInitMode = WalletInitMode.ExistingWallet,
                    zcashNetwork = ZcashNetwork.Mainnet,
                    isTorEnabled = false,
                ).use {}
        }

    @Test
    @SmallTest
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun detects_irrelevant_seeds() =
        runTest {
            // Random alias so that repeated invocations of this test will have a clean starting state
            val alias = UUID.randomUUID().toString()

            // TODO [#1094]: Consider fake SDK sync related components
            // TODO [#1094]: https://github.com/zcash/zcash-android-wallet-sdk/issues/1094
            // In the future, inject fake networking component so that it doesn't require hitting the network
            Synchronizer
                .new(
                    alias = alias,
                    birthday = null,
                    context = InstrumentationRegistry.getInstrumentation().context,
                    lightWalletEndpoint = LightWalletEndpointFixture.newEndpointForNetwork(ZcashNetwork.Mainnet),
                    setup =
                        AccountCreateSetupFixture.new(
                            seed = Mnemonics.MnemonicCode(WalletFixture.ALICE_SEED_PHRASE).toEntropy()
                        ),
                    // Using existing wallet init mode as simplification for the test
                    walletInitMode = WalletInitMode.ExistingWallet,
                    zcashNetwork = ZcashNetwork.Mainnet,
                    isTorEnabled = false,
                ).use {
                    it.getSaplingAddress(it.getAccounts()[0])
                }

            // Second instance should fail because the seed is not relevant to the wallet.
            val error =
                assertFailsWith<InitializeException> {
                    Synchronizer
                        .new(
                            alias = alias,
                            birthday = null,
                            context = InstrumentationRegistry.getInstrumentation().context,
                            lightWalletEndpoint =
                                LightWalletEndpointFixture.newEndpointForNetwork(ZcashNetwork.Mainnet),
                            setup =
                                AccountCreateSetupFixture.new(
                                    seed = Mnemonics.MnemonicCode(WalletFixture.BEN_SEED_PHRASE).toEntropy()
                                ),
                            // Using existing wallet init mode as simplification for the test
                            walletInitMode = WalletInitMode.ExistingWallet,
                            zcashNetwork = ZcashNetwork.Mainnet,
                            isTorEnabled = false,
                        ).use {}
                }
            assertEquals(InitializeException.SeedNotRelevant, error)
        }
}
