package cash.z.ecc.android.sdk.internal.transaction

import cash.z.ecc.android.sdk.db.entity.EncodedTransaction
import cash.z.ecc.android.sdk.exception.TransactionEncoderException
import cash.z.ecc.android.sdk.ext.masked
import cash.z.ecc.android.sdk.internal.SaplingParamTool
import cash.z.ecc.android.sdk.internal.twig
import cash.z.ecc.android.sdk.internal.twigTask
import cash.z.ecc.android.sdk.jni.RustBackendWelding
import cash.z.ecc.android.sdk.model.UnifiedSpendingKey
import cash.z.ecc.android.sdk.model.Zatoshi

/**
 * Class responsible for encoding a transaction in a consistent way. This bridges the gap by
 * behaving like a stateless API so that callers can request [createTransaction] and receive a
 * result, even though there are intermediate database interactions.
 *
 * @property rustBackend the instance of RustBackendWelding to use for creating and validating.
 * @property repository the repository that stores information about the transactions being created
 * such as the raw bytes and raw txId.
 */
internal class WalletTransactionEncoder(
    private val rustBackend: RustBackendWelding,
    private val saplingParamTool: SaplingParamTool,
    private val repository: TransactionRepository
) : TransactionEncoder {

    /**
     * Creates a transaction, throwing an exception whenever things are missing. When the provided
     * wallet implementation doesn't throw an exception, we wrap the issue into a descriptive
     * exception ourselves (rather than using double-bangs for things).
     *
     * @param usk the unified spending key associated with the notes that will be spent.
     * @param amount the amount of zatoshi to send.
     * @param toAddress the recipient's address.
     * @param memo the optional memo to include as part of the transaction.
     *
     * @return the successfully encoded transaction or an exception
     */
    override suspend fun createTransaction(
        usk: UnifiedSpendingKey,
        amount: Zatoshi,
        toAddress: String,
        memo: ByteArray?
    ): EncodedTransaction {
        val transactionId = createSpend(usk, amount, toAddress, memo)
        return repository.findEncodedTransactionById(transactionId)
            ?: throw TransactionEncoderException.TransactionNotFoundException(transactionId)
    }

    override suspend fun createShieldingTransaction(
        usk: UnifiedSpendingKey,
        memo: ByteArray?
    ): EncodedTransaction {
        val transactionId = createShieldingSpend(usk, memo)
        return repository.findEncodedTransactionById(transactionId)
            ?: throw TransactionEncoderException.TransactionNotFoundException(transactionId)
    }

    /**
     * Utility function to help with validation. This is not called during [createTransaction]
     * because this class asserts that all validation is done externally by the UI, for now.
     *
     * @param address the address to validate
     *
     * @return true when the given address is a valid z-addr
     */
    override suspend fun isValidShieldedAddress(address: String): Boolean =
        rustBackend.isValidShieldedAddr(address)

    /**
     * Utility function to help with validation. This is not called during [createTransaction]
     * because this class asserts that all validation is done externally by the UI, for now.
     *
     * @param address the address to validate
     *
     * @return true when the given address is a valid t-addr
     */
    override suspend fun isValidTransparentAddress(address: String): Boolean =
        rustBackend.isValidTransparentAddr(address)

    /**
     * Utility function to help with validation. This is not called during [createTransaction]
     * because this class asserts that all validation is done externally by the UI, for now.
     *
     * @param address the address to validate
     *
     * @return true when the given address is a valid ZIP 316 Unified Address
     */
    override suspend fun isValidUnifiedAddress(address: String): Boolean =
        rustBackend.isValidUnifiedAddr(address)

    override suspend fun getConsensusBranchId(): Long {
        val height = repository.lastScannedHeight()
        if (height < rustBackend.network.saplingActivationHeight) {
            throw TransactionEncoderException.IncompleteScanException(height)
        }
        return rustBackend.getBranchIdForHeight(height)
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
    ): Long {
        return twigTask(
            "creating transaction to spend $amount zatoshi to" +
                " ${toAddress.masked()} with memo $memo"
        ) {
            @Suppress("TooGenericExceptionCaught")
            try {
                saplingParamTool.ensureParams(rustBackend.saplingParamDir)
                twig("params exist! attempting to send...")
                rustBackend.createToAddress(
                    usk,
                    toAddress,
                    amount.value,
                    memo
                )
            } catch (t: Throwable) {
                twig("Caught exception while creating transaction ${t.message}, caused by: ${t.cause}.")
                throw t
            }
        }.also { result ->
            twig("result of sendToAddress: $result")
        }
    }

    private suspend fun createShieldingSpend(
        usk: UnifiedSpendingKey,
        memo: ByteArray? = byteArrayOf()
    ): Long {
        return twigTask("creating transaction to shield all UTXOs") {
            @Suppress("TooGenericExceptionCaught")
            try {
                saplingParamTool.ensureParams(rustBackend.saplingParamDir)
                twig("params exist! attempting to shield...")
                rustBackend.shieldToAddress(
                    usk,
                    memo
                )
            } catch (t: Throwable) {
                // TODO [#680]: if this error matches: Insufficient balance (have 0, need 1000 including fee)
                //  then consider custom error that says no UTXOs existed to shield
                // TODO [#680]: https://github.com/zcash/zcash-android-wallet-sdk/issues/680
                twig("Shield failed due to: ${t.message}, caused by: ${t.cause}.")
                throw t
            }
        }.also { result ->
            twig("result of shieldToAddress: $result")
        }
    }
}
