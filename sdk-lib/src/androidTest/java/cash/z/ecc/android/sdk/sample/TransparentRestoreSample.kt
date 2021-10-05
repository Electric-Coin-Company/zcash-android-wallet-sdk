package cash.z.ecc.android.sdk.sample

import androidx.test.filters.LargeTest
import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.ext.twig
import cash.z.ecc.android.sdk.type.NetworkType.Testnet
import cash.z.ecc.android.sdk.util.TestWallet
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test

/**
 * Sample tests are used to demonstrate functionality. This one attempts to setup a scenario where
 * one wallet shields funds and the other restores from the blockchain. Ultimately, they should have
 * the same data.
 */
class TransparentRestoreSample {

    val TX_VALUE = ZcashSdk.MINERS_FEE_ZATOSHI / 2

//    val walletA = SimpleWallet(SEED_PHRASE, "WalletA")

    // the wallet that only restores what everyone else did
//    val walletB = SimpleWallet(SEED_PHRASE, "WalletB")
//    // the wallet that sends Z2T transactions
//
//    // sandbox wallet
//    val walletSandbox = SimpleWallet(SEED_PHRASE, "WalletC")
//    val walletZ2T = SimpleWallet(SEED_PHRASE, "WalletZ2T")
//    val externalTransparentAddress =
//        DerivationTool.deriveTransparentAddress(Mnemonics.MnemonicCode(RANDOM_PHRASE).toSeed(), Testnet)

    //    @Test
    fun sendZ2Texternal() = runBlocking {
        twig("Syncing WalletExt")
        val extWallet = TestWallet(TestWallet.Backups.ALICE, alias = "WalletE")
        extWallet.sync()
//        extWallet.send(542, walletSandbox.transparentAddress, "External funds memo is lost, though")
        delay(1000)
        twig("Done sending funds to external address (Z->T COMPLETE!)")
    }

    //    @Test
    fun sendZ2T() = runBlocking {
//        walletSandbox.sync()
//        walletZ2T.send(543, externalTransparentAddress, "External funds memo is lost, though")
        delay(1000)
        twig("Done sending funds to external address (Z->T COMPLETE!)")
    }

//    @Test
    fun autoShield() = runBlocking<Unit> {
        val wallet = TestWallet(TestWallet.Backups.SAMPLE_WALLET, alias = "WalletC")
        wallet.sync()
        twig("Done syncing wallet!")
        val tbalance = wallet.transparentBalance()
        val address = wallet.transparentAddress

        twig("t-avail: ${tbalance.availableZatoshi}  t-total: ${tbalance.totalZatoshi}")
        Assert.assertTrue("Not enough funds to run sample. Expected some Zatoshi but found ${tbalance.availableZatoshi}. Try adding funds to $address", tbalance.availableZatoshi > 0)

        twig("Shielding available transparent funds!")
//        wallet.shieldFunds()
    }

//    @Test
    fun cli() = runBlocking<Unit> {
//        val wallet = SimpleWallet(SEED_PHRASE, "WalletCli")
//        wallet.sync()
//        wallet.rewindToHeight(1343500).join(45_000)
        val wallet = TestWallet(TestWallet.Backups.SAMPLE_WALLET, alias = "WalletC")
//        wallet.sync().rewindToHeight(1339178).join(10000)
        wallet.sync().rewindToHeight(1339178).send(
            "ztestsapling17zazsl8rryl8kjaqxnr2r29rw9d9a2mud37ugapm0s8gmyv0ue43h9lqwmhdsp3nu9dazeqfs6l",
            "is send broken?"
        ).join(5)
    }

    // This test is extremely slow and doesn't assert anything, so the benefit of this test is unclear
    // It is disabled to allow moving forward with configuring CI.
    @Test
    @LargeTest
    @Ignore("This test is extremely slow")
    fun kris() = runBlocking<Unit> {
        val wallet0 = TestWallet(TestWallet.Backups.SAMPLE_WALLET.seedPhrase, "tmpabc", Testnet, startHeight = 1330190)
//        val wallet1 = SimpleWallet(WALLET0_PHRASE, "Wallet1")

        wallet0.sync() // .shieldFunds()
//            .send(amount = 1543L, memo = "")
            .join()
//        wallet1.sync().join(5_000L)
    }

    /*





     */

