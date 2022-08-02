@file:Suppress("ktlint:filename")

package cash.z.ecc.android.sdk.demoapp.util

import android.content.Context
import cash.z.ecc.android.sdk.demoapp.R
import cash.z.ecc.android.sdk.model.ZcashNetwork
import java.util.*

fun ZcashNetwork.Companion.fromResources(context: Context): ZcashNetwork {
    val networkNameFromResources = context.getString(R.string.network_name).lowercase(Locale.ROOT)
    return if (networkNameFromResources == Testnet.networkName) {
        ZcashNetwork.Testnet
    } else if (networkNameFromResources.lowercase(Locale.ROOT) == Mainnet.networkName) {
        ZcashNetwork.Mainnet
    } else {
        throw IllegalArgumentException("Unknown network name: $networkNameFromResources")
    }
}
