package cash.z.ecc.android.sdk.internal.ext

import cash.z.ecc.android.sdk.model.BlockHeight

internal fun ClosedRange<BlockHeight>.isNotEmpty() = this.length() > 1

internal fun ClosedRange<BlockHeight>?.isNullOrEmpty() = this?.isEmpty() ?: true

// Add 1 because the range is inclusive
internal fun ClosedRange<BlockHeight>.length() =
    this.endInclusive.value
        .plus(1)
        .minus(this.start.value)

internal fun <T : Comparable<T>> ClosedRange<T>.overlaps(other: ClosedRange<T>): Boolean =
    start <= other.endInclusive && endInclusive >= other.start
