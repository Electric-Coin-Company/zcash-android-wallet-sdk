package cash.z.ecc.android.sdk.internal.storage.block

import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.internal.ext.deleteRecursivelySuspend
import cash.z.ecc.android.sdk.internal.ext.existsSuspend
import cash.z.ecc.android.sdk.internal.ext.mkdirsSuspend
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.fixture.DatabasePathFixture
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
import kotlin.test.assertTrue

class FileCompactBlockRepositoryTest {

    private val fakeRustBackend = FakeRustBackendFixture.new()
    private lateinit var rootDirectory: File

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setup() = runTest {
        rootDirectory = FilePathFixture.newRootDir()
        val blocksDir = FilePathFixture.newBlocksDir()

        if (rootDirectory.existsSuspend()) {
            rootDirectory.deleteRecursivelySuspend()
        }
        if (blocksDir.existsSuspend()) {
            blocksDir.deleteRecursivelySuspend()
        }
        blocksDir.mkdirsSuspend()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() = runTest {
        File(DatabasePathFixture.new()).deleteRecursivelySuspend()
    }

    private fun getMockedFileCompactBlockRepository(): FileCompactBlockRepository = runBlocking {
        FileCompactBlockRepository(
            rootDirectory,
            fakeRustBackend
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun getLatestHeightTest() = runTest {
        val blocks = ListOfCompactBlocksFixture.new()

        val mockedBlockRepository = getMockedFileCompactBlockRepository()
        mockedBlockRepository.write(blocks)

        assertTrue { blocks.last().height == mockedBlockRepository.getLatestHeight()?.value }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun writeBlockTest() = runTest {
        val blocks = ListOfCompactBlocksFixture.new()

        assertTrue { fakeRustBackend.metadata.isEmpty() }

        val mockedBlockRepository = getMockedFileCompactBlockRepository()
        mockedBlockRepository.write(blocks)

        assertTrue { fakeRustBackend.metadata.size == blocks.count() }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun rewindToTest() = runTest {
        val testedBlocksRange = ListOfCompactBlocksFixture.DEFAULT_FILE_BLOCK_RANGE

        val blocksRangeMiddleValue = testedBlocksRange.run {
            start.value.plus(endInclusive.value).div(2)
        }

        val blocks = ListOfCompactBlocksFixture.new(testedBlocksRange)

        val rewindHeight: Long = blocksRangeMiddleValue

        val mockedBlockRepository = getMockedFileCompactBlockRepository()
        mockedBlockRepository.write(blocks)

        mockedBlockRepository.rewindTo(BlockHeight(rewindHeight))

        // suppose to be 0
        val keptMetadataAboveRewindHeight = fakeRustBackend.metadata
            .filter { it.height > rewindHeight }

        assertTrue { keptMetadataAboveRewindHeight.isEmpty() }

        val expectedRewoundMetadataCount =
            (testedBlocksRange.endInclusive.value - blocksRangeMiddleValue).toInt()

        assertEquals(expectedRewoundMetadataCount, blocks.count() - fakeRustBackend.metadata.size)

        val expectedKeptMetadataCount =
            (blocks.count() - expectedRewoundMetadataCount)

        assertEquals(expectedKeptMetadataCount, fakeRustBackend.metadata.size)

        val keptMetadataBelowRewindHeight = fakeRustBackend.metadata
            .filter { it.height <= rewindHeight }

        assertTrue { keptMetadataBelowRewindHeight.size == expectedKeptMetadataCount }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun createTemporaryFileTest() = runTest {
        val blocks = ListOfCompactBlocksFixture.new()
        val block = blocks.first()

        val file = block.createTemporaryFile(FilePathFixture.newBlocksDir())

        assertTrue { file.existsSuspend() }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun finalizeFileTest() = runTest {
        val blocks = ListOfCompactBlocksFixture.new()
        val block = blocks.first()

        val file = block.createTemporaryFile(FilePathFixture.newBlocksDir())

        val finalizedFile = File(file.absolutePath.dropLast(ZcashSdk.TEMPORARY_FILENAME_SUFFIX.length))
        assertFalse { finalizedFile.existsSuspend() }

        file.finalizeFile()
        assertTrue { finalizedFile.existsSuspend() }
    }
}
