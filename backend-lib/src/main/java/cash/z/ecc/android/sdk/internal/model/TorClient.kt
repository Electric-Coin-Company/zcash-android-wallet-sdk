package cash.z.ecc.android.sdk.internal.model

import cash.z.ecc.android.sdk.internal.ext.existsSuspend
import cash.z.ecc.android.sdk.internal.ext.mkdirsSuspend
import cash.z.ecc.android.sdk.internal.jni.RustBackend
import co.electriccoin.lightwallet.client.PartialTorWalletClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.math.BigDecimal

class TorClient private constructor(
    private var nativeHandle: Long?,
) {
    private val accessMutex = Mutex()

    suspend fun dispose() =
        accessMutex.withLock {
            withContext(Dispatchers.IO) {
                nativeHandle?.let { freeTorRuntime(it) }
                nativeHandle = null
            }
        }

    /**
     * Returns a new isolated `TorClient` handle.
     *
     * The two `TorClient`s will share internal state and configuration, but their streams
     * will never share circuits with one another.
     *
     * Use this method when you want separate parts of your program to each have a
     * `TorClient` handle, but where you don't want their activities to be linkable to one
     * another over the Tor network.
     *
     * Calling this method is usually preferable to creating a completely separate
     * `TorClient` instance, since it can share its internals with the existing `TorClient`.
     */
    suspend fun isolatedClient(): TorClient =
        accessMutex.withLock {
            withContext(Dispatchers.IO) {
                checkNotNull(nativeHandle) { "TorClient is disposed" }
                TorClient(nativeHandle = isolatedClient(nativeHandle!!))
            }
        }

    suspend fun getExchangeRateUsd(): BigDecimal =
        accessMutex.withLock {
            withContext(Dispatchers.IO) {
                checkNotNull(nativeHandle) { "TorClient is disposed" }
                getExchangeRateUsd(nativeHandle!!)
            }
        }

    /**
     * Connects to the lightwalletd server at the given endpoint.
     *
     * Each connection returned by this method is isolated from any other Tor usage.
     */
    suspend fun createWalletClient(endpoint: String): PartialTorWalletClient =
        accessMutex.withLock {
            withContext(Dispatchers.IO) {
                checkNotNull(nativeHandle) { "TorClient is disposed" }
                IsolatedTorWalletClient.new(endpoint, nativeHandle!!)
            }
        }

    companion object {
        suspend fun new(torDir: File): TorClient =
            withContext(Dispatchers.IO) {
                RustBackend.loadLibrary()

                // Ensure that the directory exists.
                torDir.mkdirsSuspend()
                if (!torDir.existsSuspend()) {
                    error("${torDir.path} directory does not exist and could not be created.")
                }

                TorClient(nativeHandle = createTorRuntime(torDir.path))
            }

        //
        // External Functions
        //

        /**
         * @throws RuntimeException as a common indicator of the operation failure
         */
        @JvmStatic
        @Throws(RuntimeException::class)
        private external fun createTorRuntime(torDir: String): Long

        @JvmStatic
        private external fun freeTorRuntime(nativeHandle: Long)

        /**
         * @throws RuntimeException as a common indicator of the operation failure
         */
        @JvmStatic
        @Throws(RuntimeException::class)
        private external fun isolatedClient(nativeHandle: Long): Long

        /**
         * @throws RuntimeException as a common indicator of the operation failure
         */
        @JvmStatic
        @Throws(RuntimeException::class)
        private external fun getExchangeRateUsd(nativeHandle: Long): BigDecimal
    }
}
