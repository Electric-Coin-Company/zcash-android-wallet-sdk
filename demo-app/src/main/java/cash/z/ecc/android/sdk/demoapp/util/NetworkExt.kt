package cash.z.ecc.android.sdk.demoapp.util

import android.content.Context
import cash.z.ecc.android.sdk.demoapp.R
import cash.z.ecc.android.sdk.type.ZcashNetwork

fun ZcashNetwork.Companion.fromResources(context: Context) = ZcashNetwork.valueOf(context.getString(
    R.string.network_name))