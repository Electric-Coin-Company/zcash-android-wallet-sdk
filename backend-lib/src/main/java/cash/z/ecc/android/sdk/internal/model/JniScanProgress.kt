package cash.z.ecc.android.sdk.internal.model

import androidx.annotation.Keep

/**
 * Serves as cross layer (Kotlin, Rust) communication class.
 *
 * @param numerator the numerator of the progress ratio
 * @param endHeight the denominator of the progress ratio
 */
@Keep
class JniScanProgress(
    val numerator: Long,
    val denominator: Long
) {
}
