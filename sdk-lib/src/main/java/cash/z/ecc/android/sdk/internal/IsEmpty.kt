package cash.z.ecc.android.sdk.internal

import cash.z.ecc.android.sdk.model.BlockHeight

internal fun ClosedRange<BlockHeight>?.isNullOrEmpty() = this?.isEmpty() ?: true
