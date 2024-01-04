package co.electriccoin.lightwallet.client.fixture

import io.grpc.Status
import io.grpc.StatusRuntimeException

object StatusExceptionFixture {
    fun new(status: Status): StatusRuntimeException {
        return StatusRuntimeException(status)
    }
}
