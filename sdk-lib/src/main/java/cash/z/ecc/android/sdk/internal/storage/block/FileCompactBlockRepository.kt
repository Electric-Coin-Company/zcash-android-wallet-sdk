package cash.z.ecc.android.sdk.internal.storage.block

import cash.z.ecc.android.sdk.ext.toHex
import cash.z.ecc.android.sdk.internal.ext.createNewFileSuspend
import cash.z.ecc.android.sdk.internal.ext.deleteSuspend
import cash.z.ecc.android.sdk.internal.ext.listFilesSuspend
import cash.z.ecc.android.sdk.internal.ext.mkdirsSuspend
import cash.z.ecc.android.sdk.internal.ext.readBytesSuspend
import cash.z.ecc.android.sdk.internal.ext.renameToSuspend
import cash.z.ecc.android.sdk.internal.ext.writeBytesSuspend
import cash.z.ecc.android.sdk.internal.repository.CompactBlockRepository
import cash.z.ecc.android.sdk.internal.twig
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.ZcashNetwork
import cash.z.wallet.sdk.rpc.CompactFormats
import java.io.File

internal class FileCompactBlockRepository(
    private val network: ZcashNetwork,
    private val cacheDirectory: File,
) : CompactBlockRepository {

    override suspend fun getLatestHeight(): BlockHeight? {
        val files = cacheDirectory.listFilesSuspend().let {
            if (it.isNullOrEmpty()) return null
            else it
        }

        return files
            .filterNot { it.isDirectory }
            .filter { it.name.endsWith(".block") }
            .maxOf {
                twig(it.absolutePath)
                it.getBlockHeight()
            }
            .let { BlockHeight.new(network, it) }
    }

    override suspend fun findCompactBlock(height: BlockHeight): CompactFormats.CompactBlock? =
        cacheDirectory.listFilesSuspend()
            ?.find { it.getBlockHeight() == height.value }
            ?.let { CompactFormats.CompactBlock.parseFrom(it.readBytesSuspend()) }

    //naming conventions HEIGHT-BLOCKHASHHEX-block
    override suspend fun write(result: Sequence<CompactFormats.CompactBlock>): Int {
        var count = 0

        result.forEach { block ->
            val tempFileName = "${block.createFilename()}.tmp"
            val tempFile = File(cacheDirectory, tempFileName)
            tempFile.createNewFileSuspend()
            tempFile.writeBytesSuspend(block.toByteArray())

            //write block metadata

            val newFile = File(tempFile.absolutePath.replace(".tmp", ".block"))
            tempFile.renameToSuspend(newFile)

            count++
        }

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

    private fun CompactFormats.CompactBlock.createFilename() = "${height}-${hash.toByteArray().toHex()}"

    companion object {

        /**
         * @property cacheDirectory the directory for storing blocks.
         */
        suspend fun new(
            zcashNetwork: ZcashNetwork,
            cacheDirectory: File
        ): FileCompactBlockRepository {
            twig("Network: ${zcashNetwork.networkName} / file directory: ${cacheDirectory.absolutePath}")

            cacheDirectory.mkdirsSuspend()

            return FileCompactBlockRepository(zcashNetwork, cacheDirectory)
        }
    }
}
