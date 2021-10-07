package cash.z.ecc.android.sdk.internal.ext

import cash.z.ecc.android.sdk.internal.twig

internal inline fun <R> tryNull(block: () -> R): R? {
    return try {
        block()
    } catch (t: Throwable) {
        null
    }
}

/**
 * Execute the given block, converting exceptions into warnings. This can be further controlled by
 * filters so that only exceptions matching the given constraints are converted into warnings.
 *
 * @param ifContains only convert an exception into a warning if it contains the given text
 * @param unlessContains convert all exceptions into warnings unless they contain the given text
 */
internal inline fun <R> tryWarn(
    message: String,
    ifContains: String? = null,
    unlessContains: String? = null,
    block: () -> R
): R? {
    return try {
        block()
    } catch (t: Throwable) {
        val shouldThrowAnyway = (
            unlessContains != null &&
                (t.message?.toLowerCase()?.contains(unlessContains.toLowerCase()) == true)
            ) ||
            (
                ifContains != null &&
                    (t.message?.toLowerCase()?.contains(ifContains.toLowerCase()) == false)
                )
        if (shouldThrowAnyway) {
            throw t
        } else {
            twig("$message due to: $t")
            null
        }
    }
}
