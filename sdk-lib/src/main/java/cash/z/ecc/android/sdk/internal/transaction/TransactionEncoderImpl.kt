package cash.z.ecc.android.sdk.internal.transaction

import cash.z.ecc.android.sdk.exception.SdkException
import cash.z.ecc.android.sdk.exception.TransactionEncoderException
import cash.z.ecc.android.sdk.ext.masked
import cash.z.ecc.android.sdk.internal.SaplingParamTool
import cash.z.ecc.android.sdk.internal.Twig
import cash.z.ecc.android.sdk.internal.TypesafeBackend
import cash.z.ecc.android.sdk.internal.model.EncodedTransaction
import cash.z.ecc.android.sdk.internal.repository.DerivedDataRepository
import cash.z.ecc.android.sdk.model.Account
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.FirstClassByteArray
import cash.z.ecc.android.sdk.model.Proposal
import cash.z.ecc.android.sdk.model.TransactionRecipient
import cash.z.ecc.android.sdk.model.UnifiedSpendingKey
import cash.z.ecc.android.sdk.model.Zatoshi

/**
 * Class responsible for encoding a transaction in a consistent way. This bridges the gap by
 * behaving like a stateless API so that callers can request [createTransaction] and receive a
 * result, even though there are intermediate database interactions.
 *
 * @property backend the instance of RustBackendWelding to use for creating and validating.
 * @property repository the repository that stores information about the transactions being created
 * such as the raw bytes and raw txId.
 */
