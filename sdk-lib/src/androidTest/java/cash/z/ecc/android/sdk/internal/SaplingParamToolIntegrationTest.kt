package cash.z.ecc.android.sdk.internal

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import cash.z.ecc.android.sdk.exception.TransactionEncoderException
import cash.z.ecc.android.sdk.internal.ext.listFilesSuspend
import cash.z.ecc.android.sdk.test.getAppContext
import cash.z.ecc.fixture.SaplingParamsFixture
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

// TODO [#650]: https://github.com/zcash/zcash-android-wallet-sdk/issues/650
// TODO [#650]: Move integration tests to separate module
@Ignore(
    "These tests need to be refactored to a separate test module. They cause SSLHandshakeException: Chain " +
        "validation failed on CI."
)
@RunWith(AndroidJUnit4::class)
class SaplingParamToolIntegrationTest {

    private val spendSaplingParams = SaplingParamsFixture.new()

    private val outputSaplingParams = SaplingParamsFixture.new(
        SaplingParamsFixture.DESTINATION_DIRECTORY,
        SaplingParamsFixture.OUTPUT_FILE_NAME,
        SaplingParamsFixture.OUTPUT_FILE_MAX_SIZE,
        SaplingParamsFixture.OUTPUT_FILE_HASH
    )

    @Before
    fun setup() {
        // clear and prepare the param files
        runBlocking {
            SaplingParamsFixture.clearAllFilesFromDirectory(SaplingParamsFixture.DESTINATION_DIRECTORY)
            SaplingParamsFixture.clearAllFilesFromDirectory(SaplingParamsFixture.DESTINATION_DIRECTORY_LEGACY)
        }
    }

    @Test
    @LargeTest
    fun test_files_exists() = runBlocking {
        val saplingParamTool = SaplingParamTool.new(getAppContext())

        saplingParamTool.fetchParams(spendSaplingParams)
        saplingParamTool.fetchParams(outputSaplingParams)

        val result = saplingParamTool.validate(
            SaplingParamsFixture.DESTINATION_DIRECTORY
        )

        assertTrue(result)
    }

    @Test
    @LargeTest
    fun output_file_exists() = runBlocking {
        val saplingParamTool = SaplingParamTool.new(getAppContext())

        saplingParamTool.fetchParams(spendSaplingParams)
        File(spendSaplingParams.destinationDirectory, spendSaplingParams.fileName).delete()

        val result = saplingParamTool.validate(spendSaplingParams.destinationDirectory)

        assertFalse(result, "Validation should fail as the spend param file is missing.")
    }

    @Test
    @LargeTest
    fun spend_file_exists() = runBlocking {
        val saplingParamTool = SaplingParamTool.new(getAppContext())

        saplingParamTool.fetchParams(outputSaplingParams)
        File(outputSaplingParams.destinationDirectory, outputSaplingParams.fileName).delete()

        val result = saplingParamTool.validate(outputSaplingParams.destinationDirectory)

        assertFalse(result, "Validation should fail as the output param file is missing.")
    }

    @Test
    @LargeTest
    fun check_all_files_fetched() = runBlocking {
        val expectedSpendFile = File(
            SaplingParamsFixture.DESTINATION_DIRECTORY,
            SaplingParamsFixture.SPEND_FILE_NAME
        )
        val expectedOutputFile = File(
            SaplingParamsFixture.DESTINATION_DIRECTORY,
            SaplingParamsFixture.OUTPUT_FILE_NAME
        )

        val saplingParamTool = SaplingParamTool.new(getAppContext())

        saplingParamTool.ensureParams(SaplingParamsFixture.DESTINATION_DIRECTORY)

        val actualFiles = SaplingParamsFixture.DESTINATION_DIRECTORY.listFilesSuspend()
        assertNotNull(actualFiles)

        assertContains(actualFiles, expectedSpendFile)

        assertContains(actualFiles, expectedOutputFile)
    }

    @Test
    @LargeTest
    fun check_correct_spend_param_file_size() = runBlocking {
        val saplingParamTool = SaplingParamTool.new(getAppContext())

        saplingParamTool.fetchParams(spendSaplingParams)

        val expectedSpendFile = File(
            SaplingParamsFixture.DESTINATION_DIRECTORY,
            SaplingParamsFixture.SPEND_FILE_NAME
        )

        assertTrue(expectedSpendFile.length() < SaplingParamsFixture.SPEND_FILE_MAX_SIZE)
        assertFalse(expectedSpendFile.length() < SaplingParamsFixture.OUTPUT_FILE_MAX_SIZE)
    }

