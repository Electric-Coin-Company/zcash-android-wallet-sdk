package co.electriccoin

import co.electriccoin.lightwallet.client.model.BlockHeightUnsafe
import co.electriccoin.lightwallet.client.model.BlockIDUnsafe
import co.electriccoin.lightwallet.client.model.LightWalletEndpointInfoUnsafe
import co.electriccoin.lightwallet.client.model.RawTransactionUnsafe
import co.electriccoin.lightwallet.client.model.Response
import co.electriccoin.lightwallet.client.model.SendResponseUnsafe
import co.electriccoin.lightwallet.client.model.TreeStateUnsafe

interface Networking {
    suspend fun getServerInfo(): Response<LightWalletEndpointInfoUnsafe>
    suspend fun getLatestBlock(): Response<BlockIDUnsafe>
    suspend fun fetchTransaction(txId: ByteArray): Response<RawTransactionUnsafe>
    suspend fun submitTransaction(tx: ByteArray): Response<SendResponseUnsafe>
    suspend fun getTreeState(height: BlockHeightUnsafe): Response<TreeStateUnsafe>
}