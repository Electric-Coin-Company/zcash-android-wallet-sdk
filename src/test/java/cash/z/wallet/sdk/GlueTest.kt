package cash.z.wallet.sdk


import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import cash.z.wallet.sdk.rpc.CompactTxStreamerGrpc
import cash.z.wallet.sdk.rpc.Service
import cash.z.wallet.sdk.rpc.Service.BlockID
import cash.z.wallet.sdk.rpc.Service.BlockRange
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

        @BeforeAll
        @JvmStatic
        fun setup() {
            val channel = ManagedChannelBuilder.forAddress("localhost", 9067).usePlaintext().build()
            blockingStub = CompactTxStreamerGrpc.newBlockingStub(channel)
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            (blockingStub.channel as ManagedChannel).shutdown().awaitTermination(2000L, TimeUnit.MILLISECONDS)
        }
    }
}