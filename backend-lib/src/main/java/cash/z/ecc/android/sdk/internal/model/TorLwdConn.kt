package cash.z.ecc.android.sdk.internal.model

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class TorLwdConn private constructor(
    private var nativeHandle: Long?,
) {
    private val accessMutex = Mutex()

    suspend fun dispose() =
        accessMutex.withLock {
            withContext(Dispatchers.IO) {
                nativeHandle?.let { freeLightwalletdConnection(it) }
                nativeHandle = null
            }
        }

    /**
     * Fetches the transaction with the given ID.
     */
    suspend fun fetchTransaction(txId: ByteArray) =
        accessMutex.withLock {
            withContext(Dispatchers.IO) {
                checkNotNull(nativeHandle) { "TorLwdConn is disposed" }
                fetchTransaction(nativeHandle!!, txId)
            }
        }

    /**
     * Submits a transaction to the Zcash network via the given lightwalletd connection.
     */
    suspend fun submitTransaction(tx: ByteArray) =
        accessMutex.withLock {
            withContext(Dispatchers.IO) {
                checkNotNull(nativeHandle) { "TorLwdConn is disposed" }
                submitTransaction(nativeHandle!!, tx)
            }
        }

    companion object {
        internal suspend fun new(nativeHandle: Long): TorLwdConn = TorLwdConn(nativeHandle = nativeHandle)

        //
        // External Functions
        //

        @JvmStatic
        private external fun freeLightwalletdConnection(nativeHandle: Long)

        /**
         * @throws RuntimeException as a common indicator of the operation failure
         */
        @JvmStatic
        @Throws(RuntimeException::class)
        private external fun fetchTransaction(
            nativeHandle: Long,
            txId: ByteArray
        ): ByteArray

        /**
         * @throws RuntimeException as a common indicator of the operation failure
         */
        @JvmStatic
        @Throws(RuntimeException::class)
        private external fun submitTransaction(
            nativeHandle: Long,
            tx: ByteArray
        ): Boolean
    }
}
