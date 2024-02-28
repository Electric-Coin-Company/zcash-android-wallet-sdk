package cash.z.ecc.android.sdk.darkside.test

import androidx.test.platform.app.InstrumentationRegistry
import cash.z.ecc.android.bip39.Mnemonics
import cash.z.ecc.android.bip39.toSeed
import cash.z.ecc.android.sdk.SdkSynchronizer
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.WalletInitMode
import cash.z.ecc.android.sdk.ext.Darkside
import cash.z.ecc.android.sdk.internal.Twig
import cash.z.ecc.android.sdk.model.Account
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.Zatoshi
import cash.z.ecc.android.sdk.model.ZcashNetwork
import cash.z.ecc.android.sdk.tool.DerivationTool
import co.electriccoin.lightwallet.client.model.LightWalletEndpoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeoutException

/**
 * A simple wallet that connects to testnet for integration testing. The intention is that it is
 * easy to drive and nice to use.
 */
@OptIn(DelicateCoroutinesApi::class)
class TestWallet(
    val seedPhrase: String,
    val alias: String = "TestWallet",
    val network: ZcashNetwork = ZcashNetwork.Testnet,
    val endpoint: LightWalletEndpoint = LightWalletEndpoint.Darkside,
    startHeight: BlockHeight? = null
) {
    constructor(
        backup: Backups,
        network: ZcashNetwork = ZcashNetwork.Testnet,
        alias: String = "TestWallet"
    ) : this(
        backup.seedPhrase,
        network = network,
        startHeight = if (network == ZcashNetwork.Mainnet) backup.mainnetBirthday else backup.testnetBirthday,
        alias = alias
    )

    val walletScope =
        CoroutineScope(
            SupervisorJob() + newFixedThreadPoolContext(3, this.javaClass.simpleName)
        )

    // Although runBlocking isn't great, this usage is OK because this is only used within the
    // automated tests

    private val account = Account.DEFAULT
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val seed: ByteArray = Mnemonics.MnemonicCode(seedPhrase).toSeed()
    private val shieldedSpendingKey =
        runBlocking { DerivationTool.getInstance().deriveUnifiedSpendingKey(seed, network = network, account) }
    val synchronizer: SdkSynchronizer =
        Synchronizer.newBlocking(
            context,
            network,
            alias,
            endpoint,
            seed,
            startHeight,
            // Using existing wallet init mode as simplification for the test
            walletInitMode = WalletInitMode.ExistingWallet
        ) as SdkSynchronizer

    val available get() = synchronizer.saplingBalances.value?.available
    val unifiedAddress =
        runBlocking { synchronizer.getUnifiedAddress(account) }
    val transparentAddress =
        runBlocking { synchronizer.getTransparentAddress(account) }
    val birthdayHeight get() = synchronizer.latestBirthdayHeight
    val networkName get() = synchronizer.network.networkName

    suspend fun transparentBalance(): Zatoshi {
        synchronizer.refreshUtxos(account, synchronizer.latestBirthdayHeight)
        return synchronizer.getTransparentBalance(transparentAddress)
    }

    suspend fun sync(timeout: Long = -1): TestWallet {
        val killSwitch =
            walletScope.launch {
                if (timeout > 0) {
                    delay(timeout)
                    throw TimeoutException("Failed to sync wallet within ${timeout}ms")
                }
            }

        // block until synced
        synchronizer.status.first { it == Synchronizer.Status.SYNCED }
        killSwitch.cancel()
        return this
    }

    suspend fun send(
        address: String = transparentAddress,
        memo: String = "",
        amount: Zatoshi = Zatoshi(500L)
    ): TestWallet {
        synchronizer.createProposedTransactions(
            synchronizer.proposeTransfer(
                shieldedSpendingKey.account,
                address,
                amount,
                memo
            ),
            shieldedSpendingKey
        )
        return this
    }

    suspend fun rewindToHeight(height: BlockHeight): TestWallet {
        synchronizer.rewindToNearestHeight(height)
        return this
    }

    suspend fun shieldFunds(): TestWallet {
        synchronizer.refreshUtxos(Account.DEFAULT, BlockHeight.new(ZcashNetwork.Mainnet, 935000)).let { count ->
            Twig.debug { "FOUND $count new UTXOs" }
        }

        synchronizer.getTransparentBalance(transparentAddress).let { walletBalance ->
            if (walletBalance.value > 0L) {
                synchronizer.proposeShielding(shieldedSpendingKey.account, Zatoshi(100000))?.let {
                    synchronizer.createProposedTransactions(
                        it,
                        shieldedSpendingKey
                    )
                }
            }
        }

        return this
    }

    suspend fun join(timeout: Long? = null): TestWallet {
        // block until stopped
        if (timeout != null) {
            walletScope.launch {
                delay(timeout)
                synchronizer.close()
            }
        }
        synchronizer.status.first { it == Synchronizer.Status.STOPPED }
        return this
    }

    companion object {
    }

    enum class Backups(val seedPhrase: String, val testnetBirthday: BlockHeight, val mainnetBirthday: BlockHeight) {
        // TODO [#902]: Get the proper birthday values for test wallets
        // TODO [#902]: https://github.com/zcash/zcash-android-wallet-sdk/issues/902
        DEFAULT(
            "column rhythm acoustic gym cost fit keen maze fence seed mail medal shrimp tell relief clip" +
                " cannon foster soldier shallow refuse lunar parrot banana",
            BlockHeight.new(
                ZcashNetwork.Testnet,
                1_355_928
            ),
            BlockHeight.new(ZcashNetwork.Mainnet, 1_000_000)
        ),
        SAMPLE_WALLET(
            "input frown warm senior anxiety abuse yard prefer churn reject people glimpse govern glory" +
                " crumble swallow verb laptop switch trophy inform friend permit purpose",
            BlockHeight.new(
                ZcashNetwork.Testnet,
                1_330_190
            ),
            BlockHeight.new(ZcashNetwork.Mainnet, 1_000_000)
        ),
        DEV_WALLET(
            "still champion voice habit trend flight survey between bitter process artefact blind carbon" +
                " truly provide dizzy crush flush breeze blouse charge solid fish spread",
            BlockHeight.new(
                ZcashNetwork.Testnet,
                1_000_000
            ),
            BlockHeight.new(ZcashNetwork.Mainnet, 991645)
        ),
        ALICE(
            "quantum whisper lion route fury lunar pelican image job client hundred sauce chimney barely" +
                " life cliff spirit admit weekend message recipe trumpet impact kitten",
            BlockHeight.new(
                ZcashNetwork.Testnet,
                1_330_190
            ),
            BlockHeight.new(ZcashNetwork.Mainnet, 1_000_000)
        ),
        BOB(
            "canvas wine sugar acquire garment spy tongue odor hole cage year habit bullet make label" +
                " human unit option top calm neutral try vocal arena",
            BlockHeight.new(
                ZcashNetwork.Testnet,
                1_330_190
            ),
            BlockHeight.new(ZcashNetwork.Mainnet, 1_000_000)
        )
    }
}
