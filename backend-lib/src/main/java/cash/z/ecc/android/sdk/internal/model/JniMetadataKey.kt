package cash.z.ecc.android.sdk.internal.model

import androidx.annotation.Keep
import cash.z.ecc.android.sdk.internal.jni.JNI_METADATA_KEY_CHAIN_CODE_SIZE
import cash.z.ecc.android.sdk.internal.jni.JNI_METADATA_KEY_SK_SIZE

/**
 * Serves as cross layer (Kotlin, Rust) communication class.
 *
 * @param sk the ZIP 32 key required to derive child keys.
 * @param chainCode The ZIP 32 chain code required to derive child keys.
 *
 * @throws IllegalArgumentException if the values are inconsistent.
 */
@Keep
class JniMetadataKey(
    val sk: ByteArray,
    val chainCode: ByteArray,
) {
    init {
        require(sk.size == JNI_METADATA_KEY_SK_SIZE) {
            "Account UUID must be 32 bytes"
        }

        require(chainCode.size == JNI_METADATA_KEY_CHAIN_CODE_SIZE) {
            "Seed fingerprint must be 32 bytes"
        }
    }
}