    /**
     * Sanity check that the wallet has enough funds for the test
     */
//    @Test
    fun hasFunds() = runBlocking<Unit> {
        val walletSandbox = TestWallet(TestWallet.Backups.SAMPLE_WALLET.seedPhrase, "WalletC", Testnet, startHeight = 1330190)
        //        val job = walletA.walletScope.launch {
        //            twig("Syncing WalletA")
        //            walletA.sync()
        //        }
        twig("Syncing WalletSandbox")
        walletSandbox.sync()
        //        job.join()
        delay(500)

        twig("Done syncing both wallets!")
        //        val value = walletA.available
        //        val address = walletA.shieldedAddress
        //        Assert.assertTrue("Not enough funds to run sample. Expected at least $TX_VALUE Zatoshi but found $value. Try adding funds to $address", value >= TX_VALUE)

        // send z->t
        //        walletA.send(TX_VALUE, walletA.transparentAddress, "${TransparentRestoreSample::class.java.simpleName} z->t")

        walletSandbox.rewindToHeight(1339178)
        twig("Done REWINDING!")
        twig("T-ADDR (for the win!): ${walletSandbox.transparentAddress}")
        delay(500)
        //        walletB.sync()
        // rewind database B to height then rescan
    }

//    // when startHeight is null, it will use the latest checkpoint
//    class SimpleWallet(
//        seedPhrase: String,
//        alias: String = ZcashSdk.DEFAULT_ALIAS,
//        startHeight: Int? = null
//    ) {
//        val walletScope = CoroutineScope(
//            SupervisorJob() + newFixedThreadPoolContext(3, this.javaClass.simpleName)
//        )
//        private val context = InstrumentationRegistry.getInstrumentation().context
//        private val seed: ByteArray = Mnemonics.MnemonicCode(seedPhrase).toSeed()
//        private val shieldedSpendingKey = DerivationTool.deriveSpendingKeys(seed, Testnet)[0]
//        private val transparentSecretKey = DerivationTool.deriveTransparentSecretKey(seed, Testnet)
//        private val host = "lightwalletd.testnet.electriccoin.co"
//        private val initializer = Initializer(context) { config ->
//            config.importWallet(seed, startHeight)
//            config.setNetwork(Testnet, host)
//            config.alias = alias
//        }
//
//        val synchronizer = Synchronizer(initializer)
//        val available get() = synchronizer.latestBalance.availableZatoshi
//        val shieldedAddress = DerivationTool.deriveShieldedAddress(seed, Testnet)
//        val transparentAddress = DerivationTool.deriveTransparentAddress(seed, Testnet)
//        val birthdayHeight get() = synchronizer.latestBirthdayHeight
//
//        suspend fun transparentBalance(): WalletBalance {
//            synchronizer.refreshUtxos(transparentAddress, synchronizer.latestBirthdayHeight)
//            return synchronizer.getTransparentBalance(transparentAddress)
//        }
//
//        suspend fun sync(): SimpleWallet {
//            if (!synchronizer.isStarted) {
//                twig("Starting sync")
//                synchronizer.start(walletScope)
//            } else {
//                twig("Awaiting next SYNCED status")
//            }
//
//            // block until synced
//            synchronizer.status.first { it == SYNCED }
//            twig("Synced!")
//            return this
//        }
//
//        suspend fun send(address: String = transparentAddress, memo: String = "", amount: Long = 500L): SimpleWallet {
//            synchronizer.sendToAddress(shieldedSpendingKey, amount, address, memo)
//                .takeWhile { it.isPending() }
//                .collect {
//                    twig("Updated transaction: $it")
//                }
//            return this
//        }
//
//        suspend fun rewindToHeight(height: Int): SimpleWallet {
//            synchronizer.rewindToHeight(height, false)
//            return this
//        }
//
//        suspend fun shieldFunds(): SimpleWallet {
//            twig("checking $transparentAddress for transactions!")
//            synchronizer.refreshUtxos(transparentAddress, 935000).let { count ->
//                twig("FOUND $count new UTXOs")
//            }
//
//            synchronizer.getTransparentBalance(transparentAddress).let { walletBalance ->
//                twig("FOUND utxo balance of total: ${walletBalance.totalZatoshi}  available: ${walletBalance.availableZatoshi}")
//
//                if (walletBalance.availableZatoshi > 0L) {
//                    synchronizer.shieldFunds(shieldedSpendingKey, transparentSecretKey)
//                        .onCompletion { twig("done shielding funds") }
//                        .catch { twig("Failed with $it") }
//                        .collect()
//                }
//            }
//
//            return this
//        }
//
//        suspend fun join(timeout: Long? = null): SimpleWallet {
//            // block until stopped
//            twig("Staying alive until synchronizer is stopped!")
//            if (timeout != null) {
//                twig("Scheduling a stop in ${timeout}ms")
//                walletScope.launch {
//                    delay(timeout)
//                    synchronizer.stop()
//                }
//            }
//            synchronizer.status.first { it == Synchronizer.Status.STOPPED }
//            twig("Stopped!")
//            return this
//        }
//
//        companion object {
//            init {
//                Twig.plant(TroubleshootingTwig())
//            }
//        }
//    }
}
