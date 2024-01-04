package cash.z.ecc.android.sdk.internal.ext

import cash.z.ecc.android.sdk.internal.Twig

@Suppress("SwallowedException", "TooGenericExceptionCaught")
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
@Suppress("TooGenericExceptionCaught")
internal inline fun <R> tryWarn(
    @Suppress("UNUSED_PARAMETER") message: String,
    ifContains: String? = null,
    unlessContains: String? = null,
    block: () -> R
): R? {
    return try {
        block()
    } catch (t: Throwable) {
        val shouldThrowAnyway =
            (
                unlessContains != null &&
                    (t.message?.lowercase()?.contains(unlessContains.lowercase()) == true)
            ) ||
                (
                    ifContains != null &&
                        (t.message?.lowercase()?.contains(ifContains.lowercase()) == false)
                )
        if (shouldThrowAnyway) {
            throw t
        } else {
            Twig.debug(t) { "" }
            null
        }
    }
}

// Note: Do NOT change these texts as they match the ones from ScanError in
// librustzcash/zcash_client_backend/src/scanning.rs
internal const val PREV_HASH_MISMATCH =
    "The parent hash of proposed block does not correspond to the block hash at " +
        "height" // $NON-NLS
internal const val BLOCK_HEIGHT_DISCONTINUITY = "Block height discontinuity at height" // $NON-NLS
internal const val TREE_SIZE_MISMATCH =
    "note commitment tree size provided by a compact block did not match the " +
        "expected size at height" // $NON-NLS

/**
 * Check whether this error is the result of a failed continuity while scanning new blocks in the Rust layer.
 *
 * @return true in case of the check match, false otherwise
 */
internal fun Throwable.isScanContinuityError(): Boolean {
    val errorMessages =
        listOf(
            PREV_HASH_MISMATCH,
            BLOCK_HEIGHT_DISCONTINUITY,
            TREE_SIZE_MISMATCH
        )
    errorMessages.forEach { errMessage ->
        if (this.message?.lowercase()?.contains(errMessage.lowercase()) == true) {
            return true
        }
    }
    return false
}