@Suppress("TooManyFunctions")
internal class TransactionEncoderImpl(
    private val backend: TypesafeBackend,
    private val saplingParamTool: SaplingParamTool,
    private val repository: DerivedDataRepository
) : TransactionEncoder {
    /**
     * Creates a transaction, throwing an exception whenever things are missing. When the provided
     * wallet implementation doesn't throw an exception, we wrap the issue into a descriptive
     * exception ourselves (rather than using double-bangs for things).
     *
     * @param usk the unified spending key associated with the notes that will be spent.
     * @param amount the amount of zatoshi to send.
     * @param recipient the recipient's address.
     * @param memo the optional memo to include as part of the transaction.
     *
     * @return the successfully encoded transaction or an exception
     *
     * @throws TransactionEncoderException.TransactionNotFoundException in case the encoded transaction not found
     */
    override suspend fun createTransaction(
        usk: UnifiedSpendingKey,
        account: Account,
        amount: Zatoshi,
        recipient: TransactionRecipient,
        memo: ByteArray?
    ): EncodedTransaction {
        require(recipient is TransactionRecipient.RecipientAddress)

        val transactionId =
            createSpend(
                account = account,
                amount = amount,
                memo = memo,
                toAddress = recipient.addressValue,
                usk = usk,
            )
        return repository.findEncodedTransactionByTxId(transactionId)
            ?: throw TransactionEncoderException.TransactionNotFoundException(transactionId)
    }

    override suspend fun createShieldingTransaction(
        usk: UnifiedSpendingKey,
        account: Account,
        recipient: TransactionRecipient,
        memo: ByteArray?
    ): EncodedTransaction {
        require(recipient is TransactionRecipient.RecipientAccount)

        val transactionId =
            createShieldingSpend(
                account = account,
                memo = memo,
                usk = usk,
            )
        return repository.findEncodedTransactionByTxId(transactionId)
            ?: throw TransactionEncoderException.TransactionNotFoundException(transactionId)
    }

    /**
     * Creates a proposal for transferring from a valid ZIP-321 Payment URI string
     *
     * @param account the account from which to transfer funds.
     * @param uri a valid ZIP-321 Payment URI string
     *
     * @return the proposal or an exception
     *
     * @throws TransactionEncoderException.ProposalFromUriException
     */
    @Throws(TransactionEncoderException.ProposalFromUriException::class)
    override suspend fun proposeTransferFromUri(
        account: Account,
        uri: String
    ): Proposal {
        Twig.debug {
            "creating proposal from URI: $uri"
        }

        return runCatching {
            backend.proposeTransferFromUri(
                account,
                uri
            )
        }.onSuccess {
            Twig.info { "Result of proposeTransferFromUri: ${it.toPrettyString()}" }
        }.onFailure {
            Twig.error(it) { "Caught exception while creating proposal from URI String." }
        }.getOrElse {
            throw TransactionEncoderException.ProposalFromUriException(it)
        }
    }

    @Throws(TransactionEncoderException.ProposalFromParametersException::class)
    override suspend fun proposeTransfer(
        account: Account,
        recipient: String,
        amount: Zatoshi,
        memo: ByteArray?
    ): Proposal {
        Twig.debug {
            "creating proposal to spend $amount zatoshi to" +
                " ${recipient.masked()} with memo: ${memo?.decodeToString()}"
        }

        return runCatching {
            backend.proposeTransfer(
                account,
                recipient,
                amount.value,
                memo
            )
        }.onSuccess {
            Twig.info { "Result of proposeTransfer: ${it.toPrettyString()}" }
        }.onFailure {
            Twig.error(it) { "Caught exception while creating proposal." }
        }.getOrElse {
            throw TransactionEncoderException.ProposalFromParametersException(it)
        }
    }

    @Throws(TransactionEncoderException.ProposalShieldingException::class)
    override suspend fun proposeShielding(
        account: Account,
        shieldingThreshold: Zatoshi,
        memo: ByteArray?,
        transparentReceiver: String?
    ): Proposal? {
        return runCatching {
            backend.proposeShielding(account, shieldingThreshold.value, memo, transparentReceiver)
        }.onFailure {
            // TODO [#680]: if this error matches: Insufficient balance (have 0, need 1000 including fee)
            //  then consider custom error that says no UTXOs existed to shield
            // TODO [#680]: https://github.com/zcash/zcash-android-wallet-sdk/issues/680
            Twig.error(it) { "proposeShielding failed" }
        }.onSuccess { result ->
            Twig.info { "Result of proposeShielding: ${result?.toPrettyString()}" }
        }.getOrElse {
            throw TransactionEncoderException.ProposalShieldingException(it)
        }
    }

    @Throws(
        TransactionEncoderException.TransactionNotCreatedException::class,
        TransactionEncoderException.TransactionNotFoundException::class,
    )
    override suspend fun createProposedTransactions(
        proposal: Proposal,
        usk: UnifiedSpendingKey
    ): List<EncodedTransaction> {
        Twig.debug {
            "creating transactions for proposal"
        }

        val transactionIds =
            runCatching {
                saplingParamTool.ensureParams(saplingParamTool.properties.paramsDirectory)
                Twig.debug { "params exist! attempting to send..." }
                backend.createProposedTransactions(proposal, usk)
            }.onFailure {
                Twig.error(it) { "Caught exception while creating transaction." }
            }.onSuccess { result ->
                Twig.info { "Result of createProposedTransactions: $result" }
            }.getOrElse {
                throw TransactionEncoderException.TransactionNotCreatedException(it)
            }

        val txs =
            transactionIds.map { transactionId ->
                repository.findEncodedTransactionByTxId(transactionId)
                    ?: throw TransactionEncoderException.TransactionNotFoundException(transactionId)
            }

        return txs
    }

    /**
     * Utility function to help with validation. This is not called during [createTransaction]
     * because this class asserts that all validation is done externally by the UI, for now.
     *
     * @param address the address to validate
     *
     * @return true when the given address is a valid z-addr
     */
    override suspend fun isValidShieldedAddress(address: String): Boolean = backend.isValidSaplingAddr(address)

    /**
     * Utility function to help with validation. This is not called during [createTransaction]
     * because this class asserts that all validation is done externally by the UI, for now.
     *
     * @param address the address to validate
     *
     * @return true when the given address is a valid t-addr
     */
    override suspend fun isValidTransparentAddress(address: String): Boolean = backend.isValidTransparentAddr(address)

    /**
     * Utility function to help with validation. This is not called during [createTransaction]
     * because this class asserts that all validation is done externally by the UI, for now.
     *
     * @param address the address to validate
     *
     * @return true when the given address is a valid ZIP 316 Unified Address
     */
    override suspend fun isValidUnifiedAddress(address: String): Boolean = backend.isValidUnifiedAddr(address)

    /**
     * Utility function to help with validation. This is not called during [createTransaction]
     * because this class asserts that all validation is done externally by the UI, for now.
     *
     * @param address the address to validate
     *
     * @return true when the given address is a valid ZIP 320 TEX address
     */
    override suspend fun isValidTexAddress(address: String): Boolean = backend.isValidTexAddr(address)

    /**
     * Return the consensus branch that the encoder is using when making transactions.
     *
     * @param height the height at which we want to get the consensus branch
     *
     * @return id of consensus branch
     *
     * @throws TransactionEncoderException.IncompleteScanException if the [height] is less than activation height
     */
    override suspend fun getConsensusBranchId(height: BlockHeight): Long {
        if (height < backend.network.saplingActivationHeight) {
            throw TransactionEncoderException.IncompleteScanException(height)
        }
        return backend.getBranchIdForHeight(height)
    }

    /**
     * Does the proofs and processing required to create a transaction to spend funds and inserts
     * the result in the database. On average, this call takes over 10 seconds.
     *
     * @param usk the unified spending key associated with the notes that will be spent.
     * @param amount the amount of zatoshi to send.
     * @param toAddress the recipient's address.
     * @param memo the optional memo to include as part of the transaction.
     *
     * @return the row id in the transactions table that contains the spend transaction or -1 if it
     * failed.
     */
    private suspend fun createSpend(
        account: Account,
        amount: Zatoshi,
        memo: ByteArray? = null,
        toAddress: String,
        usk: UnifiedSpendingKey,
    ): FirstClassByteArray {
        Twig.debug {
            "creating transaction to spend $amount zatoshi to" +
                " ${toAddress.masked()} with memo: ${memo?.decodeToString()}"
        }

        return runCatching {
            saplingParamTool.ensureParams(saplingParamTool.properties.paramsDirectory)
            Twig.debug { "params exist! attempting to send..." }
            val proposal =
                backend.proposeTransfer(
                    account,
                    toAddress,
                    amount.value,
                    memo
                )
            val transactionIds = backend.createProposedTransactions(proposal, usk)
            assert(transactionIds.size == 1)
            transactionIds[0]
        }.onFailure {
            Twig.error(it) { "Caught exception while creating transaction." }
        }.onSuccess { result ->
            Twig.debug { "result of sendToAddress: $result" }
        }.getOrThrow()
    }

    private suspend fun createShieldingSpend(
        account: Account,
        memo: ByteArray? = null,
        usk: UnifiedSpendingKey,
    ): FirstClassByteArray {
        return runCatching {
            saplingParamTool.ensureParams(saplingParamTool.properties.paramsDirectory)
            Twig.debug { "params exist! attempting to shield..." }
            val proposal =
                backend.proposeShielding(account, SHIELDING_THRESHOLD, memo)
                    ?: throw SdkException(
                        "Insufficient balance (have 0, need $SHIELDING_THRESHOLD including fee)",
                        null
                    )
            val transactionIds = backend.createProposedTransactions(proposal, usk)
            assert(transactionIds.size == 1)
            transactionIds[0]
        }.onFailure {
            // TODO [#680]: if this error matches: Insufficient balance (have 0, need 1000 including fee)
            //  then consider custom error that says no UTXOs existed to shield
            // TODO [#680]: https://github.com/zcash/zcash-android-wallet-sdk/issues/680
            Twig.error(it) { "Shield failed" }
        }.onSuccess { result ->
            Twig.debug { "result of shieldToAddress: $result" }
        }.getOrThrow()
    }

    companion object {
        private const val SHIELDING_THRESHOLD = 100000L
    }
}
