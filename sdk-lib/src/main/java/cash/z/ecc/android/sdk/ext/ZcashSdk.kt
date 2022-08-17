package cash.z.ecc.android.sdk.ext

import cash.z.ecc.android.sdk.model.Zatoshi

/**
 * Wrapper for all the constant values in the SDK. It is important that these values stay fixed for
 * all users of the SDK. Otherwise, if individual wallet makers are using different values, it
 * becomes easier to reduce privacy by segmenting the anonymity set of users, particularly as it
 * relates to network requests.
 */
@Suppress("MagicNumber")
object ZcashSdk {

    /**
     * Miner's fee in zatoshi.
     */
    val MINERS_FEE = Zatoshi(1_000L)

    /**
     * The theoretical maximum number of blocks in a reorg, due to other bottlenecks in the protocol design.
     */
    const val MAX_REORG_SIZE = 100

    /**
     * The maximum length of a memo.
     */
    const val MAX_MEMO_SIZE = 512

    /**
     * The amount of blocks ahead of the current height where new transactions are set to expire. This value is
     * controlled by the rust backend but it is helpful to know what it is set to and should be kept in sync.
     */
    const val EXPIRY_OFFSET = 20

    /**
     * Default size of batches of blocks to request from the compact block service.
     */
    // Because blocks are buffered in memory upon download and storage into SQLite, there is an upper bound
    // above which OutOfMemoryError is thrown. Experimentally, this value is below 50 blocks.
    // Back of the envelope calculation says the maximum block size is ~100kb.
    const val DOWNLOAD_BATCH_SIZE = 10

    /**
     * Default size of batches of blocks to scan via librustzcash. The smaller this number the more granular information
     * can be provided about scan state. Unfortunately, it may also lead to a lot of overhead during scanning.
     */
    const val SCAN_BATCH_SIZE = 150

    /**
     * Default amount of time, in milliseconds, to poll for new blocks. Typically, this should be about half the average
     * block time.
     */
    const val POLL_INTERVAL = 20_000L

    /**
     * Estimate of the time between blocks.
     */
    const val BLOCK_INTERVAL_MILLIS = 75_000L

    /**
     * Default attempts at retrying.
     */
    const val RETRIES = 5

    /**
     * The default maximum amount of time to wait during retry backoff intervals. Failed loops will never wait longer
     * than this before retyring.
     */
    const val MAX_BACKOFF_INTERVAL = 600_000L

    /**
     * Default number of blocks to rewind when a chain reorg is detected. This should be large enough to recover from
     * the reorg but smaller than the theoretical max reorg size of 100.
     */
    const val REWIND_DISTANCE = 10

    /**
     * File name for the sappling spend params
     */
    const val SPEND_PARAM_FILE_NAME = "sapling-spend.params"

    /**
     * File name for the sapling output params
     */
    const val OUTPUT_PARAM_FILE_NAME = "sapling-output.params"

    /**
     * The Url that is used by default in zcashd.
     * We'll want to make this externally configurable, rather than baking it into the SDK but
     * this will do for now, since we're using a cloudfront URL that already redirects.
     */
    const val CLOUD_PARAM_DIR_URL = "https://z.cash/downloads/"

    /**
     * The default memo to use when shielding transparent funds.
     */
    const val DEFAULT_SHIELD_FUNDS_MEMO_PREFIX = "shielding:"

    /**
     * The default alias used as part of a file name for the preferences and databases. This
     * enables multiple wallets to exist on one device, which is also helpful for sweeping funds.
     */
    const val DEFAULT_ALIAS: String = "zcash_sdk"

    /**
     * The minimum alias length to be valid for our use.
     */
    const val ALIAS_MIN_LENGTH: Int = 1

    /**
     * The maximum alias length to be valid for our use.
     */
    const val ALIAS_MAX_LENGTH: Int = 99
}
