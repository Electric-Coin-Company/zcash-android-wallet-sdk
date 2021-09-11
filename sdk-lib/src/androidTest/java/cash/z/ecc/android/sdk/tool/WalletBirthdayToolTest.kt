package cash.z.ecc.android.sdk.tool

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import cash.z.ecc.android.sdk.tool.WalletBirthdayTool.Companion.IS_FALLBACK_ON_FAILURE
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WalletBirthdayToolTest {
    @Test
    @SmallTest
    fun birthday_height_from_filename() {
        assertEquals(123, WalletBirthdayTool.birthdayHeight("123.json"))
    }

    @Test
    @SmallTest
    fun load_latest_birthday() {
        // Using a separate directory, so that we don't have to keep updating this test each time
        // mainnet or testnet changes
        val directory = "saplingtree/goodnet"

        val context = ApplicationProvider.getApplicationContext<Context>()
        val birthday = WalletBirthdayTool.getFirstValidWalletBirthday(
            context,
            directory,
            listOf("1300000.json", "1290000.json")
        )
        assertEquals(1300000, birthday.height)
    }

    @Test
    @SmallTest
    fun load_latest_birthday_fallback_on_bad_json() {
        if (!IS_FALLBACK_ON_FAILURE) {
            return
        }

        val directory = "saplingtree/badnet"
        val context = ApplicationProvider.getApplicationContext<Context>()
        val birthday = WalletBirthdayTool.getFirstValidWalletBirthday(
            context,
            directory,
            listOf("1300000.json", "1290000.json")
        )
        assertEquals(1290000, birthday.height)
    }
}
