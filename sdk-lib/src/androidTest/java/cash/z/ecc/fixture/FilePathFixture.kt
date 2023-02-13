package cash.z.ecc.fixture

import java.io.File

object FilePathFixture {
    private const val BLOCKS_DIR_NAME = "blocks"

    internal fun newRootDir(path: String = DatabasePathFixture.new()) = File(path)

    internal fun newBlocksDir(
        rootDir: File = newRootDir(),
        directoryName: String = BLOCKS_DIR_NAME
    ) = File(rootDir, directoryName)
}
