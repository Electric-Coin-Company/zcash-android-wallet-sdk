package cash.z.ecc.android.sdk.internal.transaction

import cash.z.ecc.android.sdk.internal.Twig
import cash.z.ecc.android.sdk.internal.model.EncodedTransaction
import cash.z.ecc.android.sdk.model.Account
import cash.z.ecc.android.sdk.model.TransactionRecipient
import cash.z.ecc.android.sdk.model.UnifiedSpendingKey
import cash.z.ecc.android.sdk.model.Zatoshi
import co.electriccoin.lightwallet.client.LightWalletClient
import co.electriccoin.lightwallet.client.model.Response

@Suppress("TooManyFunctions")
internal class OutboundTransactionManagerImpl(
    internal val encoder: TransactionEncoder,
    private val service: LightWalletClient
) : OutboundTransactionManager {
    override suspend fun encode(
        usk: UnifiedSpendingKey,
        amount: Zatoshi,
        recipient: TransactionRecipient,
        memo: String,
        account: Account
    ): EncodedTransaction {
        return when (recipient) {
            is TransactionRecipient.Account -> {
                encoder.createShieldingTransaction(
                    usk,
                    recipient,
                    memo.toByteArray()
                )
            }
            is TransactionRecipient.Address -> {
                encoder.createTransaction(
                    usk,
                    amount,
                    recipient,
                    memo.toByteArray()
                )
            }
        }
    }

    override suspend fun submit(encodedTransaction: EncodedTransaction): Boolean {
        return when (val response = service.submitTransaction(encodedTransaction.raw.byteArray)) {
            is Response.Success -> {
                Twig.debug { "SUCCESS: submit transaction completed with response: ${response.result}" }
                true
            }

            is Response.Failure -> {
                Twig.debug {
                    "FAILURE! submit transaction completed with response: ${response.code}: ${
                        response.description
                    }"
                }
                false
            }
        }
    }

    override suspend fun isValidShieldedAddress(address: String) = encoder.isValidShieldedAddress(address)

    override suspend fun isValidTransparentAddress(address: String) = encoder.isValidTransparentAddress(address)

    override suspend fun isValidUnifiedAddress(address: String) = encoder.isValidUnifiedAddress(address)

    //
    // Helper functions
    //

    companion object {
        fun new(
            encoder: TransactionEncoder,
            lightWalletClient: LightWalletClient,
        ): OutboundTransactionManager =
            OutboundTransactionManagerImpl(
                encoder,
                lightWalletClient
            )
    }
}
