package cash.z.wallet.sdk.exception

import java.lang.RuntimeException

//TODO: rename things in here when we know what we're calling the Rust layer (librustzcash?)

/**
 * Exceptions thrown in the Rust layer of the SDK. We may not always be able to surface details about this
 * exception so it's important for the SDK to provide helpful messages whenever these errors are encountered.
 */
sealed class RustLayerException(message: String, cause: Throwable? = null) : RuntimeException(message, cause) {
    class BalanceException(cause: Throwable) : RustLayerException("Error while requesting the current balance over " +
            "JNI. This might mean that the database has been corrupted and needs to be rebuilt. Verify that " +
            "blocks are not missing or have not been scanned out of order.", cause)
}

sealed class RepositoryException(message: String, cause: Throwable? = null) : RuntimeException(message, cause) {
    object FalseStart: RepositoryException( "The channel is closed. Note that once a repository has stopped it " +
            "cannot be restarted. Verify that the repository is not being restarted.")
}

sealed class SynchronizerException(message: String, cause: Throwable? = null) : RuntimeException(message, cause) {
    object FalseStart: SynchronizerException("Once a synchronizer has stopped it cannot be restarted. Instead, a new " +
            "instance should be created.")
}

sealed class CompactBlockProcessorException(message: String, cause: Throwable? = null) : RuntimeException(message, cause) {
    class DataDbMissing(path: String): CompactBlockProcessorException("No data db file found at path $path. Verify " +
            "that the data DB has been initialized via `converter.initDataDb(path)`")
}

sealed class CompactBlockStreamException(message: String, cause: Throwable? = null) : RuntimeException(message, cause) {
    object ConnectionClosed: CompactBlockStreamException("Cannot start stream when connection is closed.")
}

sealed class WalletException(message: String, cause: Throwable? = null) : RuntimeException(message, cause) {
    class MissingBirthdayFilesException(directory: String) : WalletException(
        "Cannot initialize wallet because no birthday files were found in the $directory directory."
    )
    class FetchParamsException(message: String) : WalletException("Failed to fetch params due to: $message")
    object MissingParamsException : WalletException(
        "Cannot send funds due to missing spend or output params and attempting to download them failed."
    )
    class MalformattedBirthdayFilesException(directory: String, file: String) : WalletException(
        "Failed to parse file $directory/$file verify that it is formatted as #####.json, " +
                "where the first portion is an Int representing the height of the tree contained in the file"
    )
}