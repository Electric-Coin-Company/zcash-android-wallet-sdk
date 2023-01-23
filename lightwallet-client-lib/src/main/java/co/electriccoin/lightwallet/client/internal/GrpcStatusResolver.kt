package co.electriccoin.lightwallet.client.internal

import android.util.Log
import co.electriccoin.lightwallet.client.ApiStatusResolver
import co.electriccoin.lightwallet.client.model.Response
import io.grpc.Status

/**
 * This class provides conversion from GRPC Status to our predefined Server or Client error classes.
 */
object GrpcStatusResolver : ApiStatusResolver {

    override fun <T> resolveFailureFromStatus(throwable: Throwable): Response.Failure<T> {
        val status = Status.fromThrowable(throwable)
        Log.w(Constants.LOG_TAG, "Networking error: ${status.code}: ${status.description}")

        return when (status.code) {
            Status.Code.ABORTED -> {
                Response.Failure.Server.Aborted(
                    code = status.code.value(),
                    description = status.description
                )
            }
            Status.Code.CANCELLED -> {
                Response.Failure.Client.Canceled(
                    code = status.code.value(),
                    description = status.description
                )
            }
            Status.Code.DEADLINE_EXCEEDED -> {
                Response.Failure.Server.DeadlineExceeded(
                    code = status.code.value(),
                    description = status.description
                )
            }
            Status.Code.NOT_FOUND -> {
                Response.Failure.Server.NotFound(
                    code = status.code.value(),
                    description = status.description
                )
            }
            Status.Code.PERMISSION_DENIED -> {
                Response.Failure.Server.PermissionDenied(
                    code = status.code.value(),
                    description = status.description
                )
            }
            Status.Code.UNAVAILABLE -> {
                Response.Failure.Server.Unavailable(
                    code = status.code.value(),
                    description = status.description
                )
            }
            Status.Code.UNKNOWN -> {
                Response.Failure.Server.Unknown(
                    code = status.code.value(),
                    description = status.description
                )
            }
            else -> {
                Response.Failure.Server.Other(
                    code = status.code.value(),
                    description = status.description
                )
            }
        }
    }
}
