package cash.z.ecc.android.sdk.internal.storage.block

import androidx.annotation.VisibleForTesting
import cash.z.ecc.android.sdk.internal.Twig
import cash.z.ecc.android.sdk.internal.ext.createNewFileSuspend
import cash.z.ecc.android.sdk.internal.ext.deleteRecursivelySuspend
import cash.z.ecc.android.sdk.internal.ext.deleteSuspend
import cash.z.ecc.android.sdk.internal.ext.existsSuspend
import cash.z.ecc.android.sdk.internal.ext.listFilesSuspend
import cash.z.ecc.android.sdk.internal.ext.mkdirsSuspend
import cash.z.ecc.android.sdk.internal.ext.renameToSuspend
import cash.z.ecc.android.sdk.internal.ext.toHexReversed
import cash.z.ecc.android.sdk.internal.ext.writeBytesSuspend
import cash.z.ecc.android.sdk.internal.model.JniBlockMeta
import cash.z.ecc.android.sdk.internal.repository.CompactBlockRepository
import cash.z.ecc.android.sdk.jni.RustBackend
import cash.z.ecc.android.sdk.jni.RustBackendWelding
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.wallet.sdk.internal.rpc.CompactFormats.CompactBlock
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

        result.forEach { block ->
            val tmpFile = block.createTemporaryFile(blocksDirectory)
            // write compact block bytes
            tmpFile.writeBytesSuspend(block.toByteArray())
            // buffer metadata
            metaDataBuffer.add(block.toJniMetaData())

            val isFinalizeSuccessful = tmpFile.finalizeFile()
            check(isFinalizeSuccessful) {
                "Failed to finalize file: ${tmpFile.absolutePath}"
            }

            count++

            if (metaDataBuffer.isBufferFull()) {
                // write blocks metadata to storage when the buffer is full
                rustBackend.writeBlockMetadata(metaDataBuffer)
                metaDataBuffer.clear()
            }
        }

        if (metaDataBuffer.isNotEmpty()) {
            // write the rest of the blocks metadata to storage even though the buffer is not full
            rustBackend.writeBlockMetadata(metaDataBuffer)
            metaDataBuffer.clear()
        }

        return count
    }

    override suspend fun rewindTo(height: BlockHeight) = rustBackend.rewindBlockMetadataToHeight(height)

    override suspend fun deleteCompactBlocksMetadataFiles(): Boolean {
        // Fixme: remove childrenCount before finishing
        val childrenCount = blocksDirectory.listFilesSuspend()?.count()
        Twig.debug {
            "Removing blocks metadata directory ${blocksDirectory.path} with all its " +
                "$childrenCount children."
        }

        if (blocksDirectory.existsSuspend()) {
            return blocksDirectory.deleteRecursivelySuspend()
        }
        return true
    }

    companion object {
        /**
         * The name of the directory for downloading blocks
         */
        const val BLOCKS_DOWNLOAD_DIRECTORY = "blocks"

        /**
         * The suffix for temporary files
         */
        const val TEMPORARY_FILENAME_SUFFIX = ".tmp"

        /**
         * The suffix for block file name
         */
        const val BLOCK_FILENAME_SUFFIX = "-compactblock"

        /**
         * The size of block meta data buffer
         */
        const val BLOCKS_METADATA_BUFFER_SIZE = 10

        suspend fun new(
            rustBackend: RustBackend
        ): FileCompactBlockRepository {
            Twig.debug { "${rustBackend.fsBlockDbRoot.absolutePath} \n  ${rustBackend.dataDbFile.absolutePath}" }

            // create and check cache directories
            val blocksDirectory = File(rustBackend.fsBlockDbRoot, BLOCKS_DOWNLOAD_DIRECTORY).also {
                it.mkdirsSuspend()
            }
            if (!blocksDirectory.existsSuspend()) {
                error("${blocksDirectory.path} directory does not exist and could not be created.")
            }

            rustBackend.initBlockMetaDb()

            return FileCompactBlockRepository(blocksDirectory, rustBackend)
        }
    }
}

//
// Private helper functions
//

private fun List<JniBlockMeta>.isBufferFull(): Boolean {
    return size % FileCompactBlockRepository.BLOCKS_METADATA_BUFFER_SIZE == 0
}

internal data class CompactBlockOutputsCounts(
    val saplingOutputsCount: UInt,
    val orchardActionsCount: UInt
)

private fun CompactBlock.getOutputsCounts(): CompactBlockOutputsCounts {
    var outputsCount: UInt = 0u
    var actionsCount: UInt = 0u

    vtxList.forEach { compactTx ->
        outputsCount += compactTx.outputsCount.toUInt()
        actionsCount += compactTx.actionsCount.toUInt()
    }

    return CompactBlockOutputsCounts(outputsCount, actionsCount)
}

private fun CompactBlock.toJniMetaData(): JniBlockMeta {
    val outputs = getOutputsCounts()

    return JniBlockMeta.new(this, outputs)
}

private fun CompactBlock.createFilename(): String {
    val hashHex = hash.toByteArray().toHexReversed()
    return "$height-$hashHex${FileCompactBlockRepository.BLOCK_FILENAME_SUFFIX}"
}

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal suspend fun CompactBlock.createTemporaryFile(blocksDirectory: File): File {
    val tempFileName = "${createFilename()}${FileCompactBlockRepository.TEMPORARY_FILENAME_SUFFIX}"
    val tmpFile = File(blocksDirectory, tempFileName)

    if (tmpFile.existsSuspend()) {
        tmpFile.deleteSuspend()
    }
    tmpFile.createNewFileSuspend()

    return tmpFile
}

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal suspend fun File.finalizeFile(): Boolean {
    // rename the file
    val newFile = File(absolutePath.dropLast(FileCompactBlockRepository.TEMPORARY_FILENAME_SUFFIX.length))
    return renameToSuspend(newFile)
}
