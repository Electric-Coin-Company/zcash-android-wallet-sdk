package cash.z.ecc.android.sdk.ext

internal inline fun <R> tryNull(block: () -> R): R? {
    return try {
        block()
    } catch (t: Throwable) {
        null
    }
}

internal inline fun <R> tryWarn(
    message: String,
    unlessContains: String? = null,
    block: () -> R
): R? {
    return try {
        block()
    } catch (t: Throwable) {
        if (unlessContains != null &&
            (t.message?.toLowerCase()?.contains(unlessContains.toLowerCase()) == true)
        ) {
            throw t
        } else {
            twig("$message due to: $t")
            null
        }
    }
}
