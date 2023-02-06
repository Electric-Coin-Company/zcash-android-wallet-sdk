package co.electriccoin.lightwallet.client.internal

import androidx.test.filters.SmallTest
import cash.z.ecc.android.sdk.test.getAppContext
import kotlin.test.Ignore
import kotlin.test.Test

class CoroutineLightWalletClientImplTest {

    private val channelFactory = AndroidChannelFactory(getAppContext())

    @Test
    @SmallTest
    @Ignore("Finish the test once we can reach the needed model classes.")
    fun get_block_range_test() {
        // TODO [#897]: Prepare new model classes module
        // TODO [#897]: https://github.com/zcash/zcash-android-wallet-sdk/issues/897

        // We'd like to create a source mock flow of several values, from which some will be StatusRuntimeException and
        // then we'll check result flow's emitted values (size, order, type, etc.).
    }
}
