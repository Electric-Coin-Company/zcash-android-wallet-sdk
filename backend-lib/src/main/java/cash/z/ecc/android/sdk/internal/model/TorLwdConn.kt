package cash.z.ecc.android.sdk.internal.model

import cash.z.wallet.sdk.internal.rpc.Service.BlockID
import cash.z.wallet.sdk.internal.rpc.Service.LightdInfo
import cash.z.wallet.sdk.internal.rpc.Service.TreeState
import co.electriccoin.lightwallet.client.model.BlockIDUnsafe
import co.electriccoin.lightwallet.client.model.LightWalletEndpointInfoUnsafe
import co.electriccoin.lightwallet.client.model.TreeStateUnsafe
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
     * Returns information about this lightwalletd instance and the blockchain.
     */
    suspend fun getServerInfo(): LightWalletEndpointInfoUnsafe =
        accessMutex.withLock {
            withContext(Dispatchers.IO) {
                checkNotNull(nativeHandle) { "TorLwdConn is disposed" }
                LightWalletEndpointInfoUnsafe.new(
                    LightdInfo.parseFrom(
                        getServerInfo(nativeHandle!!)
                    )
                )
            }
        }

    /**
     * Returns information about the latest block in the network.
     */
    suspend fun getLatestBlock(): BlockIDUnsafe =
        accessMutex.withLock {
            withContext(Dispatchers.IO) {
                checkNotNull(nativeHandle) { "TorLwdConn is disposed" }
                BlockIDUnsafe.new(
                    BlockID.parseFrom(
                        getLatestBlock(nativeHandle!!)
                    )
                )
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

    /**
     * Fetches the note commitment tree state corresponding to the given block height.
     */
    suspend fun getTreeState(height: Long): TreeStateUnsafe =
        accessMutex.withLock {
            withContext(Dispatchers.IO) {
                checkNotNull(nativeHandle) { "TorLwdConn is disposed" }
                TreeStateUnsafe.new(
                    TreeState.parseFrom(
                        getTreeState(nativeHandle!!, height)
                    )
                )
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
        private external fun getServerInfo(nativeHandle: Long): ByteArray

        /**
         * @throws RuntimeException as a common indicator of the operation failure
         */
        @JvmStatic
        @Throws(RuntimeException::class)
        private external fun getLatestBlock(nativeHandle: Long): ByteArray

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
        )

        /**
         * @throws RuntimeException as a common indicator of the operation failure
         */
        @JvmStatic
        @Throws(RuntimeException::class)
        private external fun getTreeState(
            nativeHandle: Long,
            fromHeight: Long
        ): ByteArray
    }
}
