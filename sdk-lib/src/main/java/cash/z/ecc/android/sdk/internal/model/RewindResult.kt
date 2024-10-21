package cash.z.ecc.android.sdk.internal.model

import cash.z.ecc.android.sdk.exception.SdkException
import cash.z.ecc.android.sdk.model.BlockHeight

sealed interface RewindResult {
    data class Success(val height: BlockHeight) : RewindResult

    data class Invalid(
        val safeRewindHeight: BlockHeight?,
        val requestedHeight: BlockHeight,
    ) : RewindResult

    companion object {
        fun new(jni: JniRewindResult): RewindResult {
            return when (jni) {
                is JniRewindResult.Success -> Success(BlockHeight.new(jni.height))
                is JniRewindResult.Invalid ->
                    Invalid(
                        if (jni.safeRewindHeight == -1L) {
                            null
                        } else {
                            BlockHeight.new(jni.safeRewindHeight)
                        },
                        BlockHeight.new(jni.requestedHeight),
                    )

                else -> throw SdkException("Unknown JniRewindResult variant", cause = null)
            }
        }
    }
}
