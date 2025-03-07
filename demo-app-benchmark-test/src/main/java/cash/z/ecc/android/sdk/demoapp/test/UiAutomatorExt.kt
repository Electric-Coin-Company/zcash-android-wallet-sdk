package cash.z.ecc.android.sdk.demoapp.test

import androidx.test.uiautomator.EventCondition
import androidx.test.uiautomator.SearchCondition
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import kotlin.time.Duration

fun UiDevice.waitFor(
    condition: SearchCondition<Boolean>,
    timeout: Duration
): Boolean = wait(condition, timeout.inWholeMilliseconds)

fun UiObject2.clickAndWaitFor(
    condition: EventCondition<Boolean>,
    timeout: Duration
): Boolean = clickAndWait(condition, timeout.inWholeMilliseconds)
