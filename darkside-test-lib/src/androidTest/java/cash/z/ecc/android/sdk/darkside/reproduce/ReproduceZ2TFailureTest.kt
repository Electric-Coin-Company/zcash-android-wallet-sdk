package cash.z.ecc.android.sdk.darkside.reproduce

import androidx.test.ext.junit.runners.AndroidJUnit4
import cash.z.ecc.android.sdk.darkside.test.DarksideTest
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReproduceZ2TFailureTest : DarksideTest() {
    @Before
    fun setup() {
        println("dBUG RUNNING")
    }

    @Test
    @Ignore("This test is broken")
    fun once() {
    }

    @Test
    @Ignore("This test is broken")
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
