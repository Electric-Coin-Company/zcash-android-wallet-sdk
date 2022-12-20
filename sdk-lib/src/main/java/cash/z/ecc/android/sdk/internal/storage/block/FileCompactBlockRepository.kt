package cash.z.ecc.android.sdk.internal.storage.block

import androidx.annotation.VisibleForTesting
import cash.z.ecc.android.sdk.internal.ext.createNewFileSuspend
import cash.z.ecc.android.sdk.internal.ext.deleteSuspend
import cash.z.ecc.android.sdk.internal.ext.listFilesSuspend
import cash.z.ecc.android.sdk.internal.ext.mkdirsSuspend
import cash.z.ecc.android.sdk.internal.ext.readBytesSuspend
import cash.z.ecc.android.sdk.internal.ext.renameToSuspend
import cash.z.ecc.android.sdk.internal.ext.writeBytesSuspend
import cash.z.ecc.android.sdk.internal.model.JniBlockMeta
import cash.z.ecc.android.sdk.internal.repository.CompactBlockRepository
import cash.z.ecc.android.sdk.internal.twig
import cash.z.ecc.android.sdk.jni.RustBackend
import cash.z.ecc.android.sdk.jni.RustBackendWelding
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.ZcashNetwork
import cash.z.wallet.sdk.rpc.CompactFormats.CompactBlock
import java.io.File
import java.io.IOException
import java.util.Locale

internal class FileCompactBlockRepository(
    private val network: ZcashNetwork,
    private val cacheDirectory: File,
    private val rustBackend: RustBackendWelding
) : CompactBlockRepository {

    override suspend fun getLatestHeight(): BlockHeight? {
        val blocksDirectory = File(cacheDirectory, "blocks")
        val files = blocksDirectory.listFilesSuspend().let {
            if (it.isNullOrEmpty()) return null
            else it
        }
        //it would be "probably" better to query metadata sql
        return files
            .maxOf {
                it.getBlockHeight()
            }
            .let { BlockHeight.new(network, it) }
    }

    override suspend fun findCompactBlock(height: BlockHeight): CompactBlock? = //probably dont need
        File(cacheDirectory, "blocks").listFilesSuspend()
            ?.find { it.getBlockHeight() == height.value }
            ?.let { CompactBlock.parseFrom(it.readBytesSuspend()) }
    
    override suspend fun write(result: Sequence<CompactBlock>): Int {
        var count = 0
        val blocksDirectory = File(cacheDirectory, "blocks")
        val metaDataBuffer = mutableListOf<JniBlockMeta>()

        result.forEach { block ->
            val tmpFile = createTemporaryFile(block, blocksDirectory)
            //write compact block bytes
            tmpFile.writeBytesSuspend(block.toByteArray())
            //buffer metadata
            metaDataBuffer.add(block.toJniMetaData())
            tmpFile.finalizeFile()
            count++
        }

        //write blocks metadata
        rustBackend.writeBlockMetadata(metaDataBuffer.toTypedArray())

        return count
    }

    @VisibleForTesting
    private suspend fun createTemporaryFile(block: CompactBlock, blocksDirectory: File): File {
        val tempFileName = "${block.createFilename()}.tmp"
        val tmpFile = File(blocksDirectory, tempFileName)
        if (tmpFile.createNewFileSuspend()) {
            return tmpFile
        } else {
            // TODO what to do here ???
            throw IOException("File already exists")
        }
    }

    @VisibleForTesting
    private suspend fun File.finalizeFile(){
        //rename the file
        val newFile = File(absolutePath.dropLast(4))
        renameToSuspend(newFile)
    }

    override suspend fun rewindTo(height: BlockHeight) {
        File(cacheDirectory, "blocks").listFilesSuspend()
            ?.filter { it.getBlockHeight() > height.value }
            ?.forEach { it.deleteSuspend() }
    }

    override suspend fun close() {
        //TODO Not yet implemented
    }

    private fun File.getBlockHeight() = name.split("-")[0].toLong()

    private fun CompactBlock.createFilename() = "${height}-${hash.toByteArray().toHex()}-compactblock"

    private fun CompactBlock.toJniMetaData() =
        JniBlockMeta(
            height = height,
            hash = hash.toByteArray(),
            time = time.toLong(),
            saplingOutputsCount = network.saplingActivationHeight.value,
            orchardOutputsCount = network.orchardActivationHeight.value
        )

    private fun ByteArray.toHex(): String {
        val sb = StringBuilder(size * 2)
        for (b in this) {
            sb.append(String.format(Locale.ROOT, "%02x", b).reversed())
        }
        return sb.reverse().toString()
    }

    companion object {

        /**
         * @property cacheDirectory the directory for storing blocks.
         */
        suspend fun new(
            zcashNetwork: ZcashNetwork,
            cacheDirectory: File,
            rustBackend: RustBackend
        ): FileCompactBlockRepository {
            twig("${rustBackend.fsBlockDbRoot.absolutePath} \n  ${rustBackend.dataDbFile.absolutePath}")
            // create cache directories
            File(cacheDirectory, "blocks").mkdirsSuspend()
            rustBackend.initBlockMetaDb()

            return FileCompactBlockRepository(zcashNetwork, cacheDirectory, rustBackend)
        }
    }
}
