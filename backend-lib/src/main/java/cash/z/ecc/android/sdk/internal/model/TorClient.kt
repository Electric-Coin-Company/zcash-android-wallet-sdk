package cash.z.ecc.android.sdk.internal.model

import cash.z.ecc.android.sdk.internal.ext.existsSuspend
import cash.z.ecc.android.sdk.internal.ext.mkdirsSuspend
import cash.z.ecc.android.sdk.internal.jni.RustBackend
import dalvik.annotation.optimization.CriticalNative
import java.io.File
import java.math.BigDecimal

class TorClient(
    private val nativeHandle: Long?,
) {
    fun dispose() {
        nativeHandle?.let { freeTorRuntime(it) }
    }

    fun getExchangeRateUsd(): BigDecimal {
        return getExchangeRateUsd(nativeHandle!!)
    }

    companion object {
        suspend fun new(torDir: File): TorClient {
            RustBackend.loadLibrary()

            // Ensure that the directory exists.
            torDir.mkdirsSuspend()
            if (!torDir.existsSuspend()) {
                error("${torDir.path} directory does not exist and could not be created.")
            }

            return TorClient(createTorRuntime(torDir.path))
        }

        //
        // External Functions
        //

        @JvmStatic
        private external fun createTorRuntime(torDir: String): Long

        @CriticalNative
        @JvmStatic
        private external fun freeTorRuntime(nativeHandle: Long)

        @JvmStatic
        private external fun getExchangeRateUsd(nativeHandle: Long): BigDecimal
    }
}
