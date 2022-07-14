package cash.z.ecc.android.sdk.ext

import cash.z.ecc.android.sdk.model.Zatoshi

/**
 * Wrapper for all the constant values in the SDK. It is important that these values stay fixed for
 * all users of the SDK. Otherwise, if individual wallet makers are using different values, it
 * becomes easier to reduce privacy by segmenting the anonymity set of users, particularly as it
 * relates to network requests.
 */
object ZcashSdk {

    /**
     * Miner's fee in zatoshi.
     */
    val MINERS_FEE = Zatoshi(1_000L)

    /**
     * The theoretical maximum number of blocks in a reorg, due to other bottlenecks in the protocol design.
     */
    val MAX_REORG_SIZE = 100

    /**
     * The maximum length of a memo.
     */
    val MAX_MEMO_SIZE = 512

    /**
     * The amount of blocks ahead of the current height where new transactions are set to expire. This value is controlled
     * by the rust backend but it is helpful to know what it is set to and should be kept in sync.
     */
    val EXPIRY_OFFSET = 20

    /**
     * Default size of batches of blocks to request from the compact block service.
     */
    val DOWNLOAD_BATCH_SIZE = 100

    /**
     * Default size of batches of blocks to scan via librustzcash. The smaller this number the more granular information
     * can be provided about scan state. Unfortunately, it may also lead to a lot of overhead during scanning.
     */
    val SCAN_BATCH_SIZE = 150

    /**
     * Default amount of time, in milliseconds, to poll for new blocks. Typically, this should be about half the average
     * block time.
     */
    val POLL_INTERVAL = 20_000L

    /**
     * Estimate of the time between blocks.
     */
    val BLOCK_INTERVAL_MILLIS = 75_000L

    /**
     * Default attempts at retrying.
     */
    val RETRIES = 5

    /**
     * The default maximum amount of time to wait during retry backoff intervals. Failed loops will never wait longer than
     * this before retyring.
     */
    val MAX_BACKOFF_INTERVAL = 600_000L

    /**
     * Default number of blocks to rewind when a chain reorg is detected. This should be large enough to recover from the
     * reorg but smaller than the theoretical max reorg size of 100.
     */
    val REWIND_DISTANCE = 10

    const val DB_DATA_NAME = "Data.db" // $NON-NLS
    const val DB_CACHE_NAME = "Cache.db" // $NON-NLS
    const val DB_PENDING_TRANSACTIONS_NAME = "PendingTransactions.db" // $NON-NLS

    const val DATABASE_FILE_JOURNAL_SUFFIX = "-journal" // $NON-NLS
    const val DATABASE_FILE_WAL_SUFFIX = "-wal" // $NON-NLS

    /**
     * File name for the sappling spend params
     */
    val SPEND_PARAM_FILE_NAME = "sapling-spend.params"

    /**
     * File name for the sapling output params
     */
    val OUTPUT_PARAM_FILE_NAME = "sapling-output.params"

    /**
     * The Url that is used by default in zcashd.
     * We'll want to make this externally configurable, rather than baking it into the SDK but
     * this will do for now, since we're using a cloudfront URL that already redirects.
     */
    val CLOUD_PARAM_DIR_URL = "https://z.cash/downloads/"

    /**
     * The default memo to use when shielding transparent funds.
     */
    val DEFAULT_SHIELD_FUNDS_MEMO_PREFIX = "shielding:"

    val DEFAULT_ALIAS: String = "ZcashSdk"
}
