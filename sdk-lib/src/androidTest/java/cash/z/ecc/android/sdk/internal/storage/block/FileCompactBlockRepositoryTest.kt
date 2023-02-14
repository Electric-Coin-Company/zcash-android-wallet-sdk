package cash.z.ecc.android.sdk.internal.storage.block

import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.internal.ext.deleteRecursivelySuspend
import cash.z.ecc.android.sdk.internal.ext.existsSuspend
import cash.z.ecc.android.sdk.internal.ext.mkdirsSuspend
import cash.z.ecc.android.sdk.jni.RustBackendWelding
import cash.z.ecc.android.sdk.model.BlockHeight
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

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setup() = runTest {
        val rootDirectory = FilePathFixture.newRootDir()
        if (rootDirectory.existsSuspend()) {
            rootDirectory.deleteRecursivelySuspend()
        }

        val blocksDir = FilePathFixture.newBlocksDir()
        blocksDir.mkdirsSuspend()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() = runTest {
        FilePathFixture.newRootDir().deleteRecursivelySuspend()
    }

    private fun getMockedFileCompactBlockRepository(rustBackend: RustBackendWelding): FileCompactBlockRepository =
        runBlocking {
            FileCompactBlockRepository(
                FilePathFixture.newRootDir(),
                rustBackend
            )
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun getLatestHeightTest() = runTest {
        val rustBackend = FakeRustBackendFixture().new()
        val mockedBlockRepository = getMockedFileCompactBlockRepository(rustBackend)

        val blocks = ListOfCompactBlocksFixture.new()

        mockedBlockRepository.write(blocks)

        assertTrue { blocks.last().height == mockedBlockRepository.getLatestHeight()?.value }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun writeBlockTest() = runTest {
        val rustBackend = FakeRustBackendFixture().new()
        val mockedBlockRepository = getMockedFileCompactBlockRepository(rustBackend)

        assertTrue { rustBackend.metadata.isEmpty() }

        val blocks = ListOfCompactBlocksFixture.new()
        mockedBlockRepository.write(blocks)

        assertTrue { rustBackend.metadata.size == blocks.count() }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun rewindToTest() = runTest {
        val rustBackend = FakeRustBackendFixture().new()
        val mockedBlockRepository = getMockedFileCompactBlockRepository(rustBackend)

        val testedBlocksRange = ListOfCompactBlocksFixture.DEFAULT_FILE_BLOCK_RANGE

        val blocks = ListOfCompactBlocksFixture.new(testedBlocksRange)
        mockedBlockRepository.write(blocks)

        val blocksRangeMiddleValue = testedBlocksRange.run {
            start.value.plus(endInclusive.value).div(2)
        }
        val rewindHeight: Long = blocksRangeMiddleValue
        mockedBlockRepository.rewindTo(BlockHeight(rewindHeight))

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

        assertTrue { keptMetadataBelowRewindHeight.size == expectedKeptMetadataCount }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun createTemporaryFileTest() = runTest {
        val rootDir = FilePathFixture.newRootDir()
        val blocks = ListOfCompactBlocksFixture.new()
        val block = blocks.first()

        val file = block.createTemporaryFile(rootDir)

        assertTrue { file.existsSuspend() }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun finalizeFileTest() = runTest {
        val rootDir = FilePathFixture.newRootDir()
        val blocks = ListOfCompactBlocksFixture.new()
        val block = blocks.first()

        val tempFile = block.createTemporaryFile(rootDir)

        val finalizedFile = File(tempFile.absolutePath.dropLast(ZcashSdk.TEMPORARY_FILENAME_SUFFIX.length))
        assertFalse { finalizedFile.existsSuspend() }

        tempFile.finalizeFile()
        assertTrue { finalizedFile.existsSuspend() }

        assertFalse { tempFile.existsSuspend() }
    }
}
