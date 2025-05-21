package cash.z.ecc.android.sdk.internal.model

import androidx.annotation.Keep
import cash.z.ecc.android.sdk.internal.ext.isInUIntRange

@Keep
class JniTransaction(
    val height: Long,
    val data: ByteArray,
) {
    init {
        require(height.isInUIntRange()) {
            "Height $height is outside of allowed UInt range"
        }
    }
}
