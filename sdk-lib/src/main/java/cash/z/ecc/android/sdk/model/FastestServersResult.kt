package cash.z.ecc.android.sdk.model

import co.electriccoin.lightwallet.client.model.LightWalletEndpoint

/**
 * @property servers an ascended sorted list of fastest servers, or null if none loaded
 * @property isLoading indicates whether newer data is being loaded
 */
data class FastestServersResult(val servers: List<LightWalletEndpoint>?, val isLoading: Boolean)
