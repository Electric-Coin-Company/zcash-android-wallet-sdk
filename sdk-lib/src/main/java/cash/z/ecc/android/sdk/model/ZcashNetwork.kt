package cash.z.ecc.android.sdk.model

/**
 * The Zcash network.  Should be one of [ZcashNetwork.Testnet] or [ZcashNetwork.Mainnet].
 *
 * The constructor for the network is public to allow for certain test cases to use a custom "darkside" network.
 */
data class ZcashNetwork(
    val id: Int,
    val networkName: String,
    val saplingActivationHeight: BlockHeight,
    val orchardActivationHeight: BlockHeight
) {
    fun isMainnet() = id == ID_MAINNET

    fun isTestnet() = id == ID_TESTNET

    @Suppress("MagicNumber")
    companion object {
        const val ID_TESTNET = 0
        const val ID_MAINNET = 1

        // You may notice there are extra checkpoints bundled in the SDK that match the
        // sapling/orchard activation heights.

        val Testnet =
            ZcashNetwork(
                ID_TESTNET,
                "testnet",
                saplingActivationHeight = BlockHeight(280_000),
                orchardActivationHeight = BlockHeight(1_842_420)
            )

        val Mainnet =
            ZcashNetwork(
                ID_MAINNET,
                "mainnet",
                saplingActivationHeight = BlockHeight(419_200),
                orchardActivationHeight = BlockHeight(1_687_104)
            )

        fun from(id: Int) =
            when (id) {
                0 -> Testnet
                1 -> Mainnet
                else -> throw IllegalArgumentException("Unknown network id: $id")
            }
    }
}
