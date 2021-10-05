package cash.z.ecc.android.sdk.darkside.test

import cash.z.ecc.android.sdk.SdkSynchronizer
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.darkside.fixture.FixtureTransaction
import cash.z.ecc.android.sdk.ext.twig
import cash.z.ecc.android.sdk.type.ZcashNetwork
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

class DarksideTestCoordinator(val wallet: TestWallet = newDarksideTestWallet()) {

    // dependencies: private
    private lateinit var darkside: DarksideApi

    // dependencies: public
    val validator = DarksideTestValidator()
    val chainMaker = DarksideChainMaker()

    // wallet delegates
    val synchronizer get() = wallet.synchronizer
    val send get() = wallet::send
    //
    // High-level APIs
    //

    /**
     * Setup dependencies, including the synchronizer and the darkside API connection
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun enterTheDarkside(): DarksideTestCoordinator = runBlocking {
        // verify that we are on the darkside
        try {
            twig("entering the darkside")
            darkside = DarksideApi(synchronizer.channel)
            darkside.reset()
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
                        " ${error.message} Caused by: ${error.cause} Verify that the server is running! Please see https://github.com/zcash/zcash-android-wallet-sdk/blob/master/docs/tests/Darkside.md"
            )
        }
        this@DarksideTestCoordinator
    }

    fun reset(
        saplingActivationHeight: Int,
        branchId: String,
        chainName: String
    ) = darkside.reset(saplingActivationHeight, branchId, chainName)

    fun stageBlocks(url: String) = darkside.stageBlocks(url)

    fun stageEmptyBlocks(startHeight: Int,
                         count: Int = 10,
                         nonce: Int = Random.nextInt()) =
            darkside.stageEmptyBlocks(startHeight,count, nonce)
//    fun triggerSmallReorg() {
//        darkside.setBlocksUrl(smallReorg)
//    }
//
//    fun triggerLargeReorg() {
//        darkside.setBlocksUrl(largeReorg)
//    }

    // redo this as a call to wallet but add delay time to wallet join() function
    /**
     * Waits for, at most, the given amount of time for the synchronizer to download and scan blocks
     * and reach a 'SYNCED' status.
     */
    @OptIn(ExperimentalTime::class)
    fun await(timeout: Duration = Duration.seconds(15), targetHeight: Int = -1) = runBlocking {
        withTimeout(timeout) {
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
            }.filter {
                it == Synchronizer.Status.SYNCED
            }.first()

            twig("***  Done waiting for sync! ***")
        }
    }

//    /**
//     * Send a transaction and wait until it has been fully created and successfully submitted, which
//     * takes about 10 seconds.
//     */
//    suspend fun createAndSubmitTx(
//        zatoshi: Long,
//        toAddress: String,
//        memo: String = "",
//        fromAccountIndex: Int = 0
//    ) = coroutineScope {
//
//        wallet.send(toAddress, memo, zatoshi, fromAccountIndex)
//    }

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
            val balance = synchronizer.saplingBalances.value
            if (available > 0) {
                assertTrue("invalid available balance. Expected a minimum of $available but found ${balance.availableZatoshi}", available <= balance.availableZatoshi)
            }
            if (total > 0) {
                assertTrue("invalid total balance. Expected a minimum of $total but found ${balance.totalZatoshi}", total <= balance.totalZatoshi)
            }
        }
        suspend fun validateBalance(available: Long = -1, total: Long = -1, accountIndex: Int = 0) {
            val balance = (synchronizer as SdkSynchronizer).processor.getBalanceInfo(accountIndex)
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
            startHeight: Int = DarksideNetwork.DEFAULT_START_HEIGHT,
            tipHeight: Int = startHeight + 100
        ): DarksideChainMaker = apply {
            darkside
                .reset(startHeight)
                .stageBlocks(blocksUrl)
            applyTipHeight(tipHeight)
        }

        fun stageTransaction(transaction: FixtureTransaction) =
            stageTransaction(transaction.url, transaction.height)

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
                .reset(DarksideNetwork.DEFAULT_START_HEIGHT)
                .stageBlocks("https://raw.githubusercontent.com/zcash-hackworks/darksidewalletd-test-data/master/tx-incoming/blocks.txt")
            applyTipHeight(DarksideNetwork.DEFAULT_START_HEIGHT + 100)
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

        private const val DEFAULT_SEED_PHRASE =
            "still champion voice habit trend flight survey between bitter process artefact blind carbon truly provide dizzy crush flush breeze blouse charge solid fish spread"

        /**
         * @return A wallet appropriate for passing into [DarksideTestCoordinator].
         */
        fun newDarksideTestWallet(
            alias: String = "DarksideTestCoordinator",
            seedPhrase: String = DEFAULT_SEED_PHRASE,
            startHeight: Int = DarksideNetwork.DEFAULT_START_HEIGHT,
            network: ZcashNetwork = DarksideNetwork(saplingActivationHeight = startHeight),
        ) = TestWallet(
            seedPhrase,
            alias,
            network,
            network.defaultHost,
            startHeight = startHeight,
            port = network.defaultPort
        )
    }
}

private data class DarksideNetwork(
    override val id: Int = 1,
    override val networkName: String = "mainnet",
    override val saplingActivationHeight: Int = DEFAULT_START_HEIGHT,
    override val defaultHost: String = COMPUTER_LOCALHOST,
    override val defaultPort: Int = DEFAULT_PORT
) : ZcashNetwork {

    companion object {
        /**
         * This is a special localhost value on the Android emulator, which allows it to contact
         * the localhost of the computer running the emulator.
         */
        private const val COMPUTER_LOCALHOST = "10.0.2.2"

        internal const val DEFAULT_START_HEIGHT = 663_150
        private const val DEFAULT_PORT = 9067
    }
}