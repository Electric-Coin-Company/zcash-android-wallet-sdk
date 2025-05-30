package cash.z.ecc.android.sdk.util

import android.content.Context
import cash.z.ecc.android.sdk.internal.model.CombinedWalletClient
import cash.z.ecc.android.sdk.internal.model.TorClient
import co.electriccoin.lightwallet.client.LightWalletClient
import co.electriccoin.lightwallet.client.PartialTorWalletClient
import co.electriccoin.lightwallet.client.WalletClient
import co.electriccoin.lightwallet.client.model.LightWalletEndpoint

/**
 * A factory responsible for creating an instance of [WalletClient].
 */
class WalletClientFactory(
    private val context: Context,
    private val torClient: TorClient
) {
    /**
     * Creates a [CombinedWalletClient] which will leverage Tor for lightwalletd connection for functions specified
     * in [PartialTorWalletClient].
     * Other functions specified in [WalletClient] will use regular lightwalletd connection using [LightWalletClient].
     *
     * @return an instance of [WalletClient] for [endpoint]
     */
    suspend fun create(endpoint: LightWalletEndpoint): WalletClient =
        CombinedWalletClient.new(
            lightWalletClient = LightWalletClient.new(context, endpoint),
            torWalletClient = torClient.createIsolatedWalletClient("https://${endpoint.host}:${endpoint.port}"),
        )
}
