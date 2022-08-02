@file:Suppress("ktlint:filename")

package cash.z.ecc.android.sdk.model

/*
 * This is a set of extension functions currently, because we expect them to change in the future.
 */

fun LightWalletEndpoint.Companion.defaultForNetwork(zcashNetwork: ZcashNetwork): LightWalletEndpoint {
    return when (zcashNetwork.id) {
        ZcashNetwork.Mainnet.id -> LightWalletEndpoint.Mainnet
        ZcashNetwork.Testnet.id -> LightWalletEndpoint.Testnet
        else -> error("Unknown network id: ${zcashNetwork.id}")
    }
}

/**
 * This is a special localhost value on the Android emulator, which allows it to contact
 * the localhost of the computer running the emulator.
 */
private const val COMPUTER_LOCALHOST = "10.0.2.2"

private const val DEFAULT_PORT = 9087

val LightWalletEndpoint.Companion.Mainnet
    get() = LightWalletEndpoint(
        "mainnet.lightwalletd.com",
        DEFAULT_PORT,
        isSecure = true
    )

val LightWalletEndpoint.Companion.Testnet
    get() = LightWalletEndpoint(
        "testnet.lightwalletd.com",
        DEFAULT_PORT,
        isSecure = true
    )

val LightWalletEndpoint.Companion.Darkside
    get() = LightWalletEndpoint(
        COMPUTER_LOCALHOST,
        DEFAULT_PORT,
        isSecure = false
    )
