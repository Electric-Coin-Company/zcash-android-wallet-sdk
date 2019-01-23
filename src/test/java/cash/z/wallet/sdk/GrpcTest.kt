package cash.z.wallet.sdk


import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import cash.z.wallet.sdk.rpc.CompactTxStreamerGrpc
import cash.z.wallet.sdk.rpc.Service
import cash.z.wallet.sdk.rpc.Service.*
import rpc.WalletDataOuterClass
import java.util.concurrent.TimeUnit

class GrpcTest {
    @Test
    fun testSanity_protoFilesCreated() {
        val name = "Zooko"
        val data = WalletDataOuterClass.WalletData.newBuilder()
            .setName(name)
            .setId(1)
            .build()
        assertEquals(name, data.name)
    }

    @Test
    fun testSanity_serviceCreated() {
        val result = blockingStub.getLatestBlock(ChainSpec.newBuilder().build())
        assertNotNull(result)
    }

    @Test
    fun testSanity_transactionParsing() {
        val result =
            blockingStub.getBlockRange(
                BlockRange.newBuilder()
                    .setStart(heightOf(372950))
                    .setEnd(heightOf(372954))
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