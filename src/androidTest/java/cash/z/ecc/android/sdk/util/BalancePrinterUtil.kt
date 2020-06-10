package cash.z.ecc.android.sdk.util

//import cash.z.ecc.android.sdk.secure.Wallet
import androidx.test.platform.app.InstrumentationRegistry
import cash.z.ecc.android.sdk.Initializer
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.block.CompactBlockDbStore
import cash.z.ecc.android.sdk.block.CompactBlockDownloader
import cash.z.ecc.android.sdk.ext.TroubleshootingTwig
import cash.z.ecc.android.sdk.ext.Twig
import cash.z.ecc.android.sdk.ext.twig
import cash.z.ecc.android.sdk.service.LightWalletGrpcService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import okio.Okio
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * A tool for checking transactions since the given birthday and printing balances. This was useful for the Zcon1 app to
 * ensure that we loaded all the pokerchips correctly.
 */
@ExperimentalCoroutinesApi
class BalancePrinterUtil {

    private val host = "lightd-main.zecwallet.co"
    private val port = 443
    private val downloadBatchSize = 9_000
    private val birthdayHeight = 523240


    private val mnemonics = SimpleMnemonics()
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val alias = "BalanceUtil"
    private val caceDbPath = Initializer.cacheDbPath(context, alias)

    private val downloader = CompactBlockDownloader(
        LightWalletGrpcService(context, host, port),
        CompactBlockDbStore(context, caceDbPath)
    )
    
//    private val processor = CompactBlockProcessor(downloader)
    
//    private val rustBackend = RustBackend.init(context, cacheDbName, dataDbName)

    private val initializer = Initializer(context, host, port, alias)

    private lateinit var birthday: Initializer.WalletBirthday
    private var synchronizer: Synchronizer? = null

    @Before
    fun setup() {
        Twig.plant(TroubleshootingTwig())
        cacheBlocks()
        birthday = Initializer.DefaultBirthdayStore(context, birthdayHeight, alias).getBirthday()
    }

    private fun cacheBlocks() = runBlocking {
//        twig("downloading compact blocks...")
//        val latestBlockHeight = downloader.getLatestBlockHeight()
//        val lastDownloaded = downloader.getLastDownloadedHeight()
//        val blockRange = (Math.max(birthday, lastDownloaded))..latestBlockHeight
//        downloadNewBlocks(blockRange)
//        val error = validateNewBlocks(blockRange)
//        twig("validation completed with result $error")
//        assertEquals(-1, error)
    }

    private fun deleteDb(dbName: String) {
        context.getDatabasePath(dbName).absoluteFile.delete()
    }

    @Test
    fun printBalances() = runBlocking {
        readLines()
            .map { seedPhrase ->
                twig("checking balance for: $seedPhrase")
                mnemonics.toSeed(seedPhrase.toCharArray())
            }.collect { seed ->
                initializer.import(seed, birthday, clearDataDb = true, clearCacheDb = false)
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
                synchronizer?.stop()
                synchronizer = Synchronizer(initializer)
            
//            deleteDb(dataDbPath)
//            initWallet(seed)
//            twig("scanning blocks for seed <$seed>")
////            rustBackend.scanBlocks()
//            twig("done scanning blocks for seed $seed")
////            val total = rustBackend.getBalance(0)
//            twig("found total: $total")
////            val available = rustBackend.getVerifiedBalance(0)
//            twig("found available: $available")
//            twig("xrxrx2\t$seed\t$total\t$available")
//            println("xrxrx2\t$seed\t$total\t$available")
        }

        Thread.sleep(3000)
        assertEquals("foo", "bar")
    }
    
//    @Test
//    fun printBalances() = runBlocking {
//        readLines().collect { seed ->
//            deleteDb(dataDbName)
//            initWallet(seed)
//            twig("scanning blocks for seed <$seed>")
////            rustBackend.scanBlocks()
//            twig("done scanning blocks for seed $seed")
////            val total = rustBackend.getBalance(0)
//            twig("found total: $total")
////            val available = rustBackend.getVerifiedBalance(0)
//            twig("found available: $available")
//            twig("xrxrx2\t$seed\t$total\t$available")
//            println("xrxrx2\t$seed\t$total\t$available")
//        }

//        Thread.sleep(5000)
//        assertEquals("foo", "bar")
//    }

    @Throws(IOException::class)
    fun readLines() = flow<String> {
        val seedFile = javaClass.getResourceAsStream("/utils/seeds.txt")
        Okio.buffer(Okio.source(seedFile)).use { source ->
            var line: String? = source.readUtf8Line()
            while (line != null) {
                emit(line)
                line = source.readUtf8Line()
            }
        }
    }

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

    private fun downloadNewBlocks(range: IntRange) = runBlocking {
        Twig.sprout("downloading")
        twig("downloading blocks in range $range")

        var downloadedBlockHeight = range.start
        val count = range.last - range.first + 1
        val batches = (count / downloadBatchSize + (if (count.rem(downloadBatchSize) == 0) 0 else 1))
        twig("found $count missing blocks, downloading in $batches batches of $downloadBatchSize...")
        for (i in 1..batches) {
            val end = Math.min(range.first + (i * downloadBatchSize), range.last + 1)
            val batchRange = downloadedBlockHeight until end
            twig("downloaded $batchRange (batch $i of $batches)") {
//                downloader.downloadBlockRange(batchRange)
            }
            downloadedBlockHeight = end

        }
        Twig.clip("downloading")
    }

//    private fun validateNewBlocks(range: IntRange?): Int {
////        val dummyWallet = initWallet("dummySeed")
//        Twig.sprout("validating")
//        twig("validating blocks in range $range")
////        val result = rustBackend.validateCombinedChain()
//        Twig.clip("validating")
//        return result
//    }

}
