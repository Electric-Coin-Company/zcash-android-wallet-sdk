package cash.z.ecc.android.sdk.internal.storage.block

import cash.z.ecc.android.sdk.internal.ext.deleteRecursivelySuspend
import cash.z.ecc.android.sdk.internal.ext.existsSuspend
import cash.z.ecc.android.sdk.internal.ext.listSuspend
import cash.z.ecc.android.sdk.internal.ext.mkdirsSuspend
import cash.z.ecc.android.sdk.jni.Backend
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.ZcashNetwork
import cash.z.ecc.fixture.FakeRustBackendFixture
import cash.z.ecc.fixture.FilePathFixture
import co.electriccoin.lightwallet.client.fixture.ListOfCompactBlocksFixture
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.last
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
        val blocksDirectory = FilePathFixture.newBlocksDir()
        if (blocksDirectory.existsSuspend()) {
            blocksDirectory.deleteRecursivelySuspend()
        }

        blocksDirectory.mkdirsSuspend()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() = runTest {
        FilePathFixture.newBlocksDir().deleteRecursivelySuspend()
    }

    private fun getMockedFileCompactBlockRepository(
        rustBackend: Backend,
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

        val blocks = ListOfCompactBlocksFixture.newFlow()

        blockRepository.write(blocks)

        assertEquals(blocks.last().height, blockRepository.getLatestHeight()?.value)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun findCompactBlockTest() = runTest {
        val network = ZcashNetwork.Testnet
        val rustBackend = FakeRustBackendFixture().new()
        val blockRepository = getMockedFileCompactBlockRepository(rustBackend, FilePathFixture.newBlocksDir())

        val blocks = ListOfCompactBlocksFixture.newFlow()

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

        val blocks = ListOfCompactBlocksFixture.newFlow()
        val persistedCount = blockRepository.write(blocks)

        assertEquals(blocks.count(), persistedCount)
        assertEquals(blocks.count(), rustBackend.metadata.size)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun writeFewBlocksTest() = runTest {
        val rustBackend = FakeRustBackendFixture().new()
        val blockRepository = getMockedFileCompactBlockRepository(rustBackend, FilePathFixture.newBlocksDir())

        assertTrue { rustBackend.metadata.isEmpty() }

        // prepare a list of blocks to be persisted, which has smaller size than buffer size
        val reducedBlocksList = ListOfCompactBlocksFixture.newFlow().apply {
            val reduced = drop(count() / 2)
            assertTrue { reduced.count() < FileCompactBlockRepository.BLOCKS_METADATA_BUFFER_SIZE }
        }

        val persistedCount = blockRepository.write(reducedBlocksList)

        assertEquals(reducedBlocksList.count(), persistedCount)
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

        val blocks = ListOfCompactBlocksFixture.newFlow()

        val persistedCount = blockRepository.write(blocks)

        assertTrue { rootBlocksDirectory.exists() }
        assertEquals(blocks.count(), persistedCount)
        assertEquals(blocks.count(), rootBlocksDirectory.list()!!.size)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun deleteCompactBlockFilesTest() = runTest {
        val rustBackend = FakeRustBackendFixture().new()
        val blocksDirectory = FilePathFixture.newBlocksDir()
        val parentDirectory = blocksDirectory.parentFile!!

        val blockRepository = getMockedFileCompactBlockRepository(rustBackend, blocksDirectory)

        val testedBlocksRange = ListOfCompactBlocksFixture.DEFAULT_FILE_BLOCK_RANGE
        val blocks = ListOfCompactBlocksFixture.newFlow(testedBlocksRange)

        val persistedCount = blockRepository.write(blocks)

        parentDirectory.also {
            assertTrue(it.existsSuspend())
            assertTrue(it.listSuspend()!!.contains(FilePathFixture.DEFAULT_BLOCKS_DIR_NAME))
        }

        blocksDirectory.also {
            assertTrue(it.existsSuspend())
            assertEquals(blocks.count(), persistedCount)
        }

        blockRepository.deleteCompactBlockFiles()

        parentDirectory.also {
            assertTrue(it.existsSuspend())
            assertFalse(it.listSuspend()!!.contains(FilePathFixture.DEFAULT_BLOCKS_DIR_NAME))
        }

        blocksDirectory.also { blocksDir ->
            assertFalse(blocksDir.existsSuspend())
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun rewindToTest() = runTest {
        val rustBackend = FakeRustBackendFixture().new()
        val blockRepository = getMockedFileCompactBlockRepository(rustBackend, FilePathFixture.newBlocksDir())

        val testedBlocksRange = ListOfCompactBlocksFixture.DEFAULT_FILE_BLOCK_RANGE

        val blocks = ListOfCompactBlocksFixture.newFlow(testedBlocksRange)
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
        val blocks = ListOfCompactBlocksFixture.newSequence()
        val block = blocks.first()

        val file = block.createTemporaryFile(blocksDir)

        assertTrue { file.existsSuspend() }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun finalizeFileTest() = runTest {
        val blocksDir = FilePathFixture.newBlocksDir()
        val blocks = ListOfCompactBlocksFixture.newSequence()
        val block = blocks.first()

        val tempFile = block.createTemporaryFile(blocksDir)

        val finalizedFile = File(tempFile.absolutePath.dropLast(FileCompactBlockRepository.TEMPORARY_FILENAME_SUFFIX.length))
        assertFalse { finalizedFile.existsSuspend() }

        tempFile.finalizeFile()
        assertTrue { finalizedFile.existsSuspend() }

        assertFalse { tempFile.existsSuspend() }
    }
}
