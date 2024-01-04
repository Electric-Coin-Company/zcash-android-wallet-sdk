package co.electriccoin.lightwallet.client.internal

import androidx.test.filters.SmallTest
import co.electriccoin.lightwallet.client.fixture.StatusExceptionFixture
import co.electriccoin.lightwallet.client.model.Response
import io.grpc.Status
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GrpcStatusResolverTest {
    @Test
    @SmallTest
    fun resolve_explicitly_caught_server_error_test() {
        GrpcStatusResolver.resolveFailureFromStatus<Unit>(
            StatusExceptionFixture.new(Status.NOT_FOUND)
        ).also { resolvedResponse ->
            assertTrue(resolvedResponse is Response.Failure.Server.NotFound<Unit>)
            assertEquals(resolvedResponse.code, Status.NOT_FOUND.code.value())
            assertNull(resolvedResponse.description)
        }
    }

    @Test
    @SmallTest
    fun resolve_explicitly_caught_error_client_test() {
        GrpcStatusResolver.resolveFailureFromStatus<Unit>(
            StatusExceptionFixture.new(Status.CANCELLED)
        ).also { resolvedResponse ->
            assertTrue(resolvedResponse is Response.Failure.Client.Canceled<Unit>)
            assertEquals(resolvedResponse.code, Status.CANCELLED.code.value())
            assertNull(resolvedResponse.description)
        }
    }

    @Test
    @SmallTest
    fun resolve_other_test() {
        GrpcStatusResolver.resolveFailureFromStatus<Unit>(
            StatusExceptionFixture.new(Status.INTERNAL)
        ).also { resolvedResponse ->
            assertTrue(resolvedResponse is Response.Failure.Server.Other<Unit>)
            assertEquals(resolvedResponse.code, Status.INTERNAL.code.value())
            assertNull(resolvedResponse.description)
        }
    }

    @Test
    @SmallTest
    fun resolve_unexpected_error_test() {
        GrpcStatusResolver.resolveFailureFromStatus<Unit>(
            IllegalArgumentException("This should not come into the resolver.")
        ).also { resolvedResponse ->
            assertTrue(resolvedResponse is Response.Failure.Server.Unknown<Unit>)
        }
    }
}
