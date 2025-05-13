package cash.z.ecc.android.sdk.internal.model

import cash.z.ecc.android.sdk.internal.jni.RustBackend
import cash.z.wallet.sdk.internal.rpc.Service
import co.electriccoin.lightwallet.client.PartialTorWalletClient
import co.electriccoin.lightwallet.client.model.BlockHeightUnsafe
import co.electriccoin.lightwallet.client.model.BlockIDUnsafe
import co.electriccoin.lightwallet.client.model.LightWalletEndpointInfoUnsafe
import co.electriccoin.lightwallet.client.model.RawTransactionUnsafe
import co.electriccoin.lightwallet.client.model.Response
import co.electriccoin.lightwallet.client.model.SendResponseUnsafe
import co.electriccoin.lightwallet.client.model.TreeStateUnsafe
import com.google.protobuf.kotlin.toByteString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TorWalletClient private constructor(
    private var nativeHandle: Long?,
) : PartialTorWalletClient {

    suspend fun dispose() {
        execute {
            nativeHandle?.let { freeLightwalletdConnection(it) }
            nativeHandle = null
        }
    }

    override suspend fun getServerInfo(): Response<LightWalletEndpointInfoUnsafe> = execute {
        val serverInfo = getServerInfo(it)
        LightWalletEndpointInfoUnsafe.new(Service.LightdInfo.parseFrom(serverInfo))
    }

    override suspend fun getLatestBlockHeight(): Response<BlockHeightUnsafe> = execute {
        val latestBlock = getLatestBlock(it)
        val blockId = BlockIDUnsafe.new(Service.BlockID.parseFrom(latestBlock))
        BlockHeightUnsafe(blockId.height)
    }

    override suspend fun fetchTransaction(txId: ByteArray): Response<RawTransactionUnsafe> = execute {
        val transaction = fetchTransaction(it, txId)
        RawTransactionUnsafe.new(
            Service.RawTransaction.newBuilder()
                .setData(transaction.data.toByteString())
                .setHeight(transaction.height)
                .build()
        )
    }

    override suspend fun submitTransaction(tx: ByteArray): Response<SendResponseUnsafe> = execute {
        submitTransaction(it, tx)
        SendResponseUnsafe(-1, "")
    }

    override suspend fun getTreeState(height: BlockHeightUnsafe): Response<TreeStateUnsafe> = execute {
        val treeState = getTreeState(it, height.value)
        TreeStateUnsafe.new(Service.TreeState.parseFrom(treeState))
    }

    private suspend fun <T> execute(
        block: (handle: Long) -> T
    ) = withContext(Dispatchers.IO) {
        val nativeHandle = nativeHandle
        checkNotNull(nativeHandle) { "TorLwdConn is disposed" }
        try {
            Response.Success(block(nativeHandle))
        } catch (e: RuntimeException) {
            Response.Failure.OverTor(e.message)
        } catch (e: Exception) {
            Response.Failure.OverTor(e.message)
        }
    }

    companion object {

        suspend fun new(nativeHandle: Long): TorWalletClient = withContext(Dispatchers.IO) {
            RustBackend.loadLibrary()
            TorWalletClient(nativeHandle)
        }

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
        private external fun fetchTransaction(nativeHandle: Long, txId: ByteArray): JniTransaction

        /**
         * @throws RuntimeException as a common indicator of the operation failure
         */
        @JvmStatic
        @Throws(RuntimeException::class)
        private external fun submitTransaction(nativeHandle: Long, tx: ByteArray)

        /**
         * @throws RuntimeException as a common indicator of the operation failure
         */
        @JvmStatic
        @Throws(RuntimeException::class)
        private external fun getTreeState(nativeHandle: Long, fromHeight: Long): ByteArray
    }
}