package cash.z.ecc.android.sdk.internal.jni

import cash.z.ecc.android.sdk.internal.model.TorClient
import cash.z.ecc.android.sdk.internal.model.TorDormantMode
import co.electriccoin.lightwallet.client.model.BlockHeightUnsafe
import co.electriccoin.lightwallet.client.model.LightWalletEndpointInfoUnsafe
import co.electriccoin.lightwallet.client.model.RawTransactionUnsafe
import co.electriccoin.lightwallet.client.model.Response
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.io.path.createTempDirectory
import kotlin.test.Ignore
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

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
            val lwdConn = torClient.createIsolatedWalletClient("https://testnet.zec.rocks:443")

            // Confirm that it is on testnet.
            val info =
                when (val res = lwdConn.getServerInfo()) {
                    is Response.Success<LightWalletEndpointInfoUnsafe> -> res.result
                    is Response.Failure -> fail("getServerInfo failed (${res.code}): ${res.description}")
                }
            assertEquals("test", info.chainName)
            assertEquals(BlockHeightUnsafe(280000), info.saplingActivationHeightUnsafe)

            // Confirm that it has the block containing the known testnet transaction.
            val txHeight = BlockHeightUnsafe(1234567)
            assert(info.blockHeightUnsafe >= txHeight)
            val latest =
                when (val res = lwdConn.getLatestBlockHeight()) {
                    is Response.Success<BlockHeightUnsafe> -> res.result
                    is Response.Failure -> fail("getLatestBlockHeight failed (${res.code}): ${res.description}")
                }
            assert(latest.value >= txHeight.value)

            // Fetch a known testnet transaction.
            val txId =
                "9e309d29a99f06e6dcc7aee91dca23c0efc2cf5083cc483463ddbee19c1fadf1".hexToByteArray().reversedArray()
            val tx =
                when (val res = lwdConn.fetchTransaction(txId)) {
                    is Response.Success<RawTransactionUnsafe> -> res.result
                    is Response.Failure -> fail("fetchTransaction failed (${res.code}): ${res.description}")
                }

            val submit = lwdConn.submitTransaction(tx.data)

            // We should fail to resubmit the already-mined transaction.
            assertTrue(submit is Response.Failure.OverTor)
            assertEquals(
                "Failed to submit transaction (-25): failed to validate tx: transaction::Hash(\"private\"), " +
                    "error: transaction is already in state",
                submit.description
            )

            // We can background the Tor client.
            torClient.setDormant(TorDormantMode.SOFT)
        }
}
