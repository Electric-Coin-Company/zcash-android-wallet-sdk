package cash.z.wallet.sdk.ext

/**
 * Wrapper for all the constant values in the SDK. It is important that these values stay fixed for
 * all users of the SDK. Otherwise, if individual wallet makers are using different values, it
 * becomes easier to reduce privacy by segmenting the anonymity set of users, particularly as it
 * relates to network requests.
 */
object ZcashSdk : ZcashSdkCommon() {

    /**
     * The height of the first sapling block. When it comes to shielded transactions, we do not need to consider any blocks
     * prior to this height, at all.
     */
    override val SAPLING_ACTIVATION_HEIGHT = 280_000

    /**
     * The default port to use for connecting to lightwalletd instances.
     */
    override val LIGHTWALLETD_PORT = 9067

    /**
     * The default host to use for lightwalletd.
     */
    override val DEFAULT_LIGHTWALLETD_HOST = "lightd-test.zecwallet.co"

    override val DEFAULT_DB_NAME_PREFIX = "ZcashSdk_testnet_"

}
