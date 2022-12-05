package cash.z.wallet.sdk.sample.demoapp.preference.model.entry

import cash.z.wallet.sdk.sample.demoapp.preference.MockPreferenceProvider
import cash.z.wallet.sdk.sample.demoapp.preference.fixture.IntegerPreferenceDefaultFixture
import cash.z.wallet.sdk.sample.demoapp.preference.fixture.StringDefaultPreferenceFixture
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class IntegerPreferenceDefaultTest {
    @Test
    fun key() {
        assertEquals(IntegerPreferenceDefaultFixture.KEY, IntegerPreferenceDefaultFixture.new().key)
    }

    @Test
    fun value_default() = runTest {
        val entry = IntegerPreferenceDefaultFixture.new()
        assertEquals(IntegerPreferenceDefaultFixture.DEFAULT_VALUE, entry.getValue(MockPreferenceProvider()))
    }

    @Test
    fun value_override() = runTest {
        val expected = IntegerPreferenceDefaultFixture.DEFAULT_VALUE + 5

        val entry = IntegerPreferenceDefaultFixture.new()
        val mockPreferenceProvider = MockPreferenceProvider { mutableMapOf(StringDefaultPreferenceFixture.KEY.key to expected.toString()) }

        assertEquals(expected, entry.getValue(mockPreferenceProvider))
    }
}
