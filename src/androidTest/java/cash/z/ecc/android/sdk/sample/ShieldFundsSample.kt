package cash.z.ecc.android.sdk.sample

import androidx.test.platform.app.InstrumentationRegistry
import cash.z.ecc.android.bip39.Mnemonics
import cash.z.ecc.android.bip39.toSeed
import cash.z.ecc.android.sdk.Initializer
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.Synchronizer.Status.SYNCED
import cash.z.ecc.android.sdk.ext.TroubleshootingTwig
import cash.z.ecc.android.sdk.ext.Twig
import cash.z.ecc.android.sdk.ext.twig
import cash.z.ecc.android.sdk.tool.DerivationTool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test

/**
 * Samples related to shielding funds.
 */
class ShieldFundsSample {

    val SEED_PHRASE = "wish puppy smile loan doll curve hole maze file ginger hair nose key relax knife witness cannon grab despair throw review deal slush frame"//\"still champion voice habit trend flight survey between bitter process artefact blind carbon truly provide dizzy crush flush breeze blouse charge solid fish spread\"//\"deputy visa gentle among clean scout farm drive comfort patch skin salt ranch cool ramp warrior drink narrow normal lunch behind salt deal person"//"deputy visa gentle among clean scout farm drive comfort patch skin salt ranch cool ramp warrior drink narrow normal lunch behind salt deal person"

    // simple flag to turn off actually spending funds
    val IS_DRY_RUN = true

    /**
     * This test will construct a t2z transaction. It is safe to run this repeatedly, because
     * nothing is submitted to the network (because the keys don't match the address so the encoding
     * fails). Originally, it's intent is just to exercise the code and troubleshoot any issues but
     * then it became clear that this would be a cool Sample Test and PoC for writing a SimpleWallet
     * class.
     */
    @Test
    fun constructT2Z() = runBlocking {
        Twig.sprout("ShieldFundsSample")

        val wallet = SimpleWallet(SEED_PHRASE).sync()
        wallet.shieldFunds()

        Twig.clip("ShieldFundsSample")
        Assert.assertEquals(5, wallet.synchronizer.latestBalance.availableZatoshi)
    }


    // when startHeight is null, it will use the latest checkpoint
    class SimpleWallet(seedPhrase: String, startHeight: Int? = null) {
        val walletScope = CoroutineScope(
            SupervisorJob() + newFixedThreadPoolContext(3, this.javaClass.simpleName)
        )
        private val context = InstrumentationRegistry.getInstrumentation().context
        private val seed: ByteArray = Mnemonics.MnemonicCode(seedPhrase).toSeed()
        private val shieldedSpendingKey = DerivationTool.deriveSpendingKeys(seed)[0]
        private val transparentSecretKey = DerivationTool.deriveTransparentSecretKey(seed)
        private val shieldedAddress = DerivationTool.deriveShieldedAddress(seed)

        // t1b9Y6PESSGavavgge3ruTtX9X83817V29s
        private val transparentAddress = DerivationTool.deriveTransparentAddress(seed)

        private val config = Initializer.Config {
            it.setSeed(seed)
            it.setBirthdayHeight(startHeight, false)
            it.server("lightwalletd.electriccoin.co", 9067)
        }

        val synchronizer = Synchronizer(Initializer(context, config))

        suspend fun sync(): SimpleWallet {
            twig("Starting sync")
            synchronizer.start(walletScope)
            // block until synced
            synchronizer.status.first { it == SYNCED }
            twig("Synced!")
            return this
        }

        suspend fun shieldFunds(): SimpleWallet {
            twig("checking $transparentAddress for transactions!")
            synchronizer.refreshUtxos(transparentAddress, 935000).let { count ->
                twig("FOUND $count new UTXOs")
            }

            synchronizer.getTransparentBalance(transparentAddress).let { walletBalance ->
                twig("FOUND utxo balance of total: ${walletBalance.totalZatoshi}  available: ${walletBalance.availableZatoshi}")

                if (walletBalance.availableZatoshi > 0L && !IS_DRY_RUN) {
                    synchronizer.shieldFunds(shieldedSpendingKey, transparentSecretKey)
                        .onCompletion { twig("done shielding funds") }
                        .catch { twig("Failed with $it") }
                        .collect()
                }
            }

            return this
        }

        companion object {
            init {
                Twig.plant(TroubleshootingTwig())
            }
        }
    }
}
