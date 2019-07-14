package cash.z.wallet.sdk.data

import cash.z.wallet.sdk.service.LightWalletService

/**
 * Manage transactions with the main purpose of reporting which ones are still pending, particularly after failed
 * attempts or dropped connectivity. The intent is to help see transactions through to completion.
 */
interface TransactionManager {
    fun start()
    fun stop()
    suspend fun manageCreation(encoder: TransactionEncoder, zatoshiValue: Long, toAddress: String, memo: String, currentHeight: Int): SignedTransaction
    suspend fun manageSubmission(service: LightWalletService, pendingTransaction: SignedTransaction)
    suspend fun getAll(): List<SignedTransaction>
}
interface SignedTransaction {
    val raw: ByteArray
}

interface TransactionError {
    val message: String
}