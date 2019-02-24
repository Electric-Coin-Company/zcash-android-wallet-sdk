package cash.z.wallet.sdk.db

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import cash.z.wallet.sdk.secure.Wallet
import org.junit.Assert.assertEquals
import org.junit.Test


class WalletTest {
    val context: Context = InstrumentationRegistry.getInstrumentation().context

    @Test
    fun testLoadDefaultWallet() {
        val birthday = Wallet.loadBirthdayFromAssets(context, 280000)
        assertEquals("Invalid tree", "000000", birthday.tree)
        assertEquals("Invalid height", 280000, birthday.height)
        assertEquals("Invalid time", 1535262293, birthday.time)
    }
}