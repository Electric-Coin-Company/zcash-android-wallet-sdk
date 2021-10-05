package cash.z.ecc.android.sdk.demoapp.util

import android.content.Context
import cash.z.ecc.android.sdk.demoapp.R
import cash.z.ecc.android.sdk.type.NetworkType
import cash.z.ecc.android.sdk.type.ZcashNetwork

fun ZcashNetwork.Companion.fromResources(context: Context): ZcashNetwork = cash.z.ecc.android.sdk.type.NetworkType.valueOf(
    context.getString(
        R.string.network_name
    )
)
