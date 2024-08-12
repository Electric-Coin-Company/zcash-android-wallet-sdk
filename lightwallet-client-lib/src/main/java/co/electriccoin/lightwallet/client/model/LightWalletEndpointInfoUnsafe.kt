package co.electriccoin.lightwallet.client.model

import cash.z.wallet.sdk.internal.rpc.Service.LightdInfo
import java.util.Locale

/**
 * A lightwalletd endpoint information, which has come from the Light Wallet server.
 *
 * It is marked as "unsafe" because it is not guaranteed to be valid.
 */
data class LightWalletEndpointInfoUnsafe(
    val lightdInfo: LightdInfo
) {
    companion object {
        internal fun new(lightdInfo: LightdInfo) = LightWalletEndpointInfoUnsafe(lightdInfo)
    }

    val chainName: String = lightdInfo.chainName
    val consensusBranchId: String = lightdInfo.consensusBranchId
    val blockHeightUnsafe: BlockHeightUnsafe = BlockHeightUnsafe(lightdInfo.blockHeight)
    val saplingActivationHeightUnsafe: BlockHeightUnsafe = BlockHeightUnsafe(lightdInfo.saplingActivationHeight)
    val estimatedHeight: Long = lightdInfo.estimatedHeight

    // [chainName] is either "main" or "test"
    fun matchingNetwork(network: String): Boolean {
        fun String.toId() =
            lowercase(Locale.ROOT).run {
                when {
                    contains("main") -> "mainnet"
                    contains("test") -> "testnet"
                    else -> this
                }
            }
        return chainName.toId() == network.toId()
    }
}
