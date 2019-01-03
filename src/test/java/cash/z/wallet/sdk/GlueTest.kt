package cash.z.wallet.sdk


import android.util.Log
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.BeforeClass
import org.junit.Test
import rpc.CompactTxStreamerGrpc
import rpc.Service
import rpc.Service.*
import rpc.WalletDataOuterClass
import java.util.concurrent.TimeUnit

class GlueTest {

    @Test
    fun testSanity_transactionParsing() {
        val result =
            blockingStub.getBlockRange(
                BlockRange.newBuilder()
                    .setStart(heightOf(373070))
                    .setEnd(heightOf(373085))
                    .build()
            )
        assertNotNull(result)
        assertEquals(372950, result.next().height)
    }

    fun heightOf(height: Long): Service.BlockID {
        return BlockID.newBuilder().setHeight(height).build()
    }

    companion object {
        lateinit var blockingStub: CompactTxStreamerGrpc.CompactTxStreamerBlockingStub

        @BeforeClass
        @JvmStatic
        fun setup() {
            val channel = ManagedChannelBuilder.forAddress("localhost", 9067).usePlaintext().build()
            blockingStub = CompactTxStreamerGrpc.newBlockingStub(channel)
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            (blockingStub.channel as ManagedChannel).shutdown().awaitTermination(2000L, TimeUnit.MILLISECONDS)
        }
    }
}