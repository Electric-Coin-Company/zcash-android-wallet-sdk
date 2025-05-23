package cash.z.wallet.sdk.sample.demoapp.test

import android.app.KeyguardManager
import android.content.Context
import android.os.PowerManager
import androidx.test.core.app.ApplicationProvider

/**
 * Subclass this in view unit and integration tests.  This verifies that
 * prerequisites necessary for reliable UI tests are met, and it provides more useful error messages.
 */
open class UiTestPrerequisites {
    fun verifyPrerequisites() {
        assertScreenIsOn()
        assertKeyguardIsUnlocked()
    }

    companion object {
        fun assertScreenIsOn() {
            if (!isScreenOn()) {
                throw AssertionError("Screen must be on for Android UI tests to run") // $NON-NLS
            }
        }

        private fun isScreenOn(): Boolean {
            val powerService =
                ApplicationProvider
                    .getApplicationContext<Context>()
                    .getSystemService(Context.POWER_SERVICE) as PowerManager
            return powerService.isInteractive
        }

        fun assertKeyguardIsUnlocked() {
            if (isKeyguardLocked()) {
                throw AssertionError("Device must be unlocked on for Android UI tests to run") // $NON-NLS
            }
        }

        private fun isKeyguardLocked(): Boolean {
            val keyguardService = (
                ApplicationProvider
                    .getApplicationContext<Context>()
                    .getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            )

            return keyguardService.isKeyguardLocked
        }
    }
}
