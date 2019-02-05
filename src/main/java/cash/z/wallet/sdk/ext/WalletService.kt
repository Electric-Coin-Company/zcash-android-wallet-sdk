package cash.z.wallet.sdk.ext

import cash.z.wallet.sdk.rpc.Service

inline fun Int.toBlockHeight(): Service.BlockID = Service.BlockID.newBuilder().setHeight(this.toLong()).build()
inline fun IntRange.toBlockRange(): Service.BlockRange =
    Service.BlockRange.newBuilder()
        .setStart(this.first.toBlockHeight())
        .setEnd(this.last.toBlockHeight())
        .build()
