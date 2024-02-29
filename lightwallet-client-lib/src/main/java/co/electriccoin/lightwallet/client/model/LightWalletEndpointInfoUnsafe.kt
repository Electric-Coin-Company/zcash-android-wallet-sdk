package co.electriccoin.lightwallet.client.model

import cash.z.wallet.sdk.internal.rpc.Service
import java.util.Locale

/**
 * A lightwalletd endpoint information, which has come from the Light Wallet server.
 *
 * It is marked as "unsafe" because it is not guaranteed to be valid.
 */
data class LightWalletEndpointInfoUnsafe(
    val chainName: String,
    val consensusBranchId: String,
    val blockHeightUnsafe: BlockHeightUnsafe,
    val saplingActivationHeightUnsafe: BlockHeightUnsafe
) {
    companion object {
        internal fun new(lightdInfo: Service.LightdInfo) =
            LightWalletEndpointInfoUnsafe(
                lightdInfo.chainName,
                lightdInfo.consensusBranchId,
                BlockHeightUnsafe(lightdInfo.blockHeight),
                BlockHeightUnsafe(lightdInfo.saplingActivationHeight),
            )
    }

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
