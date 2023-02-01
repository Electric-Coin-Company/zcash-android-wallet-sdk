package co.electriccoin.lightwallet.client.model

import cash.z.wallet.sdk.internal.rpc.Service

/**
 * A lightwalletd endpoint information, which has come from the Light Wallet server.
 *
 * It is marked as "unsafe" because it is not guaranteed to be valid.
 */
data class LightWalletEndpointInfoUnsafe(
    val chainName: String,
    val consensusBranchId: String,
    val blockHeightUnsafe: BlockHeightUnsafe
) {
    companion object {
        internal fun new(lightdInfo: Service.LightdInfo) =
            LightWalletEndpointInfoUnsafe(
                lightdInfo.chainName,
                lightdInfo.consensusBranchId,
                BlockHeightUnsafe(lightdInfo.blockHeight)
            )
    }
}
