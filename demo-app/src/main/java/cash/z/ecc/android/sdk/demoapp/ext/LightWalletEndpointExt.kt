@file:Suppress("ktlint:standard:filename", "MagicNumber")

package cash.z.ecc.android.sdk.demoapp.ext

import cash.z.ecc.android.sdk.model.ZcashNetwork
import co.electriccoin.lightwallet.client.model.LightWalletEndpoint

/*
 * This set of extension functions suited for defaults for the Demo app is not part of the SDK's public APIs.
 */

internal fun LightWalletEndpoint.Companion.defaultForNetwork(zcashNetwork: ZcashNetwork): LightWalletEndpoint {
    return when (zcashNetwork.id) {
        ZcashNetwork.Mainnet.id -> LightWalletEndpoint.Mainnet
        ZcashNetwork.Testnet.id -> LightWalletEndpoint.Testnet
        else -> error("Unknown network id: ${zcashNetwork.id}")
    }
}

internal val LightWalletEndpoint.Companion.Mainnet
    get() =
        LightWalletEndpoint(
            "zec.rocks",
            443,
            isSecure = true
        )

internal val LightWalletEndpoint.Companion.Testnet
    get() =
        LightWalletEndpoint(
            "lightwalletd.testnet.electriccoin.co",
            9067,
            isSecure = true
        )

const val MIN_PORT_NUMBER = 1
const val MAX_PORT_NUMBER = 65535

internal fun LightWalletEndpoint.isValid() = host.isNotBlank() && port in MIN_PORT_NUMBER..MAX_PORT_NUMBER
