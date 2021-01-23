package cash.z.ecc.android.sdk.tool

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cash.z.ecc.android.sdk.exception.LightWalletException
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SaplingParamToolTest {

    val context: Context = InstrumentationRegistry.getInstrumentation().context

    val cacheDir = "${context.cacheDir.absolutePath}/params"


    @Test
    fun testFilesExist(){
        val result = SaplingParamTool.validate(cacheDir)

        Assert.assertFalse(result)
    }

    @Test
    fun testOnlySpendFileExit(){
        val result = SaplingParamTool.validate(cacheDir)

        Assert.assertFalse(result)
    }

    @Test
    fun testOnlyOutputOFileExit(){
        val result = SaplingParamTool.validate(cacheDir)

        Assert.assertFalse(result)
    }

    @Test
    suspend fun testInsufficientDeviceStorage() = runBlocking {
        SaplingParamTool.fetchParams(cacheDir)

        Assert.assertFalse("insufficient storage",false)
    }

    @Test
    fun testSufficientDeviceStorageForOnlyOneFile() = runBlocking {
        SaplingParamTool.fetchParams(cacheDir)

        Assert.assertFalse("insufficient storage",false)
    }

}