@file:Suppress("ktlint:standard:filename")

package cash.z.ecc.android.sdk.internal.ext

fun Long.isInUIntRange(): Boolean = this >= 0L && this <= UInt.MAX_VALUE.toLong()
