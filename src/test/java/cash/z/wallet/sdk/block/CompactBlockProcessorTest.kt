package cash.z.wallet.sdk.block

import cash.z.wallet.sdk.entity.CompactBlockEntity
import cash.z.wallet.sdk.ext.TroubleshootingTwig
import cash.z.wallet.sdk.ext.Twig
import cash.z.wallet.sdk.ext.ZcashSdk.SAPLING_ACTIVATION_HEIGHT
import cash.z.wallet.sdk.ext.twig
import cash.z.wallet.sdk.jni.RustBackend
import cash.z.wallet.sdk.service.LightWalletService
import cash.z.wallet.sdk.transaction.TransactionRepository
import com.nhaarman.mockitokotlin2.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CompactBlockProcessorTest {

    // Mocks/Spys
    @Mock lateinit var rustBackend: RustBackend
    lateinit var processor: CompactBlockProcessor

    // Test variables
    private var latestBlockHeight: Int = 500_000
    private var lastDownloadedHeight: Int = SAPLING_ACTIVATION_HEIGHT
    private var lastScannedHeight: Int = SAPLING_ACTIVATION_HEIGHT
    private var errorBlock: Int = -1

    @BeforeEach
    fun setUp(
    @Mock lightwalletService: LightWalletService,
    @Mock compactBlockStore: CompactBlockStore,
    @Mock repository: TransactionRepository
    ) {
        Twig.plant(TroubleshootingTwig())

     lightwalletService.stub {
            onBlocking {
                getBlockRange(any())
            }.thenAnswer { invocation ->
                val range = invocation.arguments[0] as IntRange
                range.map { CompactBlockEntity(it, ByteArray(0)) }
            }
        }
        lightwalletService.stub {
            onBlocking {
                getLatestBlockHeight()
            }.thenAnswer { latestBlockHeight }
        }

        compactBlockStore.stub {
            onBlocking {
                write(any())
            }.thenAnswer { invocation ->
                val lastBlockHeight = (invocation.arguments[0] as List<CompactBlockEntity>).last().height
                lastDownloadedHeight = lastBlockHeight
                Unit
            }
        }
        compactBlockStore.stub {
            onBlocking {
                getLatestHeight()
            }.thenAnswer { lastDownloadedHeight }
        }
        compactBlockStore.stub {
            onBlocking {
                rewindTo(any())
            }.thenAnswer { invocation ->
                lastDownloadedHeight = invocation.arguments[0] as Int
                Unit
            }
        }
        repository.stub {
            onBlocking {
                lastScannedHeight()
            }.thenAnswer { lastScannedHeight }
        }

        val downloader = spy(CompactBlockDownloader(lightwalletService, compactBlockStore))
        processor = spy(CompactBlockProcessor(downloader, repository, rustBackend))

        whenever(rustBackend.validateCombinedChain()).thenAnswer {
            errorBlock
        }

        whenever(rustBackend.scanBlocks()).thenAnswer {
            true
        }
    }

    @Test
    @Timeout(5)
    fun `check for OBOE when downloading`() = runBlocking {
        // if the last block downloaded was 350_000, then we already have that block and should start with 350_001
        lastDownloadedHeight = 550_000

        processBlocksThen {
            verify(processor).downloadNewBlocks(350_001..latestBlockHeight)
        }
    }

    @Test
    @Timeout(5)
    fun `chain error rewinds by expected amount`() = runBlocking {
        // if the highest block whose prevHash doesn't match happens at block 300_010
        errorBlock = 500_010

        // then  we should rewind the default (10) blocks
        val expectedBlock = errorBlock - 10
        processBlocksThen {
            twig("FINISHED PROCESSING!")
            verify(processor.downloader, atLeastOnce()).rewindToHeight(expectedBlock)
            verify(rustBackend, atLeastOnce()).rewindToHeight(expectedBlock)
            assertNotNull(processor)
        }
    }

    @Test
//    @Timeout(5)
    fun `chain error downloads expected number of blocks`() = runBlocking {
        // if the highest block whose prevHash doesn't match happens at block 300_010
        // and our rewind distance is the default (10), then we want to download exactly ten blocks
        errorBlock = 500_010

        // plus 1 because the range is inclusive
        val expectedRange = (errorBlock - 10 + 1)..latestBlockHeight
        processBlocksThen {
            verify(processor, atLeastOnce()).downloadNewBlocks(expectedRange)
        }
    }

    // TODO: fix the fact that flows cause this not to work as originally coded. With a channel, we can stop observing once we reach 100. A flow makes that more difficult. The SDK behavior is still the same but testing that behavior is a little tricky without some refactors.
    private suspend fun processBlocksThen(block: suspend () -> Unit) = runBlocking {
        val scope = this
        launch {
            processor.start()
        }
        processor.progress.collect { i ->
            if(i >= 100) {
                block()
                processor.stop()
            }
            twig("processed $i")
        }
        twig("Done processing!")
    }
}