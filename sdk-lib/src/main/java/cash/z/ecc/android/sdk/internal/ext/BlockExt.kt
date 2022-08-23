@file:Suppress("ktlint:filename")

package cash.z.ecc.android.sdk.internal.ext

import java.util.Locale

internal fun ByteArray.toHexReversed(): String {
    val sb = StringBuilder(size * 2)
    var i = size - 1
    while (i >= 0) {
        sb.append(String.format(Locale.ROOT, "%02x", this[i--]))
    }
    return sb.toString()
}
