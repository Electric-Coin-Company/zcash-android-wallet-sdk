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
    val MINERS_FEE = Zatoshi(10_000L)

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
     * A short amount of time, in milliseconds, to poll for new blocks used typically when a block synchronization error
     * occurs.
     */
    const val POLL_INTERVAL_SHORT = 5_000L

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
     * The default maximum amount of time to wait during retry backoff intervals. Failed loops will never wait longer
     * than this before retyring.
     */
    const val MAX_BACKOFF_INTERVAL = 600_000L

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
