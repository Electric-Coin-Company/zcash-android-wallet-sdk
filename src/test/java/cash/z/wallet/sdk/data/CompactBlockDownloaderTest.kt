package cash.z.wallet.sdk.data

import org.junit.jupiter.api.Assertions.*
import java.util.function.Supplier

class CompactBlockDownloaderTest {

    @org.junit.jupiter.api.BeforeEach
    fun setUp() {
    }

    @org.junit.jupiter.api.AfterEach
    fun tearDown() {
    }

    private val supplier: Supplier<String> = Supplier { "failure is a virtue" }

    @org.junit.jupiter.api.Test
    fun blocks() {
        assertEquals(5,  4+1, supplier)
    }

    @org.junit.jupiter.api.Test
    fun start() {
    }
}