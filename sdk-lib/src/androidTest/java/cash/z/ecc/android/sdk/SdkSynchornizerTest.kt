package cash.z.ecc.android.sdk

import androidx.test.filters.SmallTest
import cash.z.ecc.android.sdk.fixture.FakeSynchronizerFixture
import cash.z.ecc.android.sdk.test.getAppContext
import org.junit.Test
import kotlin.test.assertNotNull

class SdkSynchornizerTest {

    @Test
    @SmallTest
    fun validate_mainnet_assets() {
        val synchornizer = FakeSynchronizerFixture.new(
            getAppContext(),
        )

        assertNotNull(synchornizer)
    }
}
