package cash.z.ecc.android.sdk.internal.storage.block

import androidx.annotation.VisibleForTesting
import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.internal.ext.createNewFileSuspend
import cash.z.ecc.android.sdk.internal.ext.deleteSuspend
import cash.z.ecc.android.sdk.internal.ext.existsSuspend
import cash.z.ecc.android.sdk.internal.ext.mkdirsSuspend
import cash.z.ecc.android.sdk.internal.ext.renameToSuspend
import cash.z.ecc.android.sdk.internal.ext.toHexReversed
import cash.z.ecc.android.sdk.internal.ext.writeBytesSuspend
import cash.z.ecc.android.sdk.internal.model.JniBlockMeta
import cash.z.ecc.android.sdk.internal.repository.CompactBlockRepository
import cash.z.ecc.android.sdk.internal.twig
import cash.z.ecc.android.sdk.jni.RustBackend
import cash.z.ecc.android.sdk.jni.RustBackendWelding
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.wallet.sdk.rpc.CompactFormats.CompactBlock
import java.io.File

internal class FileCompactBlockRepository(
    private val blocksDirectory: File,
    private val rustBackend: RustBackendWelding
) : CompactBlockRepository {

    override suspend fun getLatestHeight() = rustBackend.getLatestHeight()

    override suspend fun findCompactBlock(height: BlockHeight) = rustBackend.findBlockMetadata(height)

    override suspend fun write(result: Sequence<CompactBlock>): Int {
        var count = 0

        val metaDataBuffer = mutableListOf<JniBlockMeta>()

        result.forEachIndexed { index, block ->
            val tmpFile = block.createTemporaryFile(blocksDirectory)
            // write compact block bytes
            tmpFile.writeBytesSuspend(block.toByteArray())
            // buffer metadata
            metaDataBuffer.add(block.toJniMetaData())
            check(tmpFile.finalizeFile()) {
                "Failed to finalize file: ${tmpFile.absolutePath}"
            }
            count++

            if (index % ZcashSdk.BLOCKS_METADATA_BUFFER_SIZE == 0) {
                // write blocks metadata
                rustBackend.writeBlockMetadata(metaDataBuffer.toTypedArray())
                metaDataBuffer.clear()
            }
        }

        if (metaDataBuffer.isNotEmpty()) {
            rustBackend.writeBlockMetadata(metaDataBuffer.toTypedArray())
        }

        return count
    }

    override suspend fun rewindTo(height: BlockHeight) = rustBackend.rewindBlockMetadataToHeight(height)

    companion object {

        suspend fun new(
            rustBackend: RustBackend
        ): FileCompactBlockRepository {
            twig("${rustBackend.fsBlockDbRoot.absolutePath} \n  ${rustBackend.dataDbFile.absolutePath}")

            // create cache directories
            val blocksDirectory = File(rustBackend.fsBlockDbRoot, ZcashSdk.BLOCKS_DOWNLOAD_DIRECTORY)
            if (!blocksDirectory.mkdirsSuspend()) {
                error("${blocksDirectory.path} directory does not exist and could not be created.")
            }

            rustBackend.initBlockMetaDb()

            return FileCompactBlockRepository(blocksDirectory, rustBackend)
        }
    }
}

private data class CompactBlockOutputsCounts(
    val saplingOutputsCount: Long,
    val orchardActionsCount: Long
)

private fun CompactBlock.getOutputsCounts(): CompactBlockOutputsCounts {
    var outputsCount = 0L
    var actionsCount = 0L

    vtxList.forEach { compactTx ->
        outputsCount += compactTx.outputsCount
        actionsCount += compactTx.actionsCount
    }

    return CompactBlockOutputsCounts(outputsCount, actionsCount)
}

private fun CompactBlock.toJniMetaData(): JniBlockMeta {
    val outputs = getOutputsCounts()

    return JniBlockMeta(
        height = height,
        hash = hash.toByteArray(),
        time = time.toLong(),
        saplingOutputsCount = outputs.saplingOutputsCount,
        orchardActionsCount = outputs.orchardActionsCount
    )
}

@VisibleForTesting
private fun CompactBlock.createFilename(): String {
    val hashHex = hash.toByteArray().toHexReversed()
    return "$height-$hashHex-$ZcashSdk.BLOCK_FILENAME_SUFFIX"
}

@VisibleForTesting
internal suspend fun CompactBlock.createTemporaryFile(blocksDirectory: File): File {
    val tempFileName = "${createFilename()}${ZcashSdk.TEMPORARY_FILENAME_SUFFIX}"
    val tmpFile = File(blocksDirectory, tempFileName)

    if (tmpFile.existsSuspend()) {
        tmpFile.deleteSuspend()
    }
    tmpFile.createNewFileSuspend()

    return tmpFile
}

@VisibleForTesting
internal suspend fun File.finalizeFile(): Boolean {
    // rename the file
    val newFile = File(absolutePath.dropLast(ZcashSdk.TEMPORARY_FILENAME_SUFFIX.length))
    return renameToSuspend(newFile)
}
