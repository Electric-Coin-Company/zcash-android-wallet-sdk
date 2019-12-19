package cash.z.wallet.sdk.ext

/**
 * Wrapper for all the constant values in the SDK. It is important that these values stay fixed for
 * all users of the SDK. Otherwise, if individual wallet makers are using different values, it
 * becomes easier to reduce privacy by segmenting the anonymity set of users, particularly as it
 * relates to network requests.
 */
open class ZcashSdkCommon {

    /**
     * Miner's fee in zatoshi.
     */
    val MINERS_FEE_ZATOSHI = 10_000L

    /**
     * The number of zatoshi that equal 1 ZEC.
     */
    val ZATOSHI_PER_ZEC = 100_000_000L

    /**
     * The height of the first sapling block. When it comes to shielded transactions, we do not need to consider any blocks
     * prior to this height, at all.
     */
    open val SAPLING_ACTIVATION_HEIGHT = 280_000

    /**
     * The theoretical maximum number of blocks in a reorg, due to other bottlenecks in the protocol design.
     */
    val MAX_REORG_SIZE = 100

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
     * Default amount of time, in milliseconds, to poll for new blocks. Typically, this should be about half the average
     * block time.
     */
    val POLL_INTERVAL = 75_000L

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

    /**
     * The default port to use for connecting to lightwalletd instances.
     */
    open val DEFAULT_LIGHTWALLETD_PORT = 9067

    /**
     * The default host to use for lightwalletd.
     */
    open val DEFAULT_LIGHTWALLETD_HOST = "listwallted.z.cash"

    val DB_DATA_NAME = "Data.db"
    val DB_CACHE_NAME = "Cache.db"
    open val DEFAULT_DB_NAME_PREFIX = "ZcashSdk_"

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

}
