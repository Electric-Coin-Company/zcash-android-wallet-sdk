package cash.z.ecc.android.sdk.exception

import java.lang.RuntimeException


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
    class BalanceException(cause: Throwable) : RustLayerException("Error while requesting the current balance over " +
            "JNI. This might mean that the database has been corrupted and needs to be rebuilt. Verify that " +
            "blocks are not missing or have not been scanned out of order.", cause)
}

/**
 * User-facing exceptions thrown by the transaction repository.
 */
sealed class RepositoryException(message: String, cause: Throwable? = null) : SdkException(message, cause) {
    object FalseStart: RepositoryException( "The channel is closed. Note that once a repository has stopped it " +
            "cannot be restarted. Verify that the repository is not being restarted.")
}

/**
 * High-level exceptions thrown by the synchronizer, which do not fall within the umbrella of a
 * child component.
 */
sealed class SynchronizerException(message: String, cause: Throwable? = null) : SdkException(message, cause) {
    object FalseStart: SynchronizerException("This synchronizer was already started. Multiple calls to start are not" +
                "allowed and once a synchronizer has stopped it cannot be restarted."
    )
    object NotYetStarted: SynchronizerException("The synchronizer has not yet started. Verify that" +
            " start has been called prior to this operation and that the coroutineScope is not" +
            " being accessed before it is initialized."
    )
}

/**
 * Potentially user-facing exceptions that occur while processing compact blocks.
 */
sealed class CompactBlockProcessorException(message: String, cause: Throwable? = null) : SdkException(message, cause) {
    class DataDbMissing(path: String): CompactBlockProcessorException("No data db file found at path $path. Verify " +
            "that the data DB has been initialized via `rustBackend.initDataDb(path)`")
    open class ConfigurationException(message: String, cause: Throwable?) : CompactBlockProcessorException(message, cause)
    class FileInsteadOfPath(fileName: String) : ConfigurationException("Invalid Path: the given path appears to be a" +
            " file name instead of a path: $fileName. The RustBackend expects the absolutePath to the database rather" +
            " than just the database filename because Rust does not access the app Context." +
            " So pass in context.getDatabasePath(dbFileName).absolutePath instead of just dbFileName alone.", null)
    class FailedReorgRepair(message: String) : CompactBlockProcessorException(message)
    class FailedDownload(cause: Throwable? = null) : CompactBlockProcessorException("Error while downloading blocks. This most " +
            "likely means the server is down or slow to respond. See logs for details.", cause)
    class FailedScan(cause: Throwable? = null) : CompactBlockProcessorException("Error while scanning blocks. This most " +
            "likely means a block was missed or a reorg was mishandled. See logs for details.", cause)
    class Disconnected(cause: Throwable? = null) : CompactBlockProcessorException("Disconnected Error. Unable to download blocks due to ${cause?.message}", cause)
    object Uninitialized : CompactBlockProcessorException("Cannot process blocks because the wallet has not been" +
            " initialized. Verify that the seed phrase was properly created or imported. If so, then this problem" +
            " can be fixed by re-importing the wallet.")
    open class EnhanceTransactionError(message: String, val height: Int, cause: Throwable) : CompactBlockProcessorException(message, cause) {
        class EnhanceTxDownloadError(height: Int, cause: Throwable) : EnhanceTransactionError("Error while attempting to download a transaction to enhance", height, cause)
        class EnhanceTxDecryptError(height: Int, cause: Throwable) : EnhanceTransactionError("Error while attempting to decrypt and store a transaction to enhance", height, cause)
    }
}

/**
 * Exceptions related to the wallet's birthday.
 */
sealed class BirthdayException(message: String, cause: Throwable? = null) : SdkException(message, cause) {
    object UninitializedBirthdayException : BirthdayException("Error the birthday cannot be" +
            " accessed before it is initialized. Verify that the new, import or open functions" +
            " have been called on the initializer."
    )
    class MissingBirthdayFilesException(directory: String) : BirthdayException(
        "Cannot initialize wallet because no birthday files were found in the $directory directory."
    )
    class MissingBirthdayException(val alias: String) : BirthdayException(
        "Failed to initialize wallet with alias=$alias because its birthday could not be found." +
                "  Verify the alias or perhaps a new wallet should be created, instead."
    )
    class BirthdayFileNotFoundException(directory: String, height: Int?) : BirthdayException(
        "Unable to find birthday file for $height verify that $directory/$height.json exists."
    )
    class MalformattedBirthdayFilesException(directory: String, file: String) : BirthdayException(
        "Failed to parse file $directory/$file verify that it is formatted as #####.json, " +
                "where the first portion is an Int representing the height of the tree contained in the file"
    )
}

/**
 * Exceptions thrown by the initializer.
 */
sealed class InitializerException(message: String, cause: Throwable? = null) :  SdkException(message, cause){
    class FalseStart(cause: Throwable?) : InitializerException("Failed to initialize accounts due to: $cause", cause)
    class AlreadyInitializedException(cause: Throwable, dbPath: String) : InitializerException("Failed to initialize the blocks table" +
            " because it already exists in $dbPath", cause)
    object DatabasePathException :
        InitializerException("Critical failure to locate path for storing databases. Perhaps this" +
                " device prevents apps from storing data? We cannot initialize the wallet unless" +
                " we can store data.")
}

/**
 * Exceptions thrown while interacting with lightwalletd.
 */
sealed class LightwalletException(message: String, cause: Throwable? = null) : SdkException(message, cause) {
    object InsecureConnection : LightwalletException("Error: attempted to connect to lightwalletd" +
            " with an insecure connection! Plaintext connections are only allowed when the" +
            " resource value for 'R.bool.lightwalletd_allow_very_insecure_connections' is true" +
            " because this choice should be explicit.")
    class ConsensusBranchException(sdkBranch: String, lwdBranch: String) :
        LightwalletException(
            "Error: the lightwalletd server is using a consensus branch" +
                " (branch: $lwdBranch) that does not match the transactions being created" +
                " (branch: $sdkBranch). This probably means the SDK and Server are on two" +
                " different chains, most likely because of a recent network upgrade (NU). Either" +
                " update the SDK to match lightwalletd or use a lightwalletd that matches the SDK."
        )
}

/**
 * Potentially user-facing exceptions thrown while encoding transactions.
 */
sealed class TransactionEncoderException(message: String, cause: Throwable? = null) : SdkException(message, cause) {
    class FetchParamsException(message: String) : TransactionEncoderException("Failed to fetch params due to: $message")
    object MissingParamsException : TransactionEncoderException(
        "Cannot send funds due to missing spend or output params and attempting to download them failed."
    )
    class TransactionNotFoundException(transactionId: Long) : TransactionEncoderException("Unable to find transactionId " +
            "$transactionId in the repository. This means the wallet created a transaction and then returned a row ID " +
            "that does not actually exist. This is a scenario where the wallet should have thrown an exception but failed " +
            "to do so.")
    class TransactionNotEncodedException(transactionId: Long) : TransactionEncoderException("The transaction returned by the wallet," +
            " with id $transactionId, does not have any raw data. This is a scenario where the wallet should have thrown" +
            " an exception but failed to do so.")

    class IncompleteScanException(lastScannedHeight: Int) : TransactionEncoderException("Cannot" +
            " create spending transaction because scanning is incomplete. We must scan up to the" +
            " latest height to know which consensus rules to apply. However, the last scanned" +
            " height was $lastScannedHeight.")
}
