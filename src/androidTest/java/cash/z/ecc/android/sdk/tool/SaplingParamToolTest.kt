package cash.z.ecc.android.sdk.tool

import androidx.test.ext.junit.runners.AndroidJUnit4
import junit.framework.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SaplingParamToolTest {

    @Test
    fun validateParamTest(){
        val pathDir = "pathParamsDir"

        val res = SaplingParamTool.validate(pathDir)

        assertEquals(res, true)
    }
}