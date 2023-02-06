package cash.z.ecc.android.sdk.internal.repository

import cash.z.ecc.android.sdk.internal.model.JniBlockMeta
import cash.z.ecc.android.sdk.model.BlockHeight
import co.electriccoin.lightwallet.client.model.CompactBlockUnsafe

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
     * block files from the device disk together with theirs parent directory.
     *
     * @return true when all block files are deleted, false only if the deletion fails
     */
    suspend fun deleteCompactBlockFiles(): Boolean

    /**
     * Write the given blocks to this store, which may be anything from an in-memory cache to a DB.
     *
     * @param result the list of compact blocks to persist.
     * @return Number of blocks that were written.
     */
    suspend fun write(result: Sequence<CompactBlockUnsafe>): Int

    /**
     * Remove every block above the given height.
     *
     * @param height the target height to which to rewind.
     */
    suspend fun rewindTo(height: BlockHeight)
}
