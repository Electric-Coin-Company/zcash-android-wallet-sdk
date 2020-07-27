package cash.z.ecc.android.sdk.ext

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
    override val SAPLING_ACTIVATION_HEIGHT = 419_200

    /**
     * The default port to use for connecting to lightwalletd instances.
     */
    override val DEFAULT_LIGHTWALLETD_PORT = 443

    /**
     * The default host to use for lightwalletd.
     */
    override val DEFAULT_LIGHTWALLETD_HOST = "lightd-main.zecwallet.co"

    /**
     * The default alias to use for naming database and preference files.
     */
    override val DEFAULT_ALIAS = "ZcashSdk_mainnet"

    /**
     * The name of the network that this SDK build targets.
     */
    override val NETWORK = "mainnet"
}
