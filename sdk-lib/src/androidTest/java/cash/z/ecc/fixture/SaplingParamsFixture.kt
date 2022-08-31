package cash.z.ecc.fixture

import cash.z.ecc.android.sdk.internal.Files
import cash.z.ecc.android.sdk.internal.SaplingParamTool
import cash.z.ecc.android.sdk.internal.SaplingParameters
import cash.z.ecc.android.sdk.internal.ext.deleteSuspend
import cash.z.ecc.android.sdk.test.getAppContext
import kotlinx.coroutines.runBlocking
import java.io.File

object SaplingParamsFixture {

    internal val DESTINATION_DIRECTORY_LEGACY: File = File(
        getAppContext().cacheDir,
        SaplingParamTool.SAPLING_PARAMS_LEGACY_SUBDIRECTORY
    )

    internal val DESTINATION_DIRECTORY: File
        get() = runBlocking {
            Files.getZcashNoBackupSubdirectory(getAppContext())
        }

    internal const val SPEND_FILE_NAME = SaplingParamTool.SPEND_PARAM_FILE_NAME
    internal const val SPEND_FILE_MAX_SIZE = SaplingParamTool.SPEND_PARAM_FILE_MAX_BYTES_SIZE
    internal const val SPEND_FILE_HASH = SaplingParamTool.SPEND_PARAM_FILE_SHA1_HASH

    internal const val OUTPUT_FILE_NAME = SaplingParamTool.OUTPUT_PARAM_FILE_NAME
    internal const val OUTPUT_FILE_MAX_SIZE = SaplingParamTool.OUTPUT_PARAM_FILE_MAX_BYTES_SIZE
    internal const val OUTPUT_FILE_HASH = SaplingParamTool.OUTPUT_PARAM_FILE_SHA1_HASH

    internal fun new(
        destinationDirectoryPath: File = DESTINATION_DIRECTORY,
        fileName: String = SPEND_FILE_NAME,
        fileMaxSize: Long = SPEND_FILE_MAX_SIZE,
        fileHash: String = SPEND_FILE_HASH
    ) = SaplingParameters(
        destinationDirectory = destinationDirectoryPath,
        fileName = fileName,
        fileMaxSizeBytes = fileMaxSize,
        fileHash = fileHash
    )

    internal fun writeFile(paramsFile: File) {
        paramsFile.createNewFile()
    }

    internal suspend fun clearAllFilesFromDirectory(destinationDir: File) {
        if (!destinationDir.exists()) {
            return
        }
        for (file in destinationDir.listFiles()!!) {
            file.deleteSuspend()
        }
    }
}
