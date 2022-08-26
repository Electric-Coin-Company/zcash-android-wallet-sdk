package cash.z.ecc.fixture

import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.internal.SaplingFileParameters
import cash.z.ecc.android.sdk.internal.SaplingParamTool
import cash.z.ecc.android.sdk.test.getAppContext
import java.io.File

object SaplingParamsFixture {

    val DESTINATION_DIRECTORY: String = File(getAppContext().cacheDir, "params").absolutePath

    const val SPEND_FILE_NAME = ZcashSdk.SPEND_PARAM_FILE_NAME
    const val SPEND_FILE_MAX_SIZE = SaplingParamTool.SPEND_PARAM_FILE_MAX_BYTES_SIZE

    const val OUTPUT_FILE_NAME = ZcashSdk.OUTPUT_PARAM_FILE_NAME
    const val OUTPUT_FILE_MAX_SIZE = SaplingParamTool.OUTPUT_PARAM_FILE_MAX_BYTES_SIZE

    internal fun newFile(
        destinationDirectoryPath: String = DESTINATION_DIRECTORY,
        fileName: String = SPEND_FILE_NAME,
        fileMaxSize: Long = SPEND_FILE_MAX_SIZE
    ) = SaplingFileParameters(
        destinationDirectoryPath = destinationDirectoryPath,
        fileName = fileName,
        fileMaxSizeBytes = fileMaxSize
    )
}
