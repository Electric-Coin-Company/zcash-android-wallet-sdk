package cash.z.ecc.android.sdk.internal.block

import cash.z.wallet.sdk.rpc.CompactFormats

/**
 * Interface for storing compact blocks.
 */
interface CompactBlockStore {
    /**
     * Gets the highest block that is currently stored.
     *
     * @return the latest block height.
     */
    suspend fun getLatestHeight(): Int

    /**
     * Fetch the compact block for the given height, if it exists.
     *
     * @return the compact block or null when it did not exist.
     */
    suspend fun findCompactBlock(height: Int): CompactFormats.CompactBlock?

    /**
     * Write the given blocks to this store, which may be anything from an in-memory cache to a DB.
     *
     * @param result the list of compact blocks to persist.
     */
    suspend fun write(result: List<CompactFormats.CompactBlock>)

    /**
     * Remove every block above the given height.
     *
     * @param height the target height to which to rewind.
     */
    suspend fun rewindTo(height: Int)

    /**
     * Close any connections to the block store.
     */
    fun close()
}
