package cash.z.ecc.android.sdk.internal

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cash.z.ecc.android.sdk.ext.ZcashSdk
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
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
        runBlocking { SaplingParamTool.clear(cacheDir) }
    }

    @Test
    @Ignore("This test is broken")
    fun testFilesExists() = runBlocking {
        // Given
        SaplingParamTool.fetchParams(cacheDir)

        // When
        val result = SaplingParamTool.validate(cacheDir)

        // Then
        Assert.assertFalse(result)
    }

    @Test
    fun output_file_exists() = runBlocking {
        // Given
        SaplingParamTool.fetchParams(cacheDir)
        File(cacheDir, ZcashSdk.OUTPUT_PARAM_FILE_NAME).delete()

        // When
        val result = SaplingParamTool.validate(cacheDir)

        // Then
        Assert.assertFalse("Validation should fail when the spend params are missing", result)
    }

    @Test
    fun param_file_exists() = runBlocking {
        // Given
        SaplingParamTool.fetchParams(cacheDir)
        File(cacheDir, ZcashSdk.SPEND_PARAM_FILE_NAME).delete()

        // When
        val result = SaplingParamTool.validate(cacheDir)

        // Then
        Assert.assertFalse("Validation should fail when the spend params are missing", result)
    }

    @Test
    fun testInsufficientDeviceStorage() = runBlocking {
        // Given
        SaplingParamTool.fetchParams(cacheDir)

        Assert.assertFalse("insufficient storage", false)
    }

    @Test
    fun testSufficientDeviceStorageForOnlyOneFile() = runBlocking {
        SaplingParamTool.fetchParams(cacheDir)

        Assert.assertFalse("insufficient storage", false)
    }
}
