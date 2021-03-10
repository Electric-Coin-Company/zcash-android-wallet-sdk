package cash.z.ecc.android.sdk.util

import androidx.test.platform.app.InstrumentationRegistry
import cash.z.ecc.android.sdk.Initializer
import cash.z.ecc.android.sdk.SdkSynchronizer
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.ext.Twig
import cash.z.ecc.android.sdk.ext.seedPhrase
import cash.z.ecc.android.sdk.ext.twig
import cash.z.ecc.android.sdk.tool.DerivationTool
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class DarksideTestCoordinator(val host: String = "127.0.0.1", val testName: String = "DarksideTestCoordinator") {
    private val port = 9067
    private val birthdayHeight = 663150
    private val targetHeight = 663250
    private val seedPhrase =
        "still champion voice habit trend flight survey between bitter process artefact blind carbon truly provide dizzy crush flush breeze blouse charge solid fish spread"
    private val context = InstrumentationRegistry.getInstrumentation().context

    // dependencies: private
    private lateinit var darkside: DarksideApi

    // dependencies: public
    val validator = DarksideTestValidator()
    val chainMaker = DarksideChainMaker()
//    var initializer = Initializer(context, Initializer.Builder(host, port, testName))
    lateinit var synchronizer: SdkSynchronizer

    val spendingKey: String get() = DerivationTool.deriveSpendingKeys(SimpleMnemonics().toSeed(seedPhrase.toCharArray()))[0]

    //
    // High-level APIs
    //

    /**
     * Setup dependencies, including the synchronizer and the darkside API connection
     */
    fun enterTheDarkside(): DarksideTestCoordinator = runBlocking {
        // verify that we are on the darkside
        try {
            twig("entering the darkside")
            initiate()
            synchronizer.getServerInfo().apply {
                assertTrue(
                    "Error: not on the darkside",
                    vendor.contains("dark", true)
                        or chainName.contains("dark", true)
                )
            }
            twig("darkside initiation complete!")
        } catch (error: StatusRuntimeException) {
            Assert.fail(
                "Error while fetching server status. Testing cannot begin due to:" +
                    " ${error.message} Caused by: ${error.cause} Verify that the server is running!"
            )
        }
        this@DarksideTestCoordinator
    }

    /**
     * Setup the synchronizer and darksidewalletd with their initial state
     */
    fun initiate() {
        twig("*************** INITIALIZING TEST COORDINATOR (ONLY ONCE) ***********************")
        val initializer = Initializer(context) { config ->
            config.seedPhrase(seedPhrase)
            config.setBirthdayHeight(birthdayHeight)
            config.alias = testName
        }
        synchronizer = Synchronizer(initializer) as SdkSynchronizer
        val channel = (synchronizer as SdkSynchronizer).channel
        darkside = DarksideApi(channel)
    }

//    fun triggerSmallReorg() {
//        darkside.setBlocksUrl(smallReorg)
//    }
//
//    fun triggerLargeReorg() {
//        darkside.setBlocksUrl(largeReorg)
//    }

    fun startSync(scope: CoroutineScope): DarksideTestCoordinator = apply {
        synchronizer.start(scope)
    }

    /**
     * Waits for, at most, the given amount of time for the synchronizer to download and scan blocks
     * and reach a 'SYNCED' status.
     */
    fun await(timeout: Long = 60_000L, targetHeight: Int = -1) = runBlocking {
        ScopedTest.timeoutWith(this, timeout) {
            twig("***  Waiting up to ${timeout / 1_000}s for sync ***")
            synchronizer.status.onEach {
                twig("got processor status $it")
                if (it == Synchronizer.Status.DISCONNECTED) {
                    twig("waiting a bit before giving up on connection...")
                } else if (targetHeight != -1 && (synchronizer as SdkSynchronizer).processor.getLastScannedHeight() < targetHeight) {
                    twig("awaiting new blocks from server...")
                }
            }.map {
                // whenever we're waiting for a target height, for simplicity, if we're sleeping,
                // and in between polls, then consider it that we're not synced
                if (targetHeight != -1 && (synchronizer as SdkSynchronizer).processor.getLastScannedHeight() < targetHeight) {
                    twig("switching status to DOWNLOADING because we're still waiting for height $targetHeight")
                    Synchronizer.Status.DOWNLOADING
                } else {
                    it
                }
            }.filter { it == Synchronizer.Status.SYNCED }.first()
            twig("***  Done waiting for sync! ***")
        }
    }

    /**
     * Send a transaction and wait until it has been fully created and successfully submitted, which
     * takes about 10 seconds.
     */
    suspend fun createAndSubmitTx(
        zatoshi: Long,
        toAddress: String,
        memo: String = "",
        fromAccountIndex: Int = 0
    ) = coroutineScope {
        Twig.sprout("sending")
        var job: Job? = null
        job = synchronizer
            .sendToAddress(spendingKey, zatoshi, toAddress, memo, fromAccountIndex)
            .onEach {
                twig("got an update submitted yet? ${it.isSubmitSuccess()}")
                if (it.isSubmitSuccess()) job?.cancel()
            }.launchIn(this)
        job.join()
        Twig.clip("sending")
    }

    fun stall(delay: Long = 5000L) = runBlocking {
        twig("***  Stalling for ${delay}ms ***")
        delay(delay)
    }

    //
    // Validation
    //

    inner class DarksideTestValidator {

        fun validateHasBlock(height: Int) {
            assertTrue((synchronizer as SdkSynchronizer).findBlockHashAsHex(height) != null)
            assertTrue((synchronizer as SdkSynchronizer).findBlockHash(height)?.size ?: 0 > 0)
        }

        fun validateLatestHeight(height: Int) = runBlocking<Unit> {
            val info = synchronizer.processorInfo.first()
            val networkBlockHeight = info.networkBlockHeight
            assertTrue(
                "Expected latestHeight of $height but the server last reported a height of" +
                    " $networkBlockHeight! Full details: $info",
                networkBlockHeight == height
            )
        }

        fun validateMinHeightDownloaded(minHeight: Int) = runBlocking<Unit> {
            val info = synchronizer.processorInfo.first()
            val lastDownloadedHeight = info.lastDownloadedHeight
            assertTrue(
                "Expected to have at least downloaded $minHeight but the last downloaded block was" +
                    " $lastDownloadedHeight! Full details: $info",
                lastDownloadedHeight >= minHeight
            )
        }

        fun validateMinHeightScanned(minHeight: Int) = runBlocking<Unit> {
            val info = synchronizer.processorInfo.first()
            val lastScannedHeight = info.lastScannedHeight
            assertTrue(
                "Expected to have at least scanned $minHeight but the last scanned block was" +
                    " $lastScannedHeight! Full details: $info",
                lastScannedHeight >= minHeight
            )
        }

        fun validateMaxHeightScanned(maxHeight: Int) = runBlocking<Unit> {
            val lastDownloadedHeight = synchronizer.processorInfo.first().lastScannedHeight
            assertTrue(
                "Did not expect to be synced beyond $maxHeight but we are synced to" +
                    " $lastDownloadedHeight",
                lastDownloadedHeight <= maxHeight
            )
        }

        fun validateBlockHash(height: Int, expectedHash: String) {
            val hash = (synchronizer as SdkSynchronizer).findBlockHashAsHex(height)
            assertEquals(expectedHash, hash)
        }

        fun onReorg(callback: (errorHeight: Int, rewindHeight: Int) -> Unit) {
            synchronizer.onChainErrorHandler = callback
        }

        fun validateTxCount(count: Int) {
            val txCount = (synchronizer as SdkSynchronizer).getTransactionCount()
            assertEquals("Expected $count transactions but found $txCount instead!", count, txCount)
        }

        fun validateMinBalance(available: Long = -1, total: Long = -1) {
            val balance = synchronizer.latestBalance
            if (available > 0) {
                assertTrue("invalid available balance. Expected a minimum of $available but found ${balance.availableZatoshi}", available <= balance.availableZatoshi)
            }
            if (total > 0) {
                assertTrue("invalid total balance. Expected a minimum of $total but found ${balance.totalZatoshi}", total <= balance.totalZatoshi)
            }
        }
        suspend fun validateBalance(available: Long = -1, total: Long = -1, accountIndex: Int = 0) {
            val balance = synchronizer.processor.getBalanceInfo(accountIndex)
            if (available > 0) {
                assertEquals("invalid available balance", available, balance.availableZatoshi)
            }
            if (total > 0) {
                assertEquals("invalid total balance", total, balance.totalZatoshi)
            }
        }
    }

    //
    // Chain Creations
    //

    inner class DarksideChainMaker {
        var lastTipHeight = -1

        /**
         * Resets the darksidelightwalletd server, stages the blocks represented by the given URL, then
         * applies those changes and waits for them to take effect.
         */
        fun resetBlocks(
            blocksUrl: String,
            startHeight: Int = DEFAULT_START_HEIGHT,
            tipHeight: Int = startHeight + 100
        ): DarksideChainMaker = apply {
            darkside
                .reset(startHeight)
                .stageBlocks(blocksUrl)
            applyTipHeight(tipHeight)
        }

        fun stageTransaction(url: String, targetHeight: Int): DarksideChainMaker = apply {
            darkside.stageTransactions(url, targetHeight)
        }

        fun stageTransactions(targetHeight: Int, vararg urls: String): DarksideChainMaker = apply {
            urls.forEach {
                darkside.stageTransactions(it, targetHeight)
            }
        }

        fun stageEmptyBlocks(startHeight: Int, count: Int = 10): DarksideChainMaker = apply {
            darkside.stageEmptyBlocks(startHeight, count)
        }

        fun stageEmptyBlock() = stageEmptyBlocks(lastTipHeight + 1, 1)

        fun applyTipHeight(tipHeight: Int): DarksideChainMaker = apply {
            twig("applying tip height of $tipHeight")
            darkside.applyBlocks(tipHeight)
            lastTipHeight = tipHeight
        }

        /**
         * Creates a chain with 100 blocks and a transaction in the middle.
         *
         * The chain starts at block 663150 and ends at block 663250
         */
        fun makeSimpleChain() {
            darkside
                .reset(DEFAULT_START_HEIGHT)
                .stageBlocks("https://raw.githubusercontent.com/zcash-hackworks/darksidewalletd-test-data/master/tx-incoming/blocks.txt")
            applyTipHeight(DEFAULT_START_HEIGHT + 100)
        }

        fun advanceBy(numEmptyBlocks: Int) {
            val nextBlock = lastTipHeight + 1
            twig("adding $numEmptyBlocks empty blocks to the chain starting at $nextBlock")
            darkside.stageEmptyBlocks(nextBlock, numEmptyBlocks)
            applyTipHeight(nextBlock + numEmptyBlocks)
        }

        fun applyPendingTransactions(targetHeight: Int = lastTipHeight + 1) {
            stageEmptyBlocks(lastTipHeight + 1, targetHeight - lastTipHeight)
            darkside.stageTransactions(darkside.getSentTransactions()?.iterator(), targetHeight)
            applyTipHeight(targetHeight)
        }
    }

    companion object {
        // Block URLS
        private const val beforeReorg =
            "https://raw.githubusercontent.com/zcash-hackworks/darksidewalletd-test-data/master/basic-reorg/before-reorg.txt"
        private const val smallReorg =
            "https://raw.githubusercontent.com/zcash-hackworks/darksidewalletd-test-data/master/basic-reorg/after-small-reorg.txt"
        private const val largeReorg =
            "https://raw.githubusercontent.com/zcash-hackworks/darksidewalletd-test-data/master/basic-reorg/after-large-reorg.txt"
        private const val DEFAULT_START_HEIGHT = 663150
    }
}
