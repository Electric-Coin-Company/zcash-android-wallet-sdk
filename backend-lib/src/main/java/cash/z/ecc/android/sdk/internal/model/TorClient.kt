package cash.z.ecc.android.sdk.internal.model

import cash.z.ecc.android.sdk.internal.ext.existsSuspend
import cash.z.ecc.android.sdk.internal.ext.mkdirsSuspend
import cash.z.ecc.android.sdk.internal.jni.RustBackend
import co.electriccoin.lightwallet.client.PartialTorWalletClient
import co.electriccoin.lightwallet.client.util.Disposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.math.BigDecimal

class TorClient private constructor(
    private var nativeHandle: Long?,
) : Disposable {
    private val accessMutex = Mutex()

    override suspend fun dispose() =
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
    suspend fun isolatedTorClient(): TorClient = accessMutex.withLock { isolatedTorClientInternal() }

    /**
     * The caller MUST acquire `accessMutex` before calling this function.
     *
     * @return a new isolated `TorClient` handle.
     */
    private suspend fun isolatedTorClientInternal() = withContext(Dispatchers.IO) {
        checkNotNull(nativeHandle) { "TorClient is disposed" }
        TorClient(nativeHandle = isolatedClient(nativeHandle!!))
    }

    /**
     * Changes the client's current dormant mode, putting background tasks to sleep or waking
     * them up as appropriate.
     *
     * This can be used to conserve CPU usage if you aren’t planning on using the client for
     * a while, especially on mobile platforms.
     */
    suspend fun setDormant(mode: TorDormantMode) =
        accessMutex.withLock {
            withContext(Dispatchers.IO) {
                checkNotNull(nativeHandle) { "TorClient is disposed" }
                setDormant(nativeHandle!!, mode.ordinal)
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
     * This client is isolated from any other Tor usage, and queries made with this client
     * are isolated from each other (but may still be correlatable by the server through
     * request timing, if the caller does not mitigate timing attacks).
     */
    suspend fun createIsolatedWalletClient(endpoint: String): PartialTorWalletClient =
        accessMutex.withLock {
            checkNotNull(nativeHandle) { "TorClient is disposed" }
            IsolatedTorWalletClient.new(
                isolatedTorClient = isolatedTorClientInternal(),
                endpoint = endpoint
            )
        }

    /**
     * Connects to the lightwalletd server at the given endpoint.
     *
     * This client is isolated from any other Tor usage. Queries made with this client are
     * not isolated from each other; use `createIsolatedWalletClient()` if you need this.
     */
    suspend fun createWalletClient(endpoint: String): PartialTorWalletClient =
        accessMutex.withLock {
            withContext(Dispatchers.IO) {
                checkNotNull(nativeHandle) { "TorClient is disposed" }
                TorWalletClient.new(
                    nativeHandle =
                        connectToLightwalletd(
                            nativeHandle = nativeHandle!!,
                            endpoint = endpoint
                        )
                )
            }
        }

    companion object {
        /**
         * @return a new instance of [TorClient] or null if an error occurred.
         */
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
        private external fun setDormant(nativeHandle: Long, mode: Int)

        /**
         * @throws RuntimeException as a common indicator of the operation failure
         */
        @JvmStatic
        @Throws(RuntimeException::class)
        private external fun getExchangeRateUsd(nativeHandle: Long): BigDecimal

        /**
         * @throws RuntimeException as a common indicator of the operation failure
         */
        @JvmStatic
        @Throws(RuntimeException::class)
        private external fun connectToLightwalletd(nativeHandle: Long, endpoint: String): Long
    }
}

/**
 * What level of sleep to put a Tor client into.
 *
 * The order of the enum constants MUST match the order in `parse_tor_dormant_mode()` in
 * `backend-lib/src/main/rust/lib.rs`.
 */
enum class TorDormantMode {
    /**
     * The client functions as normal, and background tasks run periodically.
     */
    NORMAL,

    /**
     * Background tasks are suspended, conserving CPU usage. Attempts to use the
     * client will wake it back up again.
     */
    SOFT,
}
