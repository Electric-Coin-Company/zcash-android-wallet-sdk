package cash.z.ecc.android.sdk.internal.repository

import cash.z.ecc.android.sdk.internal.model.JniBlockMeta
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.wallet.sdk.internal.rpc.CompactFormats

/**
 * Interface for storing compact blocks.
 */
interface CompactBlockRepository {
    /**
     * Gets the highest block that is currently stored.
     *
     * @return the latest block height.
     */
    suspend fun getLatestHeight(): BlockHeight?

    /**
     * Fetch the compact block for the given height, if it exists.
     *
     * @return the compact block or null when it did not exist.
     */
    suspend fun findCompactBlock(height: BlockHeight): JniBlockMeta?

    /**
     * This function is supposed to be used once the whole blocks sync process done. It removes all the temporary
     * blocks metadata files from the device disk together with theirs parent directory.
     *
     * @return true when its deleted, false if the deletion fails
     */
    suspend fun deleteCompactBlocksMetadataFiles(): Boolean

    /**
     * Write the given blocks to this store, which may be anything from an in-memory cache to a DB.
     *
     * @param result the list of compact blocks to persist.
     * @return Number of blocks that were written.
     */
    suspend fun write(result: Sequence<CompactFormats.CompactBlock>): Int

    /**
     * Remove every block above the given height.
     *
     * @param height the target height to which to rewind.
     */
    suspend fun rewindTo(height: BlockHeight)
}
