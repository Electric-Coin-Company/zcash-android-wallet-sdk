package cash.z.wallet.sdk.ext

import rpc.Service

fun Long.toBlockHeight(): Service.BlockID = Service.BlockID.newBuilder().setHeight(this).build()