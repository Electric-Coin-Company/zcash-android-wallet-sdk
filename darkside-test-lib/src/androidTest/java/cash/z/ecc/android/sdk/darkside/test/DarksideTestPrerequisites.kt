package cash.z.ecc.android.sdk.darkside.test

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.junit.Before

/**
 * Subclass this to validate the environment for running Darkside tests.
 */
open class DarksideTestPrerequisites {
    @Before
    fun verifyEmulator() {
        require(isProbablyEmulator(ApplicationProvider.getApplicationContext())) {
            "Darkside tests are configured to only run on the Android Emulator.  Please see https://github" +
                ".com/zcash/zcash-android-wallet-sdk/blob/master/docs/testing/Darkside.md"
        }
    }

    companion object {
        private fun isProbablyEmulator(context: Context): Boolean {
            if (isDebuggable(context)) {
                // This is imperfect and could break in the future
                if (null == Build.DEVICE ||
                    "generic" == Build.DEVICE || // $NON-NLS
                    ("generic_x86" == Build.DEVICE) // $NON-NLS
                ) {
                    return true
                }
            }

            return false
        }

        /**
         * @return Whether the application running is debuggable.  This is determined from the
         * ApplicationInfo object (`BuildInfo` is useless for libraries.)
         */
        private fun isDebuggable(context: Context): Boolean {
            val packageInfo =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.getPackageInfo(
                        context.packageName,
                        PackageManager.PackageInfoFlags.of(0L)
                    )
                } else {
                    @Suppress("Deprecation")
                    context.packageManager.getPackageInfo(context.packageName, 0)
                }

            // Normally shouldn't be null, but could be with a MockContext
            return packageInfo.applicationInfo?.let {
                0 != (it.flags and ApplicationInfo.FLAG_DEBUGGABLE)
            } ?: false
        }
    }
}
