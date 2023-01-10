package cash.z.wallet.sdk.sample.demoapp.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.filters.MediumTest
import cash.z.ecc.android.sdk.demoapp.ui.common.DisableScreenTimeout
import cash.z.ecc.android.sdk.demoapp.ui.common.LocalScreenTimeout
import cash.z.ecc.android.sdk.demoapp.ui.common.ScreenTimeout
import cash.z.wallet.sdk.sample.demoapp.test.UiTestPrerequisites
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ScreenTimeoutTest : UiTestPrerequisites() {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    @MediumTest
    fun acquireAndReleaseScreenTimeout() = runTest {
        val testSetup = TestSetup(composeTestRule)

        assertEquals(1, testSetup.getScreenTimeoutCount())

        testSetup.mutableScreenTimeoutFlag.update { false }
        composeTestRule.awaitIdle()
        assertEquals(0, testSetup.getScreenTimeoutCount())
    }

    private class TestSetup(composeTestRule: ComposeContentTestRule) {

        val mutableScreenTimeoutFlag = MutableStateFlow(true)

        private val screenTimeout = ScreenTimeout()

        fun getScreenTimeoutCount() = screenTimeout.referenceCount.value

        init {
            composeTestRule.setContent {
                CompositionLocalProvider(LocalScreenTimeout provides screenTimeout) {
                    MaterialTheme {
                        val disableScreenTimeout by mutableScreenTimeoutFlag.collectAsState()

                        TestView(disableScreenTimeout)
                    }
                }
            }
        }

        @Composable
        private fun TestView(disableScreenTimeout: Boolean) {
            if (disableScreenTimeout) {
                DisableScreenTimeout()
            }
        }
    }
}
