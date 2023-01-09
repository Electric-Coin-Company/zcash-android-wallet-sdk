package cash.z.wallet.sdk.sample.demoapp.preference.model.entry

import cash.z.wallet.sdk.sample.demoapp.preference.MockPreferenceProvider
import cash.z.wallet.sdk.sample.demoapp.preference.fixture.StringDefaultPreferenceFixture
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class StringPreferenceDefaultTest {
    @Test
    fun key() {
        assertEquals(StringDefaultPreferenceFixture.KEY, StringDefaultPreferenceFixture.new().key)
    }

    @Test
    fun value_default() = runTest {
        val entry = StringDefaultPreferenceFixture.new()
        assertEquals(StringDefaultPreferenceFixture.DEFAULT_VALUE, entry.getValue(MockPreferenceProvider()))
    }

    @Test
    fun value_override() = runTest {
        val entry = StringDefaultPreferenceFixture.new()

        val mockPreferenceProvider = MockPreferenceProvider { mutableMapOf(StringDefaultPreferenceFixture.KEY.key to "override") }

        assertEquals("override", entry.getValue(mockPreferenceProvider))
    }
}
