package cash.z.ecc.android.sdk.model

import co.electriccoin.lightwallet.client.model.LightWalletEndpoint

data class FastestServersResult(val servers: List<LightWalletEndpoint>?, val isLoading: Boolean)
