package co.electriccoin.lightwallet.client.model

internal const val CONNECTION_ERROR_CODE = 3100
internal const val CONNECTION_ERROR_DESCRIPTION = "Missing internet connection." // NON-NLS

sealed class Response<T> {
    data class Success<T>(val result: T) : Response<T>()

    sealed class Failure<T>(
        open val code: Int,
        open val description: String?
    ) : Response<T>() {

        /**
         * Use this function to convert Failure into Throwable object.
         */
        fun toThrowable() =
            Throwable("Communication failure with details: $code${description?.let{": $it"} ?: "."}") // NON-NLS

        /**
         * The client was not able to communicate with the server.
         */
        class Connection<T>(
            override val description: String? = CONNECTION_ERROR_DESCRIPTION
        ) : Failure<T>(CONNECTION_ERROR_CODE, description) {
            override fun toString(): String {
                return "Connection Error(code='$code', description='$description')" // NON-NLS
            }
        }

        /**
         * The server did respond and returned an error.
         */
        sealed class Server<T>(
            override val code: Int,
            override val description: String?
        ) : Failure<T>(code, description) {
            /**
             * The operation was aborted, typically due to a concurrency issue like sequencer check failures,
             * transaction aborts, etc.
             */
            class Aborted<T>(code: Int, description: String?) : Server<T>(code, description)

            /**
             * Deadline expired before operation could complete.  For operations that change the state of the system,
             * this error may be returned even if the operation has completed successfully.  For example, a
             * successful response from a server could have been delayed long enough for the deadline to expire.
             */
            class DeadlineExceeded<T>(code: Int, description: String?) : Server<T>(code, description)

            /**
             * Some requested entity (e.g., file or directory) was not found.
             */
            class NotFound<T>(code: Int, description: String?) : Server<T>(code, description)

            /**
             * This state covers errors like ALREADY_EXISTS, FAILED_PRECONDITION, DATA_LOSS, INTERNAL, INVALID_ARGUMENT,
             * OUT_OF_RANGE, RESOURCE_EXHAUSTED, UNAUTHENTICATED or UNIMPLEMENTED. You find about these errors in
             * {@link io.grpc.Status.Code}.
             */
            class Other<T>(code: Int, description: String?) : Server<T>(code, description)

            /**
             * The caller does not have permission to execute the specified operation.
             */
            class PermissionDenied<T>(code: Int, description: String?) : Server<T>(code, description)

            /**
             * The service is currently unavailable.  This is a most likely a transient condition and may be
             * corrected by retrying with a backoff. Note that it is not always safe to retry non-idempotent operations.
             */
            class Unavailable<T>(code: Int, description: String?) : Server<T>(code, description)

            /**
             * Unknown error. An example of where this error may be returned is if a Status value received from
             * another address space belongs to an error-space that is not known in this address space.  Also errors
             * raised by APIs that do not return enough error information may be converted to this error.
             */
            class Unknown<T>(code: Int, description: String?) : Server<T>(code, description)

            override fun toString(): String {
                return "Server Error(code='$code', description='$description')" // NON-NLS
            }
        }

        /**
         * A failure occurred on the client, such as parsing a response failed.
         */
        sealed class Client<T>(
            override val code: Int,
            override val description: String?
        ) : Failure<T>(code, description) {
            /**
             * The operation was cancelled (typically by the caller).
             */
            class Canceled<T>(code: Int, description: String?) : Client<T>(code, description)

            override fun toString(): String {
                return "Client Error(code='$code', description='$description')" // NON-NLS
            }
        }
    }
}
