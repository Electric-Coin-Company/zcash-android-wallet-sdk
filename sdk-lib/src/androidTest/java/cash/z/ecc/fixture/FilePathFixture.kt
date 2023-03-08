package cash.z.ecc.fixture

import cash.z.ecc.android.sdk.internal.storage.block.FileCompactBlockRepository
import java.io.File

object FilePathFixture {
    private val DEFAULT_ROOT_DIR_PATH = DatabasePathFixture.new()
    private const val DEFAULT_BLOCKS_DIR_NAME = FileCompactBlockRepository.BLOCKS_DOWNLOAD_DIRECTORY

    internal fun newBlocksDir(
        rootDirectoryPath: String = DEFAULT_ROOT_DIR_PATH,
        blockDirectoryName: String = DEFAULT_BLOCKS_DIR_NAME
    ) = File(rootDirectoryPath, blockDirectoryName)
}
