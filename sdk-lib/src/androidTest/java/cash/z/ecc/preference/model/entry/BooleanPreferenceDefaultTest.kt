package cash.z.ecc.preference.model.entry

import cash.z.ecc.preference.MockPreferenceProvider
import cash.z.ecc.preference.fixture.BooleanPreferenceDefaultFixture
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class BooleanPreferenceDefaultTest {
    @Test
    fun key() {
        assertEquals(BooleanPreferenceDefaultFixture.KEY, BooleanPreferenceDefaultFixture.newTrue().key)
    }

    @Test
    fun value_default_true() =
        runTest {
            val entry = BooleanPreferenceDefaultFixture.newTrue()
            assertTrue(entry.getValue(MockPreferenceProvider()))
        }

    @Test
    fun value_default_false() =
        runTest {
            val entry = BooleanPreferenceDefaultFixture.newFalse()
            assertFalse(entry.getValue(MockPreferenceProvider()))
        }

    @Test
    fun value_from_config_false() =
        runTest {
            val entry = BooleanPreferenceDefaultFixture.newTrue()
            val mockPreferenceProvider =
                MockPreferenceProvider {
                    mutableMapOf(BooleanPreferenceDefaultFixture.KEY.key to false.toString())
                }
            assertFalse(entry.getValue(mockPreferenceProvider))
        }

    @Test
    fun value_from_config_true() =
        runTest {
            val entry = BooleanPreferenceDefaultFixture.newTrue()
            val mockPreferenceProvider =
                MockPreferenceProvider {
                    mutableMapOf(BooleanPreferenceDefaultFixture.KEY.key to true.toString())
                }
            assertTrue(entry.getValue(mockPreferenceProvider))
        }
}
