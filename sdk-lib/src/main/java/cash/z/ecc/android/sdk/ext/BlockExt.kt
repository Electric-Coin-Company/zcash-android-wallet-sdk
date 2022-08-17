package cash.z.ecc.android.sdk.ext

fun ByteArray.toHex(): String {
    val sb = StringBuilder(size * 2)
    for (b in this)
        sb.append(String.format("%02x", b))
    return sb.toString()
}

// Not used within the SDK, but is used by the Wallet app
@Suppress("unused", "MagicNumber")
fun String.fromHex(): ByteArray {
    val len = length
    val data = ByteArray(len / 2)
    var i = 0
    while (i < len) {
        data[i / 2] =
            ((Character.digit(this[i], 16) shl 4) + Character.digit(this[i + 1], 16)).toByte()
        i += 2
    }
    return data
}
