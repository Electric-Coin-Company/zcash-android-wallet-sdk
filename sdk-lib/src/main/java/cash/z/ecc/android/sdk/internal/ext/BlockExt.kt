package cash.z.ecc.android.sdk.internal.ext

internal fun ByteArray.toHexReversed(): String {
    val sb = StringBuilder(size * 2)
    var i = size - 1
    while (i >= 0)
        sb.append(String.format("%02x", this[i--]))
    return sb.toString()
}
