@file:Suppress("ktlint:filename")

package cash.z.ecc.android.sdk.demoapp.util

import android.content.Context
import cash.z.ecc.android.sdk.demoapp.R
import cash.z.ecc.android.sdk.model.ZcashNetwork
import java.util.Locale

fun ZcashNetwork.Companion.fromResources(context: Context): ZcashNetwork {
    val networkNameFromResources = context.getString(R.string.network_name).lowercase(Locale.ROOT)
    @Suppress("UseRequire")
    return if (networkNameFromResources == Testnet.networkName) {
        Testnet
    } else if (networkNameFromResources.lowercase(Locale.ROOT) == Mainnet.networkName) {
        Mainnet
    } else {
        throw IllegalArgumentException("Unknown network name: $networkNameFromResources")
    }
}
