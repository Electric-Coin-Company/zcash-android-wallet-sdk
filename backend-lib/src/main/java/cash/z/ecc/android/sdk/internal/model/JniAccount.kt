package cash.z.ecc.android.sdk.internal.model

import androidx.annotation.Keep
import cash.z.ecc.android.sdk.internal.jni.JNI_ACCOUNT_UUID_BYTES_SIZE
import cash.z.ecc.android.sdk.internal.jni.JNI_ACCOUNT_SEED_FP_BYTES_SIZE

/**
 * Serves as cross layer (Kotlin, Rust) communication class.
 *
 * @param accountUuid the "one-way stable" identifier for the account.
 * @param ufvk The account's Unified Full Viewing Key, if any.
 * @throws IllegalArgumentException if the values are inconsistent.
 */
@Keep
class JniAccount(
    val accountUuid: ByteArray,
    val ufvk: String?,
    val accountName: String?
    val keySource: String?
    val seedFingerprint: ByteArray?
    val hdAccountIndex: Long?
) {
    init {
        require(accountUuid.size == JNI_ACCOUNT_UUID_BYTES_SIZE) {
            "Account UUID must be 16 bytes"
        }

        require(accountUuid.size == JNI_ACCOUNT_SEED_FP_BYTES_SIZE) {
            "Seed fingerprint must be 32 bytes"
        }
    }
}
