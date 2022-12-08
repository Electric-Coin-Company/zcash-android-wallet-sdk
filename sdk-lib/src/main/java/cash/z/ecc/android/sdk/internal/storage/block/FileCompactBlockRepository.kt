package cash.z.ecc.android.sdk.internal.storage.block

import cash.z.ecc.android.sdk.ext.toHex
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
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.ZcashNetwork
import cash.z.wallet.sdk.rpc.CompactFormats.CompactBlock
import java.io.File

internal class FileCompactBlockRepository(
    private val network: ZcashNetwork,
    private val cacheDirectory: File,
    private val rustBackend: RustBackend
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

    override suspend fun findCompactBlock(height: BlockHeight): CompactBlock? =
        cacheDirectory.listFilesSuspend()
            ?.find { it.getBlockHeight() == height.value }
            ?.let { CompactBlock.parseFrom(it.readBytesSuspend()) }

    //naming conventions HEIGHT-BLOCKHASHHEX-block
    override suspend fun write(result: Sequence<CompactBlock>): Int {
        var count = 0
        val blocksDirectory = File(cacheDirectory, "blocks")
        val metaDataBuffer = mutableListOf<JniBlockMeta>()

        result.forEach { block ->
            //create temporary file
            val tempFileName = "${block.createFilename()}.tmp"
            val tempFile = File(blocksDirectory, tempFileName)
            tempFile.createNewFileSuspend()
            //write compact block bytes
            tempFile.writeBytesSuspend(block.toByteArray())
            //buffer metadata
            metaDataBuffer.add(block.toJniMetaData())
            //rename the file
            val newFile = File(tempFile.absolutePath.dropLast(4))
            tempFile.renameToSuspend(newFile)
            count++
        }

        //write blocks metadata
        rustBackend.writeBlockMetadata(metaDataBuffer.toTypedArray())

        return count
    }

    override suspend fun rewindTo(height: BlockHeight) {
        cacheDirectory.listFilesSuspend()
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

            return FileCompactBlockRepository(zcashNetwork, cacheDirectory, rustBackend)
        }
    }
}
