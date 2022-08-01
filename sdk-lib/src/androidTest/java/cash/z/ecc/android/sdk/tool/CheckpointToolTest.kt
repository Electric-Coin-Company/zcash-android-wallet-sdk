package cash.z.ecc.android.sdk.tool

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.tool.CheckpointTool.IS_FALLBACK_ON_FAILURE
import cash.z.ecc.android.sdk.type.ZcashNetwork
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CheckpointToolTest {
    @Test
    @SmallTest
    fun birthday_height_from_filename() {
        assertEquals(
            BlockHeight.new(ZcashNetwork.Mainnet, 1_230_000),
            CheckpointTool.checkpointHeightFromFilename(ZcashNetwork.Mainnet, "1230000.json")
        )
    }

    @Test
    @SmallTest
    fun load_latest_birthday() {
        // Using a separate directory, so that we don't have to keep updating this test each time
        // mainnet or testnet changes
        val directory = "co.electriccoin.zcash/checkpoint/goodnet"

        val context = ApplicationProvider.getApplicationContext<Context>()
        val birthday = runBlocking {
            CheckpointTool.getFirstValidWalletBirthday(
                context,
                ZcashNetwork.Mainnet,
                directory,
                listOf("1300000.json", "1290000.json")
            )
        }
        assertEquals(1300000, birthday.height.value)
    }

    @Test
    @SmallTest
    fun load_latest_birthday_fallback_on_bad_json() {
        if (!IS_FALLBACK_ON_FAILURE) {
            return
        }

        val directory = "co.electriccoin.zcash/checkpoint/badnet"
        val context = ApplicationProvider.getApplicationContext<Context>()
        val birthday = runBlocking {
            CheckpointTool.getFirstValidWalletBirthday(
                context,
                ZcashNetwork.Mainnet,
                directory,
                listOf("1300000.json", "1290000.json")
            )
        }
        assertEquals(1290000, birthday.height.value)
    }
}
