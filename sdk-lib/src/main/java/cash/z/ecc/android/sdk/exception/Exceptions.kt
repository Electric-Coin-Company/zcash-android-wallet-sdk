@file:Suppress("UnusedPrivateMember")

package cash.z.ecc.android.sdk.exception

import cash.z.ecc.android.sdk.internal.SaplingParameters
import cash.z.ecc.android.sdk.internal.db.DatabaseCoordinator
import cash.z.ecc.android.sdk.internal.model.Checkpoint
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.FirstClassByteArray
import cash.z.ecc.android.sdk.model.ZcashNetwork
import co.electriccoin.lightwallet.client.model.BlockHeightUnsafe

/**
 * Marker for all custom exceptions from the SDK. Making it an interface would result in more typing
 * so it's a supertype, instead.
 */
open class SdkException(message: String, cause: Throwable?) : RuntimeException(message, cause)

/**
 * Exceptions thrown in the Rust layer of the SDK. We may not always be able to surface details about this
 * exception so it's important for the SDK to provide helpful messages whenever these errors are encountered.
 */
sealed class RustLayerException(message: String, cause: Throwable? = null) : SdkException(message, cause) {
    class BalanceException(cause: Throwable) : RustLayerException(
        "Error while requesting the current balance over " +
            "JNI. This might mean that the database has been corrupted and needs to be rebuilt. Verify that " +
            "blocks are not missing or have not been scanned out of order.",
        cause
    )
}

/**
 * Potentially user-facing exceptions that occur while processing compact blocks.
 */
sealed class CompactBlockProcessorException(message: String, cause: Throwable? = null) : SdkException(message, cause) {
    class DataDbMissing(path: String) : CompactBlockProcessorException(
        "No data db file found at path $path. Verify " +
            "that the data DB has been initialized via `rustBackend.initDataDb(path)`"
    )

    open class ConfigurationException(
        message: String,
        cause: Throwable?
    ) : CompactBlockProcessorException(message, cause)

    class FileInsteadOfPath(fileName: String) : ConfigurationException(
        "Invalid Path: the given path appears to be a" +
            " file name instead of a path: $fileName. The RustBackend expects the absolutePath to the database rather" +
            " than just the database filename because Rust does not access the app Context." +
            " So pass in context.getDatabasePath(dbFileName).absolutePath instead of just dbFileName alone.",
        null
    )

    class FailedReorgRepair(message: String) : CompactBlockProcessorException(message)

    data object NoAccount : CompactBlockProcessorException(
        "Attempting to scan without an account. This is probably a setup error or a race condition."
    ) {
        private fun readResolve(): Any = NoAccount
    }

    class FailedSynchronizationException(message: String, cause: Throwable) : CompactBlockProcessorException(
        "Common error while running the block synchronization. This is typically caused by a failed underlying " +
            "synchronization operation. See failure description: $message OR the root cause: $cause"
    )

    class FailedDownloadException(cause: Throwable? = null) : CompactBlockProcessorException(
        "Error while downloading blocks. This most likely means the server is down or slow to respond. " +
            "See logs for details.",
        cause
    )

    class FailedScanException(cause: Throwable? = null) : CompactBlockProcessorException(
        "Error while scanning blocks. This most likely means a problem with locally persisted data. " +
            "See logs for details.",
        cause
    )

    class FailedDeleteException(cause: Throwable? = null) : CompactBlockProcessorException(
        "Error while deleting block files. This most likely means the data are not persisted correctly." +
            " See logs for details.",
        cause
    )

    open class EnhanceTransactionError(
        message: String,
        val height: BlockHeight,
        cause: Throwable
    ) : CompactBlockProcessorException(message, cause) {
        class EnhanceTxDownloadError(
            height: BlockHeight,
            cause: Throwable
        ) : EnhanceTransactionError(
                "Error while attempting to download a transaction to enhance",
                height,
                cause
            )

        class EnhanceTxDecryptError(
            height: BlockHeight,
            cause: Throwable
        ) : EnhanceTransactionError(
                "Error while attempting to decrypt and store a transaction to enhance",
                height,
                cause
            )
    }

