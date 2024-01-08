@file:Suppress("ktlint:standard:filename")

package cash.z.ecc.android.sdk.ext

import co.electriccoin.lightwallet.client.model.LightWalletEndpoint

/*
 * This is a set of the [LightWalletEndpoint] extension functions used for the darkside tests only and not part of the
 *  public APIs.
 */

/**
 * This is a special localhost value on the Android emulator, which allows it to contact
 * the localhost of the computer running the emulator.
 */
private const val COMPUTER_LOCALHOST = "10.0.2.2"

private const val DEFAULT_PORT = 9067

internal val LightWalletEndpoint.Companion.Darkside
    get() =
        LightWalletEndpoint(
            COMPUTER_LOCALHOST,
            DEFAULT_PORT,
            isSecure = false
        )
