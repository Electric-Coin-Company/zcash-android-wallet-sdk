package cash.z.ecc.android.sdk.internal.storage.block

import cash.z.ecc.android.sdk.internal.ext.deleteRecursivelySuspend
import cash.z.ecc.android.sdk.internal.ext.existsSuspend
import cash.z.ecc.android.sdk.internal.ext.mkdirsSuspend
import cash.z.ecc.android.sdk.internal.service.LightWalletGrpcService
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.LightWalletEndpoint
import cash.z.ecc.android.sdk.model.Mainnet
import cash.z.ecc.android.sdk.model.ZcashNetwork
import cash.z.ecc.android.sdk.test.getAppContext
import cash.z.ecc.fixture.DatabasePathFixture
import cash.z.ecc.fixture.FakeRustBackend
import cash.z.ecc.fixture.FakeRustBackendFixture
import cash.z.ecc.fixture.FileBlockRangeFixture
import cash.z.ecc.fixture.FilePathFixture
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FileCompactBlockRepositoryTest {

    private val rustBackend: FakeRustBackend
        get() = FakeRustBackendFixture.fakeRustBackend

    private lateinit var compactBlockRepository: FileCompactBlockRepository

    private val lightwalletdHost = LightWalletEndpoint.Mainnet
    private val context = getAppContext()

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setup() = runTest {
        val rootDir = FilePathFixture.rootDir
        val blocksDir = FilePathFixture.blocksDir

        if (rootDir.existsSuspend()) {
            rootDir.deleteRecursivelySuspend()
        }
        if (blocksDir.existsSuspend()) {
            blocksDir.deleteRecursivelySuspend()
        }
        blocksDir.mkdirsSuspend()

        compactBlockRepository =
            FileCompactBlockRepository(ZcashNetwork.Testnet, rootDir, rustBackend)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() = runTest {
        File(DatabasePathFixture.new()).deleteRecursivelySuspend()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun getLatestHeightTest() = runTest {
        val blockRange = FileBlockRangeFixture.new()

        val lightwalletService = LightWalletGrpcService.new(context, lightwalletdHost)
        val blocks = lightwalletService.getBlockRange(blockRange).toList()

        compactBlockRepository.write(blocks.asSequence())

        assertTrue { blocks.last().height == compactBlockRepository.getLatestHeight().value }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun writeBlockTest() = runTest {
        val blockRange = FileBlockRangeFixture.new()

        val lightwalletService = LightWalletGrpcService.new(context, lightwalletdHost)
        val blocks = lightwalletService.getBlockRange(blockRange)

        assertTrue { rustBackend.metadata.size == 0 }

        compactBlockRepository.write(blocks)

        assertTrue { rustBackend.metadata.size == 10 }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun rewindToTest() = runTest {
        val blockRange = FileBlockRangeFixture.new()

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
        val blockRange = FileBlockRangeFixture.new()

        val lightwalletService = LightWalletGrpcService.new(context, lightwalletdHost)
        val blocks = lightwalletService.getBlockRange(blockRange)
        val block = blocks.first()

        val file = compactBlockRepository.createTemporaryFile(block, File(DatabasePathFixture.new(), "blocks"))

        assertTrue { file.existsSuspend() }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun finalizeFileTest() = runTest {
        val blockRange = FileBlockRangeFixture.new()

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
