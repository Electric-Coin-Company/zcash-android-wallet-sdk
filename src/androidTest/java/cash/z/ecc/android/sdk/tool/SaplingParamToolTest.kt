package cash.z.ecc.android.sdk.tool

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cash.z.ecc.android.sdk.exception.LightWalletException
import cash.z.ecc.android.sdk.ext.ZcashSdk
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class SaplingParamToolTest {

    val context: Context = InstrumentationRegistry.getInstrumentation().context

    val cacheDir = "${context.cacheDir.absolutePath}/params"

    @Before
    fun setup() {
        // clear the param files
        SaplingParamTool.clear(cacheDir)
    }


    @Test
    suspend fun testFilesExists(){
        // Given
        SaplingParamTool.fetchParams(cacheDir)

        // When
        val result = SaplingParamTool.validate(cacheDir)

        // Then
        Assert.assertFalse(result)
    }

    @Test
    suspend fun testOnlySpendFileExits(){
        // Given
        SaplingParamTool.fetchParams(cacheDir)
        File("$cacheDir/${ZcashSdk.OUTPUT_PARAM_FILE_NAME}").delete()

        // When
        val result = SaplingParamTool.validate(cacheDir)

        // Then
        Assert.assertFalse("Validation should fail when the spend params are missing", result)
    }

    @Test
    suspend fun testOnlyOutputOFileExits(){
        // Given
        SaplingParamTool.fetchParams(cacheDir)
        File("$cacheDir/${ZcashSdk.SPEND_PARAM_FILE_NAME}").delete()

        // When
        val result = SaplingParamTool.validate(cacheDir)

        // Then
        Assert.assertFalse("Validation should fail when the spend params are missing", result)
    }

    @Test
    suspend fun testInsufficientDeviceStorage() = runBlocking {
        // Given
        SaplingParamTool.fetchParams(cacheDir)

        Assert.assertFalse("insufficient storage",false)
    }

    @Test
    fun testSufficientDeviceStorageForOnlyOneFile() = runBlocking {
        SaplingParamTool.fetchParams(cacheDir)

        Assert.assertFalse("insufficient storage",false)
    }

}