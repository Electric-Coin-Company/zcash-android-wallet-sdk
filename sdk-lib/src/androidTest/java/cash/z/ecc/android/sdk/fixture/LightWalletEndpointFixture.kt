@file:Suppress("ktlint:standard:filename")

package cash.z.ecc.android.sdk.fixture

import cash.z.ecc.android.sdk.model.ZcashNetwork
import co.electriccoin.lightwallet.client.model.LightWalletEndpoint

/*
 * This is a set of extension functions for testing purposes only.
 */

object LightWalletEndpointFixture {
    private const val DEFAULT_PORT = 9067

    val LightWalletEndpoint.Companion.Mainnet
        get() =
            LightWalletEndpoint(
                "mainnet.lightwalletd.com",
                DEFAULT_PORT,
                isSecure = true
            )

    val LightWalletEndpoint.Companion.Testnet
        get() =
            LightWalletEndpoint(
                "lightwalletd.testnet.electriccoin.co",
                DEFAULT_PORT,
                isSecure = true
            )

    fun newEndpointForNetwork(zcashNetwork: ZcashNetwork): LightWalletEndpoint {
        return when (zcashNetwork.id) {
            ZcashNetwork.Mainnet.id -> LightWalletEndpoint.Mainnet
            ZcashNetwork.Testnet.id -> LightWalletEndpoint.Testnet
            else -> error("Unknown network id: ${zcashNetwork.id}")
        }
    }
}
