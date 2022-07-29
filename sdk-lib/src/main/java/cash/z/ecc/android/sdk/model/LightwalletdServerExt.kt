@file:Suppress("ktlint:filename")

package cash.z.ecc.android.sdk.model

/*
 * This is a set of extension functions currently, because we expect them to change in the future.
 */

fun LightwalletdServer.Companion.defaultForNetwork(zcashNetwork: ZcashNetwork): LightwalletdServer {
    return when (zcashNetwork.id) {
        ZcashNetwork.Mainnet.id -> LightwalletdServer.Mainnet
        ZcashNetwork.Testnet.id -> LightwalletdServer.Testnet
        else -> error("Unknown network id: ${zcashNetwork.id}")
    }
}

/**
 * This is a special localhost value on the Android emulator, which allows it to contact
 * the localhost of the computer running the emulator.
 */
private const val COMPUTER_LOCALHOST = "10.0.2.2"

private const val DEFAULT_PORT = 9087

val LightwalletdServer.Companion.Mainnet
    get() = LightwalletdServer(
        "mainnet.lightwalletd.com",
        @Suppress("MagicNumber")
        DEFAULT_PORT,
        isSecure = true
    )

val LightwalletdServer.Companion.Testnet
    get() = LightwalletdServer(
        "testnet.lightwalletd.com",
        DEFAULT_PORT,
        isSecure = true
    )

val LightwalletdServer.Companion.Darkside
    get() = LightwalletdServer(
        COMPUTER_LOCALHOST,
        DEFAULT_PORT,
        isSecure = false
    )
