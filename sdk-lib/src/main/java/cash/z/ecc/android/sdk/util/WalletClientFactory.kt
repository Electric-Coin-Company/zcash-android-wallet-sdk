package cash.z.ecc.android.sdk.util

import android.content.Context
import cash.z.ecc.android.sdk.internal.model.CombinedWalletClient
import cash.z.ecc.android.sdk.internal.model.TorClient
import cash.z.ecc.android.sdk.model.ZcashNetwork
import co.electriccoin.lightwallet.client.LightWalletClient
import co.electriccoin.lightwallet.client.BaseWalletClient
import co.electriccoin.lightwallet.client.model.LightWalletEndpoint

class WalletClientFactory(
    private val context: Context,
    private val isTorEnabled: Boolean,
    private val torClient: TorClient,
    private val network: ZcashNetwork
) {
    suspend fun create(endpoint: LightWalletEndpoint): BaseWalletClient {
        val lightWalletClient = LightWalletClient.new(context, endpoint)
        return if (isTorEnabled) {
            CombinedWalletClient(
                lightWalletClient = lightWalletClient,
                torWalletClient = torClient.createWalletClient(
                    "http://${network.networkName}.${endpoint.host}:${endpoint.port}"
                ),
            )
        } else {
            lightWalletClient
        }
    }
}
