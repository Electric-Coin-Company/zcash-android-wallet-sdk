package cash.z.ecc.android.sdk.integration.darkside.reproduce

import cash.z.ecc.android.sdk.ext.DarksideTest
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

class ReproduceZ2TFailureTest : DarksideTest() {
    @Before
    fun setup() {
        println("dBUG RUNNING")
    }

    @Test
    fun once() {
    }

    @Test
    fun twice() {
    }

    companion object {
        @JvmStatic
        @BeforeClass
        fun beforeAll() {
            println("dBUG BEFOERE IOT ALL")
        }
    }
}
