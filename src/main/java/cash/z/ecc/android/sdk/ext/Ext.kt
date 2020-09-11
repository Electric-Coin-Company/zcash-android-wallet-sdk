package cash.z.ecc.android.sdk.ext

internal inline fun <R> tryNull(block: () -> R): R? {
    return try {
        block()
    } catch (t: Throwable) {
        null
    }
}

internal inline fun <R> tryWarn(message: String,  block: () -> R): R? {
    return try {
        block()
    } catch (t: Throwable) {
        twig("$message due to: $t")
        return null
    }
}

