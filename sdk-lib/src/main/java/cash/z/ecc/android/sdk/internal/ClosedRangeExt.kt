package cash.z.ecc.android.sdk.internal

import cash.z.ecc.android.sdk.model.BlockHeight

internal fun ClosedRange<BlockHeight>?.isNullOrEmpty() = this?.isEmpty() ?: true

// Add 1 because the range is inclusive
internal fun ClosedRange<BlockHeight>.length() =
    this.endInclusive.value.plus(1).minus(this.start.value)