package cash.z.ecc.android.sdk.internal.ext.android

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals

class ComputableFlowTest {

    private class TestComputableFlow : ComputableFlow<Int>(dispatcher = Dispatchers.Unconfined) {
        var computationCounter: Int = 0
            private set

        override fun compute() = ++computationCounter
    }

    @Test
    fun shouldComputeOnInvalidation() {
        val testComputableFlow = TestComputableFlow()

        testComputableFlow.invalidate()
        testComputableFlow.invalidate()

        assertEquals(2, testComputableFlow.computationCounter)
    }

    @Test
    fun shouldInvalidateOnCollection() = runBlocking {
        val testComputableFlow = TestComputableFlow()
        testComputableFlow.flow.first()

        assertEquals(1, testComputableFlow.computationCounter)
    }
}