    class MismatchedNetwork(clientNetwork: String?, serverNetwork: String?) : CompactBlockProcessorException(
        "Incompatible server: this client expects a server using $clientNetwork but it was $serverNetwork! Try " +
            "updating the client or switching servers."
    )

    class MismatchedConsensusBranch(clientBranchId: String, serverBranchId: String) : CompactBlockProcessorException(
        message =
            "Incompatible server: this client expects a consensus branch $clientBranchId but it " +
                "was $serverBranchId! Try updating the client or switching servers."
    )

    class MismatchedSaplingActivationHeight(
        clientHeight: Long,
        serverHeight: Long
    ) : CompactBlockProcessorException(
            message =
                "Incompatible server: this client expects a sapling activation height $clientHeight but it " +
                    "was $serverHeight! Try updating the client or switching servers."
        )

    class BadBlockHeight(serverBlockHeight: BlockHeightUnsafe) : CompactBlockProcessorException(
        "The server returned a block height of $serverBlockHeight which is not valid."
    )
}

/**
 * Exceptions related to the wallet's birthday.
 */
sealed class BirthdayException(message: String, cause: Throwable? = null) : SdkException(message, cause) {
    class MissingBirthdayFilesException(directory: String) : BirthdayException(
        "Cannot initialize wallet because no birthday files were found in the $directory directory."
    )

    class ExactBirthdayNotFoundException internal constructor(
        birthday: BlockHeight,
        nearestMatch: Checkpoint? = null
    ) : BirthdayException(
            "Unable to find birthday that exactly matches $birthday.${
                if (nearestMatch != null) {
                    " An exact match was request but the nearest match found was ${nearestMatch.height}."
                } else {
                    ""
                }
            }"
        )

    class BirthdayFileNotFoundException(directory: String, height: BlockHeight?) : BirthdayException(
        "Unable to find birthday file for $height verify that $directory/$height.json exists."
    )

    class MalformattedBirthdayFilesException(directory: String, file: String, cause: Throwable?) : BirthdayException(
        "Failed to parse file $directory/$file verify that it is formatted as #####.json, " +
            "where the first portion is an Int representing the height of the tree contained in the file",
        cause
    )
}

/**
 * Exceptions thrown by the initializer.
 */
sealed class InitializeException(message: String, cause: Throwable? = null) : SdkException(message, cause) {
    data object SeedRequired : InitializeException(
        "A pending database migration requires the wallet's seed. Call this initialization " +
            "method again with the seed."
    ) {
        private fun readResolve(): Any = SeedRequired
    }

    data object SeedNotRelevant : InitializeException(
        "The provided seed is not relevant to any of the derived accounts in the wallet database."
    ) {
        private fun readResolve(): Any = SeedNotRelevant
    }

    class FalseStart(cause: Throwable?) : InitializeException("Failed to initialize accounts due to: $cause", cause)

    class AlreadyInitializedException(cause: Throwable, dbPath: String) : InitializeException(
        "Failed to initialize the blocks table" +
            " because it already exists in $dbPath",
        cause
    )

    data object MissingBirthdayException : InitializeException(
        "Expected a birthday for this wallet but failed to find one. This usually means that " +
            "wallet setup did not happen correctly. A workaround might be to interpret the " +
            "birthday,  based on the contents of the wallet data but it is probably better " +
            "not to mask this error because the root issue should be addressed."
    ) {
        private fun readResolve(): Any = MissingBirthdayException
    }

    object MissingViewingKeyException : InitializeException(
        "Expected a unified viewingKey for this wallet but failed to find one. This usually means" +
            " that wallet setup happened incorrectly. A workaround might be to derive the" +
            " unified viewingKey from the seed or seedPhrase, if they exist, but it is probably" +
            " better not to mask this error because the root issue should be addressed."
    ) {
        private fun readResolve(): Any = MissingViewingKeyException
    }

