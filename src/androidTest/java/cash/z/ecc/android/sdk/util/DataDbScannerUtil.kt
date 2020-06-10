package cash.z.ecc.android.sdk.util

import androidx.test.platform.app.InstrumentationRegistry
import cash.z.ecc.android.sdk.Initializer
import cash.z.ecc.android.sdk.SdkSynchronizer
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.ext.TroubleshootingTwig
import cash.z.ecc.android.sdk.ext.Twig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

/**
 * A tool for validating an existing database and testing reorgs.
 */
@ExperimentalCoroutinesApi
class DataDbScannerUtil {
    private val context = InstrumentationRegistry.getInstrumentation().context

    private val host = "lightd-main.zecwallet.co"
    private val port = 443
    private val alias = "ScannerUtil"


//    private val mnemonics = SimpleMnemonics()
//    private val caceDbPath = Initializer.cacheDbPath(context, alias)

//    private val downloader = CompactBlockDownloader(
//        LightWalletGrpcService(context, host, port),
//        CompactBlockDbStore(context, caceDbPath)
//    )
    
//    private val processor = CompactBlockProcessor(downloader)
    
//    private val rustBackend = RustBackend.init(context, cacheDbName, dataDbName)

    private val initializer = Initializer(context, host, port, alias)

    private lateinit var birthday: Initializer.WalletBirthday
    private val birthdayHeight = 600_000
    private lateinit var synchronizer: Synchronizer

    @Before
    fun setup() {
        Twig.plant(TroubleshootingTwig())
//        cacheBlocks()
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
    fun scanExistingDb() {
        initializer.open(birthday)
        synchronizer = Synchronizer(initializer)

        println("sync!")
        synchronizer.start()
        val scope = (synchronizer as SdkSynchronizer).coroutineScope

        scope.launch {
            synchronizer.status.collect { status ->
                //            when (status) {
                println("received status of $status")
                //            }
            }
        }
        println("going to sleep!")
        Thread.sleep(125000)
        println("I'm back and I'm out!")
        synchronizer.stop()
    }
//
//    @Test
//    fun printBalances() = runBlocking {
//        readLines()
//            .map { seedPhrase ->
//                twig("checking balance for: $seedPhrase")
//                mnemonics.toSeed(seedPhrase.toCharArray())
//            }.collect { seed ->
//                initializer.import(seed, birthday, clearDataDb = true, clearCacheDb = false)
//                    /*
//                what I need to do right now
//                - for each seed
//                - I can reuse the cache of blocks... so just like get the cache once
//                - I need to scan into a new database
//                    - I don't really need a new rustbackend
//                    - I definitely don't need a new grpc connection
//                - can I just use a processor and point it to a different DB?
//                + so yeah, I think I need to use the processor directly right here and just swap out its pieces
//                    - perhaps create a new initializer and use that to configure the processor?
//                    - or maybe just set the data destination for the processor
//                    - I might need to consider how state is impacting this design
//                        - can we be more stateless and thereby improve the flexibility of this code?!!!
//                      */
//                synchronizer?.stop()
//                synchronizer = Synchronizer(context, initializer)
//
////            deleteDb(dataDbPath)
////            initWallet(seed)
////            twig("scanning blocks for seed <$seed>")
//////            rustBackend.scanBlocks()
////            twig("done scanning blocks for seed $seed")
//////            val total = rustBackend.getBalance(0)
////            twig("found total: $total")
//////            val available = rustBackend.getVerifiedBalance(0)
////            twig("found available: $available")
////            twig("xrxrx2\t$seed\t$total\t$available")
////            println("xrxrx2\t$seed\t$total\t$available")
//        }
//
//        Thread.sleep(5000)
//        assertEquals("foo", "bar")
//    }


}
