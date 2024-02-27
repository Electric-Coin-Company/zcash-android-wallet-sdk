package cash.z.ecc.android.sdk.internal.transaction

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
     */
    override suspend fun createTransaction(
        usk: UnifiedSpendingKey,
        amount: Zatoshi,
        recipient: TransactionRecipient,
        memo: ByteArray?
    ): EncodedTransaction {
        require(recipient is TransactionRecipient.Address)

        val transactionId = createSpend(usk, amount, recipient.addressValue, memo)
        return repository.findEncodedTransactionByTxId(transactionId)
            ?: throw TransactionEncoderException.TransactionNotFoundException(transactionId)
    }

    override suspend fun createShieldingTransaction(
        usk: UnifiedSpendingKey,
        recipient: TransactionRecipient,
        memo: ByteArray?
    ): EncodedTransaction {
        require(recipient is TransactionRecipient.Account)

        val transactionId = createShieldingSpend(usk, memo)
        return repository.findEncodedTransactionByTxId(transactionId)
            ?: throw TransactionEncoderException.TransactionNotFoundException(transactionId)
    }

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

        @Suppress("TooGenericExceptionCaught")
        return try {
            backend.proposeTransfer(
                account,
                recipient,
                amount.value,
                memo
            )
        } catch (t: Throwable) {
            Twig.debug(t) { "Caught exception while creating proposal." }
            throw t
        }.also { result ->
            Twig.debug { "result of proposeTransfer: $result" }
        }
    }

    override suspend fun proposeShielding(
        account: Account,
        shieldingThreshold: Zatoshi,
        memo: ByteArray?
    ): Proposal {
        @Suppress("TooGenericExceptionCaught")
        return try {
            backend.proposeShielding(account, shieldingThreshold.value, memo)
        } catch (t: Throwable) {
            // TODO [#680]: if this error matches: Insufficient balance (have 0, need 1000 including fee)
            //  then consider custom error that says no UTXOs existed to shield
            // TODO [#680]: https://github.com/zcash/zcash-android-wallet-sdk/issues/680
            Twig.debug(t) { "proposeShielding failed" }
            throw t
        }.also { result ->
            Twig.debug { "result of proposeShielding: $result" }
        }
    }

    override suspend fun createProposedTransactions(
        proposal: Proposal,
        usk: UnifiedSpendingKey
    ): List<EncodedTransaction> {
        Twig.debug {
            "creating transactions for proposal"
        }

        @Suppress("TooGenericExceptionCaught")
        val transactionId =
            try {
                saplingParamTool.ensureParams(saplingParamTool.properties.paramsDirectory)
                Twig.debug { "params exist! attempting to send..." }
                backend.createProposedTransaction(proposal, usk)
            } catch (t: Throwable) {
                Twig.debug(t) { "Caught exception while creating transaction." }
                throw t
            }.also { result ->
                Twig.debug { "result of createProposedTransactions: $result" }
            }

        val tx =
            repository.findEncodedTransactionByTxId(transactionId)
                ?: throw TransactionEncoderException.TransactionNotFoundException(transactionId)

        return listOf(tx)
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
        usk: UnifiedSpendingKey,
        amount: Zatoshi,
        toAddress: String,
        memo: ByteArray? = byteArrayOf()
    ): FirstClassByteArray {
        Twig.debug {
            "creating transaction to spend $amount zatoshi to" +
                " ${toAddress.masked()} with memo: ${memo?.decodeToString()}"
        }

        @Suppress("TooGenericExceptionCaught")
        return try {
            saplingParamTool.ensureParams(saplingParamTool.properties.paramsDirectory)
            Twig.debug { "params exist! attempting to send..." }
            val proposal =
                backend.proposeTransfer(
                    usk.account,
                    toAddress,
                    amount.value,
                    memo
                )
            backend.createProposedTransaction(proposal, usk)
        } catch (t: Throwable) {
            Twig.debug(t) { "Caught exception while creating transaction." }
            throw t
        }.also { result ->
            Twig.debug { "result of sendToAddress: $result" }
        }
    }

    @Suppress("MagicNumber")
    private suspend fun createShieldingSpend(
        usk: UnifiedSpendingKey,
        memo: ByteArray? = byteArrayOf()
    ): FirstClassByteArray {
        @Suppress("TooGenericExceptionCaught")
        return try {
            saplingParamTool.ensureParams(saplingParamTool.properties.paramsDirectory)
            Twig.debug { "params exist! attempting to shield..." }
            val proposal = backend.proposeShielding(usk.account, 100000, memo)
            backend.createProposedTransaction(proposal, usk)
        } catch (t: Throwable) {
            // TODO [#680]: if this error matches: Insufficient balance (have 0, need 1000 including fee)
            //  then consider custom error that says no UTXOs existed to shield
            // TODO [#680]: https://github.com/zcash/zcash-android-wallet-sdk/issues/680
            Twig.debug(t) { "Shield failed" }
            throw t
        }.also { result ->
            Twig.debug { "result of shieldToAddress: $result" }
        }
    }
}