    class MissingAddressException(description: String, cause: Throwable? = null) : InitializeException(
        "Expected a $description address for this wallet but failed to find one. This usually" +
            " means that wallet setup happened incorrectly. If this problem persists, a" +
            " workaround might be to go to settings and WIPE the wallet and rescan. Doing so" +
            " will restore any missing address information. Meanwhile, please report that" +
            " this happened so that the root issue can be uncovered and corrected." +
            if (cause != null) "\nCaused by: $cause" else ""
    )

    data object DatabasePathException :
        InitializeException(
            "Critical failure to locate path for storing databases. Perhaps this device prevents" +
                " apps from storing data? We cannot initialize the wallet unless we can store" +
                " data."
        ) {
        private fun readResolve(): Any = DatabasePathException
    }

    data class MissingDatabaseException(
        val network: ZcashNetwork,
        val alias: String
    ) : InitializeException(
            "The requested database file with network: $network and alias: $alias does not exist yet. Create and " +
                "initialize it using functions from ${DatabaseCoordinator::class.simpleName} first."
        )

    class InvalidBirthdayHeightException(birthday: BlockHeight?, network: ZcashNetwork) : InitializeException(
        "Invalid birthday height of ${birthday?.value}. The birthday height must be at least the height of" +
            " Sapling activation on ${network.networkName} (${network.saplingActivationHeight})."
    )

    data object MissingDefaultBirthdayException : InitializeException(
        "The birthday height is missing and it is unclear which value to use as a default."
    ) {
        private fun readResolve(): Any = MissingDefaultBirthdayException
    }
}

/**
 * Exceptions thrown while interacting with lightwalletd.
 */
sealed class LightWalletException(message: String, cause: Throwable? = null) : SdkException(message, cause) {
    class DownloadBlockException(code: Int, description: String?, cause: Throwable) : SdkException(
        "Failed to download block with code: $code due to: ${description ?: "-"}",
        cause
    )

    class GetSubtreeRootsException(code: Int, description: String?, cause: Throwable) : SdkException(
        "Failed to get subtree roots with code: $code due to: ${description ?: "-"}",
        cause
    )

    class FetchUtxosException(code: Int, description: String?, cause: Throwable) : SdkException(
        "Failed to fetch UTXOs with code: $code due to: ${description ?: "-"}",
        cause
    )

    class GetLatestBlockHeightException(code: Int, description: String?, cause: Throwable) : SdkException(
        "Failed to fetch latest block height with code: $code due to: ${description ?: "-"}",
        cause
    )
}

/**
 * Potentially user-facing exceptions thrown while encoding transactions.
 */
sealed class TransactionEncoderException(
    message: String,
    cause: Throwable? = null
) : SdkException(message, cause) {
    class FetchParamsException internal constructor(
        internal val parameters: SaplingParameters,
        message: String
    ) : TransactionEncoderException("Failed to fetch params: $parameters, due to: $message")

    class ValidateParamsException internal constructor(
        internal val parameters: SaplingParameters,
        message: String
    ) : TransactionEncoderException("Failed to validate fetched params: $parameters, due to:$message")

    data object MissingParamsException : TransactionEncoderException(
        "Cannot send funds due to missing spend or output params and attempting to download them failed."
    ) {
        private fun readResolve(): Any = MissingParamsException
    }

    class TransactionNotFoundException(transactionId: FirstClassByteArray) : TransactionEncoderException(
        "Unable to find transactionId $transactionId in the repository. This means the wallet created a transaction " +
            "and then returned a row ID that does not actually exist. This is a scenario where the wallet should " +
            "have thrown an exception but failed to do so."
    )

    class TransactionNotEncodedException(transactionId: Long) : TransactionEncoderException(
        "The transaction returned by the wallet," +
            " with id $transactionId, does not have any raw data. This is a scenario where the wallet should have " +
            "thrown an exception but failed to do so."
    )

    class IncompleteScanException(lastScannedHeight: BlockHeight?) : TransactionEncoderException(
        "Cannot" +
            " create spending transaction because scanning is incomplete. We must scan up to the" +
            " latest height to know which consensus rules to apply. However, the last scanned" +
            " height was $lastScannedHeight."
    )
}

class TransactionSubmitException : Exception()
