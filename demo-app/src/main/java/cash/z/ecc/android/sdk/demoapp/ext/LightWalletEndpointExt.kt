@file:Suppress("ktlint:standard:filename")

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

private const val DEFAULT_PORT = 9067

internal val LightWalletEndpoint.Companion.Mainnet
    get() =
        LightWalletEndpoint(
            "mainnet.lightwalletd.com",
            DEFAULT_PORT,
            isSecure = true
        )

internal val LightWalletEndpoint.Companion.Testnet
    get() =
        LightWalletEndpoint(
            "lightwalletd.testnet.electriccoin.co",
            DEFAULT_PORT,
            isSecure = true
        )

const val MIN_PORT_NUMBER = 1
const val MAX_PORT_NUMBER = 65535

internal fun LightWalletEndpoint.isValid() = host.isNotBlank() && port in MIN_PORT_NUMBER..MAX_PORT_NUMBER
