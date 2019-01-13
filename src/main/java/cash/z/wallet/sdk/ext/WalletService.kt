package cash.z.wallet.sdk.ext

import rpc.Service

inline fun Long.toBlockHeight(): Service.BlockID = Service.BlockID.newBuilder().setHeight(this).build()
inline fun LongRange.toBlockRange(): Service.BlockRange =
    Service.BlockRange.newBuilder()
        .setStart(this.first.toBlockHeight())
        .setEnd(this.last.toBlockHeight())
        .build()
