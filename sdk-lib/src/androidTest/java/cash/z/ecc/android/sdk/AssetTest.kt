package cash.z.ecc.android.sdk

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SmallTest
import cash.z.ecc.android.sdk.model.ZcashNetwork
import cash.z.ecc.android.sdk.tool.CheckpointTool
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AssetTest {
    @Test
    @SmallTest
    fun validate_mainnet_assets() {
        val network = ZcashNetwork.Mainnet
        val assets = listAssets(network)

        assertFilesExist(assets)
        assertFilenames(assets)
        assertFileContents(network, assets)
    }

    @Test
    @SmallTest
    fun validate_testnet_assets() {
        val network = ZcashNetwork.Testnet
        val assets = listAssets(network)

        assertFilesExist(assets)
        assertFilenames(assets)
        assertFileContents(network, assets)
    }

    private fun assertFilesExist(files: Array<String>?) {
        assertFalse(files.isNullOrEmpty())
    }

    private fun assertFilenames(files: Array<String>?) {
        files?.forEach {
            val split = it.split('.')
            assertEquals(2, split.size)

            val intString = split.first()
            val extensionString = split.last()

            // Will throw exception if cannot be parsed
            intString.toInt()

            assertEquals("json", extensionString)
        }
    }

    private fun assertFileContents(
        network: ZcashNetwork,
        files: Array<String>?
    ) {
        files?.map { filename ->
            val filePath = "${CheckpointTool.checkpointDirectory(network)}/$filename"
            ApplicationProvider.getApplicationContext<Context>().assets.open(filePath)
                .use { inputSteam ->
                    inputSteam.bufferedReader().use { bufferedReader ->
                        val slurped = bufferedReader.readText()

                        JsonFile(JSONObject(slurped), filename)
                    }
                }
        }?.forEach {
            val jsonObject = it.jsonObject
            assertTrue(jsonObject.has("network"))
            assertTrue(jsonObject.has("height"))
            assertTrue(jsonObject.has("hash"))
            assertTrue(jsonObject.has("time"))
            assertTrue(jsonObject.has("saplingTree"))

            val expectedNetworkName =
                when (network) {
                    ZcashNetwork.Mainnet -> "main"
                    ZcashNetwork.Testnet -> "test"
                    else -> IllegalArgumentException("Unsupported network $network")
                }
            assertEquals("File: ${it.filename}", expectedNetworkName, jsonObject.getString("network"))

            assertEquals(
                "File: ${it.filename}",
                CheckpointTool.checkpointHeightFromFilename(it.filename).value,
                jsonObject.getLong("height")
            )

            // In the future, additional validation of the JSON can be added
        }
    }

    private data class JsonFile(val jsonObject: JSONObject, val filename: String)

    companion object {
        fun listAssets(network: ZcashNetwork): Array<String>? =
            runBlocking {
                CheckpointTool.listCheckpointDirectoryContents(
                    ApplicationProvider.getApplicationContext(),
                    CheckpointTool.checkpointDirectory(network)
                )
            }
    }
}
