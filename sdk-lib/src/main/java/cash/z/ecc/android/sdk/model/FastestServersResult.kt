package cash.z.ecc.android.sdk.model

import co.electriccoin.lightwallet.client.model.LightWalletEndpoint

sealed interface FastestServersResult {

    /**
     * Fastest server latency measurement is ongoing.
     */
    data object Measuring : FastestServersResult

    /**
     * Fastest server validation is ongoing & latency measurement is done.
     */
    data class Validating(val servers: List<LightWalletEndpoint>) : FastestServersResult

    /**
     * Fastest server measurement is completely done.
     */
    data class Done(val servers: List<LightWalletEndpoint>) : FastestServersResult
}
