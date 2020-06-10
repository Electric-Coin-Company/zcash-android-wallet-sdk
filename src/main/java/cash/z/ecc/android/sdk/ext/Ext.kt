package cash.z.ecc.android.sdk.ext

internal inline fun <R> tryNull(block: () -> R): R? {
    return try {
        block()
    } catch (t: Throwable) {
        null
    }
}
