package co.electriccoin.lightwallet.client.internal

import androidx.test.filters.SmallTest
import cash.z.ecc.android.sdk.test.getAppContext
import kotlin.test.Ignore
import kotlin.test.Test

class ChannelFactoryTest {

    private val channelFactory = AndroidChannelFactory(getAppContext())

    @Test
    @SmallTest
    @Ignore("Finish the test once we can reach the needed model classes.")
    fun new_channel_sanity_test() {
        // TODO [#897]: Prepare new model classes module
        // TODO [#897]: https://github.com/zcash/zcash-android-wallet-sdk/issues/897
    }
}
