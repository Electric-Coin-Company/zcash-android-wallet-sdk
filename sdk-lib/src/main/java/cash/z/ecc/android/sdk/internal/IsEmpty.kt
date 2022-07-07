package cash.z.ecc.android.sdk.internal

import cash.z.ecc.android.sdk.model.BlockHeight

internal fun ClosedRange<BlockHeight>?.isEmpty() = this?.isEmpty() ?: true
