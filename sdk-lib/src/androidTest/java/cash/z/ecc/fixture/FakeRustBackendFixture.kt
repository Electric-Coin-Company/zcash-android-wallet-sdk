package cash.z.ecc.fixture

import kotlinx.coroutines.runBlocking

internal object FakeRustBackendFixture {

    val new = runBlocking {
        FakeRustBackend()
    }
}
