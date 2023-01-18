package cash.z.ecc.fixture

import cash.z.ecc.android.sdk.jni.RustBackend
import cash.z.ecc.android.sdk.model.ZcashNetwork
import kotlinx.coroutines.runBlocking
import java.io.File

internal object FakeRustBackendFixture {
    val fakeRustBackend = runBlocking {
        FakeRustBackend(
            RustBackend.init(
                File(""),
                File(""),
                File(""),
                ZcashNetwork.Mainnet,
                ZcashNetwork.Mainnet.saplingActivationHeight
            )
        )
    }
}
