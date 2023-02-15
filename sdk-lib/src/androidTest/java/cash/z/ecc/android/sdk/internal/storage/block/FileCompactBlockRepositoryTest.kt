package cash.z.ecc.android.sdk.internal.storage.block

import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.internal.ext.deleteRecursivelySuspend
import cash.z.ecc.android.sdk.internal.ext.existsSuspend
import cash.z.ecc.android.sdk.internal.ext.mkdirsSuspend
import cash.z.ecc.android.sdk.jni.RustBackendWelding
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.ZcashNetwork
import cash.z.ecc.fixture.FakeRustBackendFixture
import cash.z.ecc.fixture.FilePathFixture
import co.electriccoin.lightwallet.client.fixture.ListOfCompactBlocksFixture
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FileCompactBlockRepositoryTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setup() = runTest {
        val rootDirectory = FilePathFixture.newBlocksDir()
        if (rootDirectory.existsSuspend()) {
            rootDirectory.deleteRecursivelySuspend()
        }

        val blocksDir = FilePathFixture.newBlocksDir()
        blocksDir.mkdirsSuspend()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() = runTest {
        FilePathFixture.newBlocksDir().deleteRecursivelySuspend()
    }

    private fun getMockedFileCompactBlockRepository(
        rustBackend: RustBackendWelding,
        rootBlocksDirectory: File
    ): FileCompactBlockRepository = runBlocking {
        FileCompactBlockRepository(
            rootBlocksDirectory,
            rustBackend
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun getLatestHeightTest() = runTest {
        val rustBackend = FakeRustBackendFixture().new()
        val blockRepository = getMockedFileCompactBlockRepository(rustBackend, FilePathFixture.newBlocksDir())

        val blocks = ListOfCompactBlocksFixture.new()

        blockRepository.write(blocks)

        assertEquals(blocks.last().height, blockRepository.getLatestHeight()?.value)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun findCompactBlockTest() = runTest {
        val network = ZcashNetwork.Testnet
        val rustBackend = FakeRustBackendFixture().new()
        val blockRepository = getMockedFileCompactBlockRepository(rustBackend, FilePathFixture.newBlocksDir())

        val blocks = ListOfCompactBlocksFixture.new()

        blockRepository.write(blocks)

        val firstPersistedBlock = blockRepository.findCompactBlock(
            BlockHeight.new(network, blocks.first().height)
        )
        val lastPersistedBlock = blockRepository.findCompactBlock(
            BlockHeight.new(network, blocks.last().height)
        )
        val notPersistedBlockHeight = BlockHeight.new(
            network,
            blockHeight = blocks.last().height + 1
        )

        assertNotNull(firstPersistedBlock)
        assertNotNull(lastPersistedBlock)

        assertEquals(blocks.first().height, firstPersistedBlock.height)
        assertEquals(blocks.last().height, blockRepository.getLatestHeight()?.value)
        assertNull(blockRepository.findCompactBlock(notPersistedBlockHeight))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun writeBlocksTest() = runTest {
        val rustBackend = FakeRustBackendFixture().new()
        val blockRepository = getMockedFileCompactBlockRepository(rustBackend, FilePathFixture.newBlocksDir())

        assertTrue { rustBackend.metadata.isEmpty() }

        val blocks = ListOfCompactBlocksFixture.new()
        val persistedBlocksCount = blockRepository.write(blocks)

        assertEquals(blocks.count(), persistedBlocksCount)
        assertEquals(blocks.count(), rustBackend.metadata.size)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun writeFewBlocksTest() = runTest {
        val rustBackend = FakeRustBackendFixture().new()
        val blockRepository = getMockedFileCompactBlockRepository(rustBackend, FilePathFixture.newBlocksDir())

        assertTrue { rustBackend.metadata.isEmpty() }

        // prepare a list of blocks to be persisted, which has smaller size than buffer size
        val reducedBlocksList = ListOfCompactBlocksFixture.new().apply {
            val reduced = drop(count() / 2)
            assertTrue { reduced.count() < ZcashSdk.BLOCKS_METADATA_BUFFER_SIZE }
        }

        val persistedBlocksCount = blockRepository.write(reducedBlocksList)

        assertEquals(reducedBlocksList.count(), persistedBlocksCount)
        assertEquals(reducedBlocksList.count(), rustBackend.metadata.size)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun writeBlocksAndCheckStorageTest() = runTest {
        val rustBackend = FakeRustBackendFixture().new()
        val rootBlocksDirectory = FilePathFixture.newBlocksDir()
        val blockRepository = getMockedFileCompactBlockRepository(rustBackend, rootBlocksDirectory)

        assertTrue { rootBlocksDirectory.exists() }
        assertTrue { rootBlocksDirectory.list()!!.isEmpty() }

        val blocks = ListOfCompactBlocksFixture.new()
        val persistedBlocksCount = blockRepository.write(blocks)

        assertTrue { rootBlocksDirectory.exists() }
        assertEquals(blocks.count(), persistedBlocksCount)
        assertEquals(blocks.count(), rootBlocksDirectory.list()!!.size)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun rewindToTest() = runTest {
        val rustBackend = FakeRustBackendFixture().new()
        val blockRepository = getMockedFileCompactBlockRepository(rustBackend, FilePathFixture.newBlocksDir())

        val testedBlocksRange = ListOfCompactBlocksFixture.DEFAULT_FILE_BLOCK_RANGE

        val blocks = ListOfCompactBlocksFixture.new(testedBlocksRange)
        blockRepository.write(blocks)

        val blocksRangeMiddleValue = testedBlocksRange.run {
            start.value.plus(endInclusive.value).div(2)
        }
        val rewindHeight: Long = blocksRangeMiddleValue
        blockRepository.rewindTo(BlockHeight(rewindHeight))

        // suppose to be 0
        val keptMetadataAboveRewindHeight = rustBackend.metadata
            .filter { it.height > rewindHeight }

        assertTrue { keptMetadataAboveRewindHeight.isEmpty() }

        val expectedRewoundMetadataCount =
            (testedBlocksRange.endInclusive.value - blocksRangeMiddleValue).toInt()

        assertEquals(expectedRewoundMetadataCount, blocks.count() - rustBackend.metadata.size)

        val expectedKeptMetadataCount =
            (blocks.count() - expectedRewoundMetadataCount)

        assertEquals(expectedKeptMetadataCount, rustBackend.metadata.size)

        val keptMetadataBelowRewindHeight = rustBackend.metadata
            .filter { it.height <= rewindHeight }

        assertEquals(expectedKeptMetadataCount, keptMetadataBelowRewindHeight.size)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun createTemporaryFileTest() = runTest {
        val blocksDir = FilePathFixture.newBlocksDir()
        val blocks = ListOfCompactBlocksFixture.new()
        val block = blocks.first()

        val file = block.createTemporaryFile(blocksDir)

        assertTrue { file.existsSuspend() }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun finalizeFileTest() = runTest {
        val blocksDir = FilePathFixture.newBlocksDir()
        val blocks = ListOfCompactBlocksFixture.new()
        val block = blocks.first()

        val tempFile = block.createTemporaryFile(blocksDir)

        val finalizedFile = File(tempFile.absolutePath.dropLast(ZcashSdk.TEMPORARY_FILENAME_SUFFIX.length))
        assertFalse { finalizedFile.existsSuspend() }

        tempFile.finalizeFile()
        assertTrue { finalizedFile.existsSuspend() }

        assertFalse { tempFile.existsSuspend() }
    }
}
