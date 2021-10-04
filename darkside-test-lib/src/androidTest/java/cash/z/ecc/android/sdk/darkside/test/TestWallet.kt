package cash.z.ecc.android.sdk.darkside.test

import androidx.test.platform.app.InstrumentationRegistry
import cash.z.ecc.android.bip39.Mnemonics
import cash.z.ecc.android.bip39.toSeed
import cash.z.ecc.android.sdk.Initializer
import cash.z.ecc.android.sdk.SdkSynchronizer
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.block.CompactBlockDbStore
import cash.z.ecc.android.sdk.block.CompactBlockDownloader
import cash.z.ecc.android.sdk.block.CompactBlockProcessor
import cash.z.ecc.android.sdk.db.entity.isPending
import cash.z.ecc.android.sdk.ext.Twig
import cash.z.ecc.android.sdk.ext.twig
import cash.z.ecc.android.sdk.service.LightWalletGrpcService
import cash.z.ecc.android.sdk.tool.DerivationTool
import cash.z.ecc.android.sdk.transaction.PagedTransactionRepository
import cash.z.ecc.android.sdk.transaction.PersistentTransactionManager
import cash.z.ecc.android.sdk.transaction.WalletTransactionEncoder
import cash.z.ecc.android.sdk.type.NetworkType
import cash.z.ecc.android.sdk.type.WalletBalance
import cash.z.ecc.android.sdk.type.ZcashNetwork
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import java.util.concurrent.TimeoutException

/**
 * A simple wallet that connects to testnet for integration testing. The intention is that it is
 * easy to drive and nice to use.
 */
