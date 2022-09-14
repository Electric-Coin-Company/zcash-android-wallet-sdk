package co.electriccoin.lightwallet.client.model

import cash.z.wallet.sdk.rpc.Service

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
