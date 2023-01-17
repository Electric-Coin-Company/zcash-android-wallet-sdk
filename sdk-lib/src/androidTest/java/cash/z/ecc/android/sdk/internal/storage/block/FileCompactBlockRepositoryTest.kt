package cash.z.ecc.android.sdk.internal.storage.block

import androidx.test.platform.app.InstrumentationRegistry
import cash.z.ecc.android.sdk.internal.ext.deleteRecursivelySuspend
import cash.z.ecc.android.sdk.internal.ext.existsSuspend
import cash.z.ecc.android.sdk.internal.ext.mkdirsSuspend
import cash.z.ecc.android.sdk.internal.service.LightWalletGrpcService
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.LightWalletEndpoint
import cash.z.ecc.android.sdk.model.Mainnet
import cash.z.ecc.android.sdk.model.Testnet
import cash.z.ecc.android.sdk.model.ZcashNetwork
import cash.z.ecc.fixture.DatabasePathFixture
import cash.z.ecc.fixture.FakeRustBackend
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FileCompactBlockRepositoryTest {

    private val rustBackend = FakeRustBackend()
    private lateinit var compactBlockRepository: FileCompactBlockRepository

    private val lightwalletdHost = LightWalletEndpoint.Testnet
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setup() = runTest {
        val file = File(DatabasePathFixture.new())
        val blocksDir = File(file, "blocks")

        if (file.existsSuspend()) {
            file.deleteRecursivelySuspend()
        }
        if (blocksDir.existsSuspend()) {
            blocksDir.deleteRecursivelySuspend()
        }
        blocksDir.mkdirsSuspend()

        compactBlockRepository =
            FileCompactBlockRepository(ZcashNetwork.Testnet, file, rustBackend)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() = runTest {
        File(DatabasePathFixture.new()).deleteRecursivelySuspend()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun getLatestHeightTest() = runTest {
        val blockRange = BlockHeight.new(ZcashNetwork.Mainnet, 500_000)..BlockHeight.new(
            ZcashNetwork.Mainnet,
            500_009
        )

        val lightwalletService = LightWalletGrpcService.new(context, lightwalletdHost)
        val blocks = lightwalletService.getBlockRange(blockRange).toList()

        compactBlockRepository.write(blocks.asSequence())

        assertTrue { blocks.last().height == compactBlockRepository.getLatestHeight().value }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun writeBlockTest() = runTest {
        val blockRange = BlockHeight.new(ZcashNetwork.Mainnet, 500_000)..BlockHeight.new(
            ZcashNetwork.Mainnet,
            500_009
        )

        val lightwalletService = LightWalletGrpcService.new(context, lightwalletdHost)
        val blocks = lightwalletService.getBlockRange(blockRange)

        compactBlockRepository.write(blocks)

        assertTrue { rustBackend.metadata.size == 10 }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun rewindToTest() = runTest {
        val blockRange = BlockHeight.new(ZcashNetwork.Mainnet, 500_000)..BlockHeight.new(
            ZcashNetwork.Mainnet,
            500_009
        )

        val lightwalletService = LightWalletGrpcService.new(context, lightwalletdHost)
        val blocks = lightwalletService.getBlockRange(blockRange)

        val rewindHeight: Long = 500_005
        compactBlockRepository.write(blocks)

        compactBlockRepository.rewindTo(BlockHeight(rewindHeight))

        val metaData = rustBackend.metadata.filter { it.height > rewindHeight }
        assertTrue { metaData.isEmpty() }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun createTemporaryFileTest() = runTest {
        val blockRange = BlockHeight.new(ZcashNetwork.Mainnet, 500_000)..BlockHeight.new(
            ZcashNetwork.Mainnet,
            500_009
        )

        val lightwalletService = LightWalletGrpcService.new(context, lightwalletdHost)
        val blocks = lightwalletService.getBlockRange(blockRange)
        val block = blocks.first()

        val file = compactBlockRepository.createTemporaryFile(block, File(DatabasePathFixture.new(), "blocks"))

        assertTrue { file.existsSuspend() }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun finalizeFileTest() = runTest {
        val blockRange = BlockHeight.new(ZcashNetwork.Mainnet, 500_000)..BlockHeight.new(
            ZcashNetwork.Mainnet,
            500_009
        )

        val lightwalletService = LightWalletGrpcService.new(context, lightwalletdHost)
        val blocks = lightwalletService.getBlockRange(blockRange)
        val block = blocks.first()

        val file = compactBlockRepository.createTemporaryFile(block, File(DatabasePathFixture.new(), "blocks"))
        with(compactBlockRepository) {
            val finalizedFile = File(file.absolutePath.dropLast(4))
            assertFalse { finalizedFile.existsSuspend() }
            file.finalizeFile()
            assertTrue { finalizedFile.existsSuspend() }
        }
    }
}
