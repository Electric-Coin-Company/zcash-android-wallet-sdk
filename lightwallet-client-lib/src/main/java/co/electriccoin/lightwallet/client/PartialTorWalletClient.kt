package co.electriccoin.lightwallet.client

import co.electriccoin.lightwallet.client.model.Response
import co.electriccoin.lightwallet.client.util.Disposable

interface PartialTorWalletClient :
    PartialWalletClient,
    Disposable {
    suspend fun checkSingleUseTransparentAddress(accountUuid: ByteArray): Response<String?>

    suspend fun fetchUtxosByAddress(accountUuid: ByteArray, address: String): Response<String?>
}
