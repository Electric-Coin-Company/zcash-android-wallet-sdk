package cash.z.ecc.fixture

import cash.z.ecc.android.sdk.ext.ZcashSdk
import java.io.File

object FilePathFixture {
    private const val DEFAULT_BLOCKS_DIR_NAME = ZcashSdk.BLOCKS_DOWNLOAD_DIRECTORY

    internal fun newRootDir(path: String = DatabasePathFixture.new()) = File(path)

    internal fun newBlocksDir(
        rootDir: File = newRootDir(),
        directoryName: String = DEFAULT_BLOCKS_DIR_NAME
    ) = File(rootDir, directoryName)
}
