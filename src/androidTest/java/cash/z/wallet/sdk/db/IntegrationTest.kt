package cash.z.wallet.sdk.db

import androidx.test.platform.app.InstrumentationRegistry
import cash.z.wallet.sdk.data.*
import cash.z.wallet.sdk.jni.JniConverter
import cash.z.wallet.sdk.secure.Wallet
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.AfterEach

/*
TODO:
setup a test that we can run and just watch things happen, to give confidence that logging is expressive enough to
verify that the SDK is behaving as expected.
 */
class IntegrationTest {

    private val dataDbName = "IntegrationData.db"
    private val cacheDdName = "IntegrationCache.db"
    private val context = InstrumentationRegistry.getInstrumentation().context

    private lateinit var downloader: CompactBlockStream
    private lateinit var processor: CompactBlockProcessor
    private lateinit var synchronizer: Synchronizer
    private lateinit var repository: TransactionRepository
    private lateinit var wallet: Wallet

    @Before
    fun setup() {
        deleteDbs()
    }

    private fun deleteDbs() {
        // prior to each run, delete the DBs for sanity
        listOf(cacheDdName, dataDbName).map { context.getDatabasePath(it).absoluteFile }.forEach {
            println("Deleting ${it.name}")
            it.delete()
        }
    }

    @Test
    fun testSync() = runBlocking<Unit> {
        val converter = JniConverter()
        converter.initLogs()
        val logger = TroubleshootingTwig()

        downloader = CompactBlockStream("10.0.2.2", 9067, logger)
        processor = CompactBlockProcessor(context, converter, cacheDdName, dataDbName, logger = logger)
        repository = PollingTransactionRepository(context, dataDbName, 10_000L, converter, logger)
        wallet = Wallet(converter, context.getDatabasePath(dataDbName).absolutePath, context.cacheDir.absolutePath, arrayOf(0), SampleSeedProvider("dummyseed"))

//        repository.start(this)
        synchronizer = Synchronizer(
            downloader,
            processor,
            repository,
            wallet,
            logger
        ).start(this)

        for(i in synchronizer.downloader.progress()) {
            logger.twig("made progress: $i")
        }
    }

    @AfterEach
    fun tearDown() {
        repository.stop()
        synchronizer.stop()
    }
}