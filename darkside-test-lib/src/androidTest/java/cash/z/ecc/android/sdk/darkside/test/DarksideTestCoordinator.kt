package cash.z.ecc.android.sdk.darkside.test

import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.internal.twig
import cash.z.ecc.android.sdk.model.Account
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.Darkside
import cash.z.ecc.android.sdk.model.ZcashNetwork
import co.electriccoin.lightwallet.client.internal.DarksideApi
import co.electriccoin.lightwallet.client.internal.new
import co.electriccoin.lightwallet.client.model.BlockHeightUnsafe
import co.electriccoin.lightwallet.client.model.LightWalletEndpoint
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue

class DarksideTestCoordinator(val wallet: TestWallet) {
    constructor(
        alias: String = "DarksideTestCoordinator",
        seedPhrase: String = DEFAULT_SEED_PHRASE,
        startHeight: BlockHeight = DEFAULT_START_HEIGHT,
        network: ZcashNetwork = ZcashNetwork.Mainnet,
        endpoint: LightWalletEndpoint = LightWalletEndpoint.Darkside
    ) : this(TestWallet(seedPhrase, alias, network, endpoint, startHeight = startHeight))

    private val targetHeight = BlockHeight.new(wallet.network, 663250)
    private val context = InstrumentationRegistry.getInstrumentation().context

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
    fun enterTheDarkside(): DarksideTestCoordinator = runBlocking {
        // verify that we are on the darkside
        try {
            twig("entering the darkside")
            initiate()

            // In the future, we may want to have the SDK internally verify being on the darkside by matching the network type

            // synchronizer.getServerInfo().apply {
            //     assertTrue(
            //         "Error: not on the darkside",
            //         vendor.contains("dark", true)
            //             or chainName.contains("dark", true)
            //     )
            // }
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
        darkside = DarksideApi.new(ApplicationProvider.getApplicationContext(), LightWalletEndpoint.Darkside)
        darkside.reset(BlockHeightUnsafe(wallet.network.saplingActivationHeight.value))
    }

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
    fun await(timeout: Long = 60_000L, targetHeight: BlockHeight? = null) = runBlocking {
        ScopedTest.timeoutWith(this, timeout) {
            twig("***  Waiting up to ${timeout / 1_000}s for sync ***")
            synchronizer.status.onEach {
                twig("got processor status $it")
                if (it == Synchronizer.Status.DISCONNECTED) {
                    twig("waiting a bit before giving up on connection...")
                } else if (targetHeight != null && synchronizer.processor.getLastScannedHeight() < targetHeight) {
                    twig("awaiting new blocks from server...")
                }
            }.map {
                // whenever we're waiting for a target height, for simplicity, if we're sleeping,
                // and in between polls, then consider it that we're not synced
                if (targetHeight != null && synchronizer.processor.getLastScannedHeight() < targetHeight) {
                    twig("switching status to DOWNLOADING because we're still waiting for height $targetHeight")
                    Synchronizer.Status.DOWNLOADING
                } else {
                    it
                }
            }.filter { it == Synchronizer.Status.SYNCED }.first()
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

    fun stall(delay: Long = 5000L) = runBlocking {
        twig("***  Stalling for ${delay}ms ***")
        delay(delay)
    }

    //
    // Validation
    //

    inner class DarksideTestValidator {

        fun validateHasBlock(height: BlockHeight) {
            runBlocking {
                assertTrue(synchronizer.findBlockHashAsHex(height) != null)
                assertTrue(synchronizer.findBlockHash(height)?.size ?: 0 > 0)
            }
        }

        fun validateLatestHeight(height: BlockHeight) = runBlocking<Unit> {
            val info = synchronizer.processorInfo.first()
            val networkBlockHeight = info.networkBlockHeight
            assertTrue(
                "Expected latestHeight of $height but the server last reported a height of" +
                    " $networkBlockHeight! Full details: $info",
                networkBlockHeight == height
            )
        }

        fun validateMinHeightDownloaded(minHeight: BlockHeight) = runBlocking<Unit> {
            val info = synchronizer.processorInfo.first()
            val lastDownloadedHeight = info.lastDownloadedHeight
            assertNotNull(lastDownloadedHeight)
            assertTrue(
                "Expected to have at least downloaded $minHeight but the last downloaded block was" +
                    " $lastDownloadedHeight! Full details: $info",
                lastDownloadedHeight!! >= minHeight
            )
        }

        fun validateMinHeightScanned(minHeight: BlockHeight) = runBlocking<Unit> {
            val info = synchronizer.processorInfo.first()
            val lastScannedHeight = info.lastScannedHeight
            assertNotNull(lastScannedHeight)
            assertTrue(
                "Expected to have at least scanned $minHeight but the last scanned block was" +
                    " $lastScannedHeight! Full details: $info",
                lastScannedHeight!! >= minHeight
            )
        }

        fun validateMaxHeightScanned(maxHeight: BlockHeight) = runBlocking<Unit> {
            val lastDownloadedHeight = synchronizer.processorInfo.first().lastScannedHeight
            assertNotNull(lastDownloadedHeight)
            assertTrue(
                "Did not expect to be synced beyond $maxHeight but we are synced to" +
                    " $lastDownloadedHeight",
                lastDownloadedHeight!! <= maxHeight
            )
        }

        fun validateBlockHash(height: BlockHeight, expectedHash: String) {
            val hash = runBlocking { synchronizer.findBlockHashAsHex(height) }
            assertEquals(expectedHash, hash)
        }

        fun onReorg(callback: (errorHeight: BlockHeight, rewindHeight: BlockHeight) -> Unit) {
            synchronizer.onChainErrorHandler = callback
        }

        fun validateTxCount(count: Int) {
            val txCount = runBlocking { synchronizer.getTransactionCount() }
            assertEquals("Expected $count transactions but found $txCount instead!", count, txCount)
        }

        fun validateMinBalance(available: Long = -1, total: Long = -1) {
            val balance = synchronizer.saplingBalances.value
            if (available > 0) {
                assertTrue("invalid available balance. Expected a minimum of $available but found ${balance?.available}", available <= balance?.available?.value!!)
            }
            if (total > 0) {
                assertTrue("invalid total balance. Expected a minimum of $total but found ${balance?.total}", total <= balance?.total?.value!!)
            }
        }
        suspend fun validateBalance(available: Long = -1, total: Long = -1, account: Account) {
            val balance = synchronizer.processor.getBalanceInfo(account)
            if (available > 0) {
                assertEquals("invalid available balance", available, balance.available)
            }
            if (total > 0) {
                assertEquals("invalid total balance", total, balance.total)
            }
        }
    }

    //
    // Chain Creations
    //

    inner class DarksideChainMaker {
        var lastTipHeight: BlockHeight? = null

        /**
         * Resets the darksidelightwalletd server, stages the blocks represented by the given URL, then
         * applies those changes and waits for them to take effect.
         */
        fun resetBlocks(
            blocksUrl: String,
            startHeight: BlockHeight = DEFAULT_START_HEIGHT,
            tipHeight: BlockHeight = startHeight + 100
        ): DarksideChainMaker = apply {
            darkside
                .reset(BlockHeightUnsafe(startHeight.value))
                .stageBlocks(blocksUrl)
            applyTipHeight(tipHeight)
        }

        fun stageTransaction(url: String, targetHeight: BlockHeight): DarksideChainMaker = apply {
            darkside.stageTransactions(url, BlockHeightUnsafe(targetHeight.value))
        }

        fun stageTransactions(targetHeight: BlockHeight, vararg urls: String): DarksideChainMaker = apply {
            urls.forEach {
                darkside.stageTransactions(it, BlockHeightUnsafe(targetHeight.value))
            }
        }

        fun stageEmptyBlocks(startHeight: BlockHeight, count: Int = 10): DarksideChainMaker = apply {
            darkside.stageEmptyBlocks(BlockHeightUnsafe(startHeight.value), count)
        }

        fun stageEmptyBlock() = stageEmptyBlocks(lastTipHeight!! + 1, 1)

        fun applyTipHeight(tipHeight: BlockHeight): DarksideChainMaker = apply {
            twig("applying tip height of $tipHeight")
            darkside.applyBlocks(BlockHeightUnsafe(tipHeight.value))
            lastTipHeight = tipHeight
        }

        /**
         * Creates a chain with 100 blocks and a transaction in the middle.
         *
         * The chain starts at block 663150 and ends at block 663250
         */
        fun makeSimpleChain() {
            darkside
                .reset(BlockHeightUnsafe(DEFAULT_START_HEIGHT.value))
                .stageBlocks("https://raw.githubusercontent.com/zcash-hackworks/darksidewalletd-test-data/master/tx-incoming/blocks.txt")
            applyTipHeight(DEFAULT_START_HEIGHT + 100)
        }

        fun advanceBy(numEmptyBlocks: Int) {
            val nextBlock = lastTipHeight!! + 1
            twig("adding $numEmptyBlocks empty blocks to the chain starting at $nextBlock")
            darkside.stageEmptyBlocks(BlockHeightUnsafe(nextBlock.value), numEmptyBlocks)
            applyTipHeight(nextBlock + numEmptyBlocks)
        }

        fun applyPendingTransactions(targetHeight: BlockHeight = lastTipHeight!! + 1) {
            stageEmptyBlocks(lastTipHeight!! + 1, (targetHeight.value - lastTipHeight!!.value).toInt())
            darkside.stageTransactions(darkside.getSentTransactions()?.iterator(), BlockHeightUnsafe(targetHeight.value))
            applyTipHeight(targetHeight)
        }
    }

    companion object {
        /**
         * This is a special localhost value on the Android emulator, which allows it to contact
         * the localhost of the computer running the emulator.
         */
        const val COMPUTER_LOCALHOST = "10.0.2.2"

        // Block URLS
        private const val beforeReorg =
            "https://raw.githubusercontent.com/zcash-hackworks/darksidewalletd-test-data/master/basic-reorg/before-reorg.txt"
        private const val smallReorg =
            "https://raw.githubusercontent.com/zcash-hackworks/darksidewalletd-test-data/master/basic-reorg/after-small-reorg.txt"
        private const val largeReorg =
            "https://raw.githubusercontent.com/zcash-hackworks/darksidewalletd-test-data/master/basic-reorg/after-large-reorg.txt"
        private val DEFAULT_START_HEIGHT = BlockHeight.new(ZcashNetwork.Mainnet, 663150)
        private const val DEFAULT_SEED_PHRASE =
            "still champion voice habit trend flight survey between bitter process artefact blind carbon truly provide dizzy crush flush breeze blouse charge solid fish spread"
    }
}
