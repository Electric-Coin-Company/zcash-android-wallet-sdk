package cash.z.ecc.android.sdk.internal.model

import cash.z.wallet.sdk.internal.rpc.Service
import cash.z.wallet.sdk.internal.rpc.Service.BlockID
import cash.z.wallet.sdk.internal.rpc.Service.LightdInfo
import cash.z.wallet.sdk.internal.rpc.Service.TreeState
import co.electriccoin.Networking
import co.electriccoin.lightwallet.client.model.BlockHeightUnsafe
import co.electriccoin.lightwallet.client.model.BlockIDUnsafe
import co.electriccoin.lightwallet.client.model.LightWalletEndpointInfoUnsafe
import co.electriccoin.lightwallet.client.model.RawTransactionUnsafe
import co.electriccoin.lightwallet.client.model.Response
import co.electriccoin.lightwallet.client.model.SendResponseUnsafe
import co.electriccoin.lightwallet.client.model.TreeStateUnsafe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * TODO document [Response] use
 */

class TorLwdConn private constructor(
    private var nativeHandle: Long?,
): Networking {
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
    override suspend fun getServerInfo(): Response<LightWalletEndpointInfoUnsafe> =
        accessMutex.withLock {
            withContext(Dispatchers.IO) {
                checkNotNull(nativeHandle) { "TorLwdConn is disposed" }
                runCatching {
                    getServerInfo(nativeHandle!!)
                }.fold(
                    onSuccess = { value ->
                        Response.Success(
                            LightWalletEndpointInfoUnsafe.new(
                                LightdInfo.parseFrom(
                                    value
                                )
                            )
                        )
                    },
                    onFailure = { throwable -> Response.Failure.OverTor(throwable.message) }
                )
            }
        }

    /**
     * Returns information about the latest block in the network.
     */
    override suspend fun getLatestBlock(): Response<BlockIDUnsafe> =
        accessMutex.withLock {
            withContext(Dispatchers.IO) {
                checkNotNull(nativeHandle) { "TorLwdConn is disposed" }
                Response.Success(
                    BlockIDUnsafe.new(
                        BlockID.parseFrom(
                            getLatestBlock(nativeHandle!!)
                        )
                    )
                )
            }
        }

    /**
     * Fetches the transaction with the given ID.
     */
    override suspend fun fetchTransaction(txId: ByteArray): Response<RawTransactionUnsafe> =
        accessMutex.withLock {
            withContext(Dispatchers.IO) {
                checkNotNull(nativeHandle) { "TorLwdConn is disposed" }
                Response.Success(
                    RawTransactionUnsafe.new(
                        Service.RawTransaction.parseFrom(
                            fetchTransaction(nativeHandle!!, txId)
                        )
                    )
                )
            }
        }

    /**
     * Submits a transaction to the Zcash network via the given lightwalletd connection.
     */
    override suspend fun submitTransaction(tx: ByteArray): Response<SendResponseUnsafe> =
        accessMutex.withLock {
            withContext(Dispatchers.IO) {
                checkNotNull(nativeHandle) { "TorLwdConn is disposed" }
                Response.Success(
                    // TODO fix parsing the data
                    SendResponseUnsafe(
                        -1,
                        ""
                        // Service.SendResponse.parseFrom(
                        //    submitTransaction(nativeHandle!!, tx)
                        // )
                    )
                )
            }
        }

    /**
     * Fetches the note commitment tree state corresponding to the given block height.
     */
    override suspend fun getTreeState(height: BlockHeightUnsafe): Response<TreeStateUnsafe> =
        accessMutex.withLock {
            withContext(Dispatchers.IO) {
                checkNotNull(nativeHandle) { "TorLwdConn is disposed" }
                Response.Success(
                    TreeStateUnsafe.new(
                        TreeState.parseFrom(
                            getTreeState(nativeHandle!!, height.value)
                        )
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