    @Test
    @LargeTest
    fun check_correct_output_param_file_size() = runBlocking {
        val saplingParamTool = SaplingParamTool.new(getAppContext())

        saplingParamTool.fetchParams(outputSaplingParams)

        val expectedOutputFile = File(
            SaplingParamsFixture.DESTINATION_DIRECTORY,
            SaplingParamsFixture.OUTPUT_FILE_NAME
        )

        assertTrue(expectedOutputFile.length() < SaplingParamsFixture.OUTPUT_FILE_MAX_SIZE)
        assertFalse(expectedOutputFile.length() > SaplingParamsFixture.SPEND_FILE_MAX_SIZE)
    }

    @Test
    @LargeTest
    fun fetch_params_uninitialized_test() = runTest {
        val saplingParamTool = SaplingParamTool.new(getAppContext())

        SaplingParamsFixture.DESTINATION_DIRECTORY.delete()

        assertFailsWith<TransactionEncoderException.FetchParamsException> {
            saplingParamTool.fetchParams(spendSaplingParams)
        }

        assertFalse(saplingParamTool.validate(SaplingParamsFixture.DESTINATION_DIRECTORY))
    }

    @Test
    @LargeTest
    fun fetch_params_incorrect_hash_test() = runTest {
        val saplingParamTool = SaplingParamTool.new(getAppContext())

        assertFailsWith<TransactionEncoderException.ValidateParamsException> {
            saplingParamTool.fetchParams(
                SaplingParamsFixture.new(
                    fileName = SaplingParamsFixture.OUTPUT_FILE_NAME,
                    fileMaxSize = SaplingParamsFixture.OUTPUT_FILE_MAX_SIZE,
                    fileHash = "test_hash_which_causes_failure_of_validation"
                )
            )
        }

        assertFalse(saplingParamTool.validate(SaplingParamsFixture.DESTINATION_DIRECTORY))
    }

    @Test
    @LargeTest
    fun fetch_params_incorrect_max_file_size_test() = runTest {
        val saplingParamTool = SaplingParamTool.new(getAppContext())

        assertFailsWith<TransactionEncoderException.ValidateParamsException> {
            saplingParamTool.fetchParams(
                SaplingParamsFixture.new(
                    fileName = SaplingParamsFixture.OUTPUT_FILE_NAME,
                    fileHash = SaplingParamsFixture.OUTPUT_FILE_HASH,
                    fileMaxSize = 0
                )
            )
        }

        assertFalse(saplingParamTool.validate(SaplingParamsFixture.DESTINATION_DIRECTORY))
    }

    @Test
    @LargeTest
    fun fetch_param_manual_recover_test_from_fetch_params_exception() = runTest {
        val saplingParamTool = SaplingParamTool.new(getAppContext())

        SaplingParamsFixture.DESTINATION_DIRECTORY.delete() // will cause the FetchParamsException

        val exception = assertFailsWith<TransactionEncoderException.FetchParamsException> {
            saplingParamTool.fetchParams(outputSaplingParams)
        }

        assertEquals(outputSaplingParams.fileName, exception.parameters.fileName)

        val expectedOutputFile = File(
            SaplingParamsFixture.DESTINATION_DIRECTORY,
            SaplingParamsFixture.OUTPUT_FILE_NAME
        )

        assertFalse(expectedOutputFile.exists())

        // to set up the missing deleted folder
        SaplingParamTool.initAndGetParamsDestinationDir(saplingParamTool.properties)

        // re-try with parameters returned by the exception
        saplingParamTool.fetchParams(exception.parameters)

        assertTrue(expectedOutputFile.exists())
    }

    @Test
    @LargeTest
    fun fetch_param_manual_recover_test_from_validate_params_exception() = runTest {
        val saplingParamTool = SaplingParamTool.new(getAppContext())

        val expectedOutputFile = File(
            SaplingParamsFixture.DESTINATION_DIRECTORY,
            SaplingParamsFixture.OUTPUT_FILE_NAME
        )

        val outputSaplingParams = SaplingParamsFixture.new(
            SaplingParamsFixture.DESTINATION_DIRECTORY,
            SaplingParamsFixture.OUTPUT_FILE_NAME,
            SaplingParamsFixture.OUTPUT_FILE_MAX_SIZE,
            SaplingParamsFixture.SPEND_FILE_HASH // will cause the ValidateParamsException
        )

        val exception = assertFailsWith<TransactionEncoderException.ValidateParamsException> {
            saplingParamTool.fetchParams(outputSaplingParams)
        }

        assertFalse(expectedOutputFile.exists())

        val fixedOutputSaplingParams = SaplingParamsFixture.new(
            destinationDirectoryPath = exception.parameters.destinationDirectory,
            fileName = exception.parameters.fileName,
            fileMaxSize = exception.parameters.fileMaxSizeBytes,
            fileHash = SaplingParamsFixture.OUTPUT_FILE_HASH // fixed file hash
        )

        // re-try with fixed parameters
        saplingParamTool.fetchParams(fixedOutputSaplingParams)

        assertTrue(expectedOutputFile.exists())
    }
}
