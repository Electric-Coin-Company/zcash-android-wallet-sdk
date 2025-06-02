package cash.z.ecc.android.sdk.internal.model

import androidx.annotation.Keep

/**
 * Serves as cross layer (Kotlin, Rust) communication class.
 *
 * @param status the HTTP status code
 * @param version the HTTP version of the response
 * @param body the response body
 */
@Keep
@Suppress("LongParameterList")
class JniHttpResponseBytes(
    val status: Int,
    val version: String,
    val headers: Array<JniHttpHeader>,
    val body: ByteArray,
) {
}