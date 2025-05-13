package cash.z.ecc.android.sdk.internal.model

import cash.z.ecc.android.sdk.internal.jni.RustBackend
import co.electriccoin.lightwallet.client.PartialTorWalletClient
import co.electriccoin.lightwallet.client.model.BlockHeightUnsafe
import co.electriccoin.lightwallet.client.model.Response
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * A tor wallet client that disposes connection after every rpc over tor.
 */
class IsolatedTorWalletClient private constructor(
    private val endpoint: String,
    private val nativeHandle: Long
) : PartialTorWalletClient {

    private val semaphore = Mutex()

    override suspend fun getServerInfo() = executeAndDispose { it.getServerInfo() }

    override suspend fun getLatestBlockHeight() = executeAndDispose { it.getLatestBlockHeight() }

    override suspend fun fetchTransaction(txId: ByteArray) = executeAndDispose { it.fetchTransaction(txId) }

    override suspend fun submitTransaction(tx: ByteArray) = executeAndDispose { it.submitTransaction(tx) }

    override suspend fun getTreeState(height: BlockHeightUnsafe) = executeAndDispose { it.getTreeState(height) }

    private suspend fun <T> executeAndDispose(
        block: suspend (PartialTorWalletClient) -> Response<T>
    ): Response<T> = semaphore.withLock {
        var client: TorWalletClient? = null
        try {
            client = withContext(Dispatchers.IO) {
                TorWalletClient.new(
                    connectToLightwalletd(
                        nativeHandle = nativeHandle,
                        endpoint = endpoint
                    )
                )
            }
            block(client)
        } catch (e: RuntimeException) {
            Response.Failure.OverTor(e.message)
        } finally {
            client?.dispose()
        }
    }

    companion object {

        suspend fun new(endpoint: String, nativeHandle: Long) = withContext(Dispatchers.IO) {
            RustBackend.loadLibrary()
            IsolatedTorWalletClient(endpoint, nativeHandle)
        }

        @JvmStatic
        @Throws(RuntimeException::class)
        private external fun connectToLightwalletd(nativeHandle: Long, endpoint: String): Long
    }
}
