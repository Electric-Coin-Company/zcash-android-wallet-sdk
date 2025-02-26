package cash.z.ecc.android.sdk.internal.jni

import cash.z.ecc.android.sdk.internal.model.TorClient
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.io.path.createTempDirectory
import kotlin.test.Ignore
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TorClientTest {
    @OptIn(ExperimentalStdlibApi::class)
    @Test
    @Ignore("requires network access")
    fun tor_lwd_can_fetch_and_submit_tx() =
        runTest {
            // Spin up a new Tor client.
            val torDir = createTempDirectory("tor-client-").toFile()
            val torClient = TorClient.new(torDir)

            // Connect to a testnet lightwalletd server.
            val lwdConn = torClient.connectToLightwalletd("https://testnet.zec.rocks:443")

            // Fetch a known testnet transaction.
            val txId =
                "9e309d29a99f06e6dcc7aee91dca23c0efc2cf5083cc483463ddbee19c1fadf1".hexToByteArray().reversedArray()
            val tx = lwdConn.fetchTransaction(txId)

            // We should fail to resubmit the already-mined transaction.
            val exception =
                assertFailsWith<RuntimeException> {
                    lwdConn.submitTransaction(tx)
                }
            assertEquals(
                "Failed to submit transaction (-25): failed to validate tx: transaction::Hash(\"private\"), " +
                    "error: transaction is already in state",
                exception.message
            )
        }
}
