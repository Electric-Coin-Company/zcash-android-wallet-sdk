package cash.z.ecc.android.sdk.util

import androidx.test.platform.app.InstrumentationRegistry
import cash.z.ecc.android.sdk.CloseableSynchronizer
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.WalletInitMode
import cash.z.ecc.android.sdk.fixture.LightWalletEndpointFixture
import cash.z.ecc.android.sdk.internal.Twig
import cash.z.ecc.android.sdk.internal.ext.deleteSuspend
import cash.z.ecc.android.sdk.internal.model.Checkpoint
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.ZcashNetwork
import cash.z.ecc.android.sdk.test.readFileLinesInFlow
import cash.z.ecc.android.sdk.tool.CheckpointTool
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

/**
 * A tool for checking transactions since the given birthday and printing balances. This was useful for the Zcon1 app to
 * ensure that we loaded all the pokerchips correctly.
 */
@ExperimentalCoroutinesApi
class BalancePrinterUtil {
    private val network = ZcashNetwork.Mainnet
    private val downloadBatchSize = 9_000
    private val birthdayHeight = BlockHeight.new(network, 523240)

    private val mnemonics = SimpleMnemonics()
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val alias = "BalanceUtil"
//    private val caceDbPath = Initializer.cacheDbPath(context, alias)
//
//    private val downloader = CompactBlockDownloader(
//        LightWalletGrpcService(context, host, port),
//        CompactBlockDbStore(context, caceDbPath)
//    )

//    private val processor = CompactBlockProcessor(downloader)

//    private val rustBackend = RustBackend.init(context, cacheDbName, dataDbName)

    private lateinit var birthday: Checkpoint
    private var synchronizer: CloseableSynchronizer? = null

    @Before
    fun setup() {
        cacheBlocks()
        birthday = runBlocking { CheckpointTool.loadNearest(context, network, birthdayHeight) }
    }

    private fun cacheBlocks() =
        runBlocking {
//        twig("downloading compact blocks...")
//        val latestBlockHeight = downloader.getLatestBlockHeight()
//        val lastDownloaded = downloader.getLastDownloadedHeight()
//        val blockRange = (Math.max(birthday, lastDownloaded))..latestBlockHeight
//        downloadNewBlocks(blockRange)
        }

    private suspend fun deleteDb(dbName: String) {
        context.getDatabasePath(dbName).absoluteFile.deleteSuspend()
    }

    @Test
    @Ignore("This test is broken")
    fun printBalances() =
        runBlocking {
            readFileLinesInFlow("/utils/seeds.txt")
                .map { seedPhrase ->
                    Twig.debug { "checking balance for: $seedPhrase" }
                    mnemonics.toSeed(seedPhrase.toCharArray())
                }.collect { seed ->
                    // TODO: clear the dataDb but leave the cacheDb

                /*
            what I need to do right now
            - for each seed
            - I can reuse the cache of blocks... so just like get the cache once
            - I need to scan into a new database
                - I don't really need a new rustbackend
                - I definitely don't need a new grpc connection
            - can I just use a processor and point it to a different DB?
            + so yeah, I think I need to use the processor directly right here and just swap out its pieces
                - perhaps create a new initializer and use that to configure the processor?
                - or maybe just set the data destination for the processor
                - I might need to consider how state is impacting this design
                    - can we be more stateless and thereby improve the flexibility of this code?!!!
                 */
                    synchronizer?.close()
                    synchronizer =
                        Synchronizer.new(
                            context,
                            network,
                            lightWalletEndpoint = LightWalletEndpointFixture.newEndpointForNetwork(network),
                            seed = seed,
                            birthday = birthdayHeight,
                            // Using existing wallet init mode as simplification for the test
                            walletInitMode = WalletInitMode.ExistingWallet
                        )

//            deleteDb(dataDbPath)
//            initWallet(seed)
//            twig("scanning blocks for seed <$seed>")
// //            rustBackend.scanBlocks()
//            twig("done scanning blocks for seed $seed")
// //            val total = rustBackend.getBalance(0)
//            twig("found total: $total")
// //            val available = rustBackend.getVerifiedBalance(0)
//            twig("found available: $available")
//            twig("xrxrx2\t$seed\t$total\t$available")
//            println("xrxrx2\t$seed\t$total\t$available")
                }
        }

//    @Test
//    fun printBalances() = runBlocking {
//        readLines().collect { seed ->
//            deleteDb(dataDbName)
//            initWallet(seed)
//            twig("scanning blocks for seed <$seed>")
//            rustBackend.scanBlocks()
//            twig("done scanning blocks for seed $seed")
//            val total = rustBackend.getBalance(0)
//            twig("found total: $total")
//            val available = rustBackend.getVerifiedBalance(0)
//            twig("found available: $available")
//            twig("xrxrx2\t$seed\t$total\t$available")
//            println("xrxrx2\t$seed\t$total\t$available")
//        }

//        Thread.sleep(5000)
//        assertEquals("foo", "bar")
//    }

//    private fun initWallet(seed: String): Wallet {
//        val spendingKeyProvider = Delegates.notNull<String>()
//        return Wallet(
//            context,
//            rustBackend,
//            SampleSeedProvider(seed),
//            spendingKeyProvider,
//            Wallet.loadBirthdayFromAssets(context, birthday)
//        ).apply {
//            runCatching {
//                initialize()
//            }
//        }
//    }
}
