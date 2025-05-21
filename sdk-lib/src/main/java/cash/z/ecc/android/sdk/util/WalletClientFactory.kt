package cash.z.ecc.android.sdk.util

import android.content.Context
import cash.z.ecc.android.sdk.internal.model.CombinedWalletClient
import cash.z.ecc.android.sdk.internal.model.TorClient
import cash.z.ecc.android.sdk.model.ZcashNetwork
import co.electriccoin.lightwallet.client.LightWalletClient
import co.electriccoin.lightwallet.client.PartialTorWalletClient
import co.electriccoin.lightwallet.client.WalletClient
import co.electriccoin.lightwallet.client.model.LightWalletEndpoint

/**
 * A factory responsible for creating an instance of [WalletClient].
 */
class WalletClientFactory(
    private val context: Context,
    private val isTorEnabled: Boolean,
    private val torClient: TorClient,
    private val network: ZcashNetwork
) {
    /**
     * If [isTorEnabled] is true, a [CombinedWalletClient] will be returned which will leverage tor for lightwalletd
     * connection for functions specified in [PartialTorWalletClient]. Other functions specified in [WalletClient]
     * will use regular lightwalletd connection using [LightWalletClient].
     *
     * @return an instance of [WalletClient] for [endpoint]
     */
    suspend fun create(endpoint: LightWalletEndpoint): WalletClient {
        val lightWalletClient = LightWalletClient.new(context, endpoint)
        return if (isTorEnabled) {
            CombinedWalletClient.new(
                lightWalletClient = lightWalletClient,
                torWalletClient =
                    torClient.createWalletClient(
                        "https://${endpoint.host}:${endpoint.port}"
                    ),
            )
        } else {
            lightWalletClient
        }
    }
}