class TestWallet(
        val seedPhrase: String,
        val alias: String = "TestWallet",
        val network: ZcashNetwork = NetworkType.Testnet,
        val host: String = network.defaultHost,
        startHeight: Int? = null,
        val port: Int = network.defaultPort,
) {
    constructor(
            backup: Backups,
            network: ZcashNetwork = NetworkType.Testnet,
            alias: String = "TestWallet"
    ) : this(
        backup.seedPhrase,
        network = network,
        startHeight = if (network == NetworkType.Mainnet) backup.mainnetBirthday else backup.testnetBirthday,
        alias = alias
    )

    val walletScope = CoroutineScope(
        SupervisorJob() + newFixedThreadPoolContext(3, this.javaClass.simpleName)
    )
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val seed: ByteArray = Mnemonics.MnemonicCode(seedPhrase).toSeed()
    private val shieldedSpendingKey = DerivationTool.deriveSpendingKeys(seed, network = network)[0]
    private val transparentSecretKey = DerivationTool.deriveTransparentSecretKey(seed, network = network)
    val initializer = Initializer(context) { config ->
        config.importWallet(seed, startHeight, network, host, alias = alias)
    }
    val synchronizer: SdkSynchronizer = TestWallet.buildbuildSynchronizer(initializer, startHeight!!)
    val service = (synchronizer.processor.downloader.lightWalletService as LightWalletGrpcService)

    val available get() = synchronizer.saplingBalances.value.availableZatoshi
    val shieldedAddress = DerivationTool.deriveShieldedAddress(seed, network = network)
    val transparentAddress = DerivationTool.deriveTransparentAddress(seed, network = network)
    val birthdayHeight get() = synchronizer.latestBirthdayHeight
    val networkName get() = synchronizer.network.networkName
    val connectionInfo get() = service.connectionInfo.toString()

    suspend fun transparentBalance(): WalletBalance {
        synchronizer.refreshUtxos(transparentAddress, synchronizer.latestBirthdayHeight)
        return synchronizer.getTransparentBalance(transparentAddress)
    }

    suspend fun sync(timeout: Long = -1): TestWallet {
        val killSwitch = walletScope.launch {
            if (timeout > 0) {
                delay(timeout)
                throw TimeoutException("Failed to sync wallet within ${timeout}ms")
            }
        }
        if (!synchronizer.isStarted) {
            twig("Starting sync")
            synchronizer.start(walletScope)
        } else {
            twig("Awaiting next SYNCED status")
        }

        // block until synced
        synchronizer.status.first { it == Synchronizer.Status.SYNCED }
        killSwitch.cancel()
        twig("Synced!")
        return this
    }

    suspend fun send(address: String = transparentAddress, memo: String = "", amount: Long = 500L, fromAccountIndex: Int = 0): TestWallet {
        Twig.sprout("$alias sending")
        synchronizer.sendToAddress(shieldedSpendingKey, amount, address, memo, fromAccountIndex)
            .takeWhile { it.isPending() }
            .collect {
                twig("Updated transaction: $it")
            }
        Twig.clip("$alias sending")
        return this
    }

    suspend fun rewindToHeight(height: Int): TestWallet {
        synchronizer.rewindToNearestHeight(height, false)
        return this
    }

    suspend fun shieldFunds(): TestWallet {
        twig("checking $transparentAddress for transactions!")
        synchronizer.refreshUtxos(transparentAddress, 935000).let { count ->
            twig("FOUND $count new UTXOs")
        }

        synchronizer.getTransparentBalance(transparentAddress).let { walletBalance ->
            twig("FOUND utxo balance of total: ${walletBalance.totalZatoshi}  available: ${walletBalance.availableZatoshi}")

            if (walletBalance.availableZatoshi > 0L) {
                synchronizer.shieldFunds(shieldedSpendingKey, transparentSecretKey)
                    .onCompletion { twig("done shielding funds") }
                    .catch { twig("Failed with $it") }
                    .collect()
            }
        }

        return this
    }

    suspend fun join(timeout: Long? = null): TestWallet {
        // block until stopped
        twig("Staying alive until synchronizer is stopped!")
        if (timeout != null) {
            twig("Scheduling a stop in ${timeout}ms")
            walletScope.launch {
                delay(timeout)
                synchronizer.stop()
            }
        }
        synchronizer.status.first { it == Synchronizer.Status.STOPPED }
        twig("Stopped!")
        return this
    }

    companion object {
        init {
            Twig.enabled(true)
        }
    }

    enum class Backups(val seedPhrase: String, val testnetBirthday: Int, val mainnetBirthday: Int) {
        // TODO: get the proper birthday values for these wallets
        DEFAULT("column rhythm acoustic gym cost fit keen maze fence seed mail medal shrimp tell relief clip cannon foster soldier shallow refuse lunar parrot banana", 1_355_928, 1_000_000),
        SAMPLE_WALLET("input frown warm senior anxiety abuse yard prefer churn reject people glimpse govern glory crumble swallow verb laptop switch trophy inform friend permit purpose", 1_330_190, 1_000_000),
        DEV_WALLET("still champion voice habit trend flight survey between bitter process artefact blind carbon truly provide dizzy crush flush breeze blouse charge solid fish spread", 1_000_000, 991645),
        ALICE("quantum whisper lion route fury lunar pelican image job client hundred sauce chimney barely life cliff spirit admit weekend message recipe trumpet impact kitten", 1_330_190, 1_000_000),
        BOB("canvas wine sugar acquire garment spy tongue odor hole cage year habit bullet make label human unit option top calm neutral try vocal arena", 1_330_190, 1_000_000),
        ;
    }
}

private fun TestWallet.Companion.buildbuildSynchronizer(initializer: Initializer, saplingActivationHeight: Int): SdkSynchronizer {
    val repository = PagedTransactionRepository(initializer.context, 1000, initializer.rustBackend, initializer.birthday, initializer.viewingKeys, initializer.overwriteVks)
    val blockStore = CompactBlockDbStore(initializer.context, initializer.rustBackend.pathCacheDb)
    val service = LightWalletGrpcService(initializer.context, initializer.host, initializer.port)
    val downloader = CompactBlockDownloader(service, blockStore)
    val encoder = WalletTransactionEncoder(initializer.rustBackend, repository)
    val txManager = PersistentTransactionManager(initializer.context, encoder, service)

    val processor = CompactBlockProcessor(downloader, repository, initializer.rustBackend, saplingActivationHeight)
    return Synchronizer(initializer,
            repository,
            blockStore,
            service,
            encoder,
            downloader,
            txManager,
            processor) as SdkSynchronizer
}
