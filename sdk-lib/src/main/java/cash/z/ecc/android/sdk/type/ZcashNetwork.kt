package cash.z.ecc.android.sdk.type

import cash.z.ecc.android.sdk.model.BlockHeight

enum class ZcashNetwork(
    val id: Int,
    val networkName: String,
    val saplingActivationHeight: BlockHeight,
    val defaultHost: String,
    val defaultPort: Int
) {
    Testnet(0, "testnet", BlockHeight(280_000), "testnet.lightwalletd.com", 9067),
    Mainnet(1, "mainnet", BlockHeight(419_200), "mainnet.lightwalletd.com", 9067);

    companion object {
        fun from(id: Int) = values().first { it.id == id }
    }
}
