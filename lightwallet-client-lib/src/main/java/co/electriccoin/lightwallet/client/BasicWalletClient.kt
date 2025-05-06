package co.electriccoin.lightwallet.client

import co.electriccoin.lightwallet.client.model.BlockHeightUnsafe
import co.electriccoin.lightwallet.client.model.LightWalletEndpointInfoUnsafe
import co.electriccoin.lightwallet.client.model.RawTransactionUnsafe
import co.electriccoin.lightwallet.client.model.Response
import co.electriccoin.lightwallet.client.model.SendResponseUnsafe
import co.electriccoin.lightwallet.client.model.TreeStateUnsafe

interface BasicWalletClient {

    /**
     * @return useful server details.
     */
    suspend fun getServerInfo(): Response<LightWalletEndpointInfoUnsafe>

    /**
     * @return the latest block height known to the service.
     */
    suspend fun getLatestBlockHeight(): Response<BlockHeightUnsafe>

    /**
     * @return the full transaction info.
     */
    suspend fun fetchTransaction(txId: ByteArray): Response<RawTransactionUnsafe>

    /**
     * Submit a raw transaction.
     *
     * @return the response from the server.
     */
    suspend fun submitTransaction(tx: ByteArray): Response<SendResponseUnsafe>

    /**
     * @return the latest block height known to the service.
     */
    suspend fun getTreeState(height: BlockHeightUnsafe): Response<TreeStateUnsafe>
}