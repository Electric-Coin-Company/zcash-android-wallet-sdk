package cash.z.ecc.android.sdk.internal.model

import cash.z.ecc.android.sdk.internal.ext.existsSuspend
import cash.z.ecc.android.sdk.internal.ext.mkdirsSuspend
import cash.z.ecc.android.sdk.internal.jni.RustBackend
import dalvik.annotation.optimization.CriticalNative
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.math.BigDecimal

class TorClient(
    private val nativeHandle: Long?,
) {
    suspend fun dispose() = withContext(Dispatchers.IO) {
        nativeHandle?.let { freeTorRuntime(it) }
    }

    suspend fun getExchangeRateUsd(): BigDecimal = withContext(Dispatchers.IO) {
        getExchangeRateUsd(nativeHandle!!)
    }

    companion object {
        suspend fun new(torDir: File): TorClient = withContext(Dispatchers.IO) {
            RustBackend.loadLibrary()

            // Ensure that the directory exists.
            torDir.mkdirsSuspend()
            if (!torDir.existsSuspend()) {
                error("${torDir.path} directory does not exist and could not be created.")
            }

            TorClient(createTorRuntime(torDir.path))
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
