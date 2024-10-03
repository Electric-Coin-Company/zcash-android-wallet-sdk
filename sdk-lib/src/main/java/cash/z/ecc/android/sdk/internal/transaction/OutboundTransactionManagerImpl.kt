package cash.z.ecc.android.sdk.internal.transaction

import cash.z.ecc.android.sdk.internal.Twig
import cash.z.ecc.android.sdk.internal.ext.toHexReversed
import cash.z.ecc.android.sdk.internal.model.EncodedTransaction
import cash.z.ecc.android.sdk.model.Account
import cash.z.ecc.android.sdk.model.Proposal
import cash.z.ecc.android.sdk.model.TransactionRecipient
import cash.z.ecc.android.sdk.model.TransactionSubmitResult
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
        val memoBytes =
            if (memo.isBlank()) {
                null
            } else {
                memo.toByteArray()
            }
        return when (recipient) {
            is TransactionRecipient.Account -> {
                encoder.createShieldingTransaction(
                    usk,
                    recipient,
                    memoBytes
                )
            }
            is TransactionRecipient.Address -> {
                encoder.createTransaction(
                    usk,
                    amount,
                    recipient,
                    memoBytes
                )
            }
        }
    }

    /**
     * Creates a proposal for transferring funds from a ZIP-321 compliant payment URI
     *
     * @param account the account from which to transfer funds.
     * @param uri a ZIP-321 compliant payment URI
     *
     * @return the proposal or an exception
     */
    override suspend fun proposeTransferFromUri(
        account: Account,
        uri: String
    ): Proposal = encoder.proposeTransferFromUri(account, uri)

    override suspend fun proposeTransfer(
        account: Account,
        recipient: String,
        amount: Zatoshi,
        memo: String
    ): Proposal {
        val memoBytes =
            if (memo.isBlank()) {
                null
            } else {
                memo.toByteArray()
            }
        return encoder.proposeTransfer(account, recipient, amount, memoBytes)
    }

    override suspend fun proposeShielding(
        account: Account,
        shieldingThreshold: Zatoshi,
        memo: String,
        transparentReceiver: String?
    ): Proposal? {
        val memoBytes =
            if (memo.isBlank()) {
                null
            } else {
                memo.toByteArray()
            }
        return encoder.proposeShielding(account, shieldingThreshold, memoBytes, transparentReceiver)
    }

    override suspend fun createProposedTransactions(
        proposal: Proposal,
        usk: UnifiedSpendingKey
    ): List<EncodedTransaction> = encoder.createProposedTransactions(proposal, usk)

    override suspend fun submit(encodedTransaction: EncodedTransaction): TransactionSubmitResult {
        return when (val response = service.submitTransaction(encodedTransaction.raw.byteArray)) {
            is Response.Success -> {
                if (response.result.code == 0) {
                    Twig.info {
                        "SUCCESS: submit transaction completed for:" +
                            " ${encodedTransaction.txId.byteArray.toHexReversed()}"
                    }
                    TransactionSubmitResult.Success(encodedTransaction.txId)
                } else {
                    Twig.error {
                        "FAILURE! submit transaction ${encodedTransaction.txId.byteArray.toHexReversed()} " +
                            "completed with response: ${response.result.code}: ${response.result.message}"
                    }
                    TransactionSubmitResult.Failure(
                        encodedTransaction.txId,
                        false,
                        response.result.code,
                        response.result.message
                    )
                }
            }

            is Response.Failure -> {
                Twig.error {
                    "FAILURE! submit transaction failed with gRPC response: ${response.code}: ${
                        response.description
                    }"
                }
                TransactionSubmitResult.Failure(
                    encodedTransaction.txId,
                    true,
                    response.code,
                    response.description
                )
            }
        }
    }

    override suspend fun isValidShieldedAddress(address: String) = encoder.isValidShieldedAddress(address)

    override suspend fun isValidTransparentAddress(address: String) = encoder.isValidTransparentAddress(address)

    override suspend fun isValidUnifiedAddress(address: String) = encoder.isValidUnifiedAddress(address)

    override suspend fun isValidTexAddress(address: String) = encoder.isValidTexAddress(address)

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
