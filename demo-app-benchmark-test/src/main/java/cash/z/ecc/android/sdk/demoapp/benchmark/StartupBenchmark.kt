package cash.z.ecc.android.sdk.demoapp.benchmark

import android.content.ComponentName
import android.content.Intent
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import cash.z.ecc.android.sdk.demoapp.test.UiTestPrerequisites
import cash.z.ecc.android.sdk.demoapp.test.clickAndWaitFor
import cash.z.ecc.android.sdk.demoapp.test.waitFor
import org.junit.Rule
import org.junit.Test
import java.util.regex.Pattern
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

// TODO [#809]: Enable macrobenchmark on CI
// TODO [#809]: https://github.com/zcash/zcash-android-wallet-sdk/issues/809

/**
 * Purpose of this class is to provide a basic startup measurements, and captured system traces for investigating the
 * app's performance. It navigates to the device's home screen, and launches the default activity.
 *
 * Run this benchmark from Android Studio only against the Benchmark build type set in all modules.
 *
 * We ideally run this against a physical device with Android SDK level 29, at least, as profiling is provided by this
 * version and later on.
 */
class StartupBenchmark : UiTestPrerequisites() {
    companion object {
        private const val APP_TARGET_PACKAGE_NAME = "cash.z.ecc.android.sdk.demoapp.mainnet" // NON-NLS
        private const val APP_TARGET_ACTIVITY_NAME = "cash.z.ecc.android.sdk.demoapp.MainActivity" // NON-NLS

        private const val ADDRESS_SCREEN_SECTION = "ADDRESS_SCREEN" // NON-NLS
        private const val UNIFIED_ADDRESS_SECTION = "UNIFIED_ADDRESS" // NON-NLS
        private const val SAPLING_ADDRESS_SECTION = "SAPLING_ADDRESS" // NON-NLS
        private const val TRANSPARENT_ADDRESS_SECTION = "TRANSPARENT_ADDRESS" // NON-NLS
    }

    private val unifiedAddressPattern = "^u1[a-z0-9]{211}$".toPattern() // NON-NLS
    private val saplingAddressPattern = "^[a-z0-9]{78}$".toPattern() // NON-NLS
    private val transparentAddressPattern = "^[a-zA-Z0-9]{35}$".toPattern() // NON-NLS

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    /**
     * This test starts the Demo-app on Home screen and measures its metrics.
     */
    @Test
    fun appStartup() =
        benchmarkRule.measureRepeated(
            packageName = APP_TARGET_PACKAGE_NAME,
            metrics = listOf(StartupTimingMetric()),
            iterations = 5,
            startupMode = StartupMode.COLD,
            setupBlock = {
                // Press home button before each run to ensure the starting activity isn't visible
                pressHome()
            }
        ) {
            startLegacyActivityAndWait()
        }

    /**
     * Advanced trace events startup test, which starts the Demo-app on the Home screen and then navigates to the
     * Get Address screen. Logic for providing addresses from SDK is measured here.
     */
    @Test
    @OptIn(ExperimentalMetricApi::class)
    fun tracesSdkStartup() =
        benchmarkRule.measureRepeated(
            packageName = APP_TARGET_PACKAGE_NAME,
            metrics =
                listOf(
                    TraceSectionMetric(
                        sectionName = ADDRESS_SCREEN_SECTION,
                        mode = TraceSectionMetric.Mode.First,
                        targetPackageOnly = false
                    ),
                    TraceSectionMetric(
                        sectionName = UNIFIED_ADDRESS_SECTION,
                        mode = TraceSectionMetric.Mode.First,
                        targetPackageOnly = false
                    ),
                    TraceSectionMetric(
                        sectionName = SAPLING_ADDRESS_SECTION,
                        mode = TraceSectionMetric.Mode.First,
                        targetPackageOnly = false
                    ),
                    TraceSectionMetric(
                        sectionName = TRANSPARENT_ADDRESS_SECTION,
                        mode = TraceSectionMetric.Mode.First,
                        targetPackageOnly = false
                    )
                ),
            compilationMode = CompilationMode.Full(),
            startupMode = StartupMode.COLD,
            iterations = 5,
            measureBlock = {
                startLegacyActivityAndWait()
                gotoAddressScreen()
                waitForAddressScreen()
                closeAddressScreen()
            }
        )

    private fun MacrobenchmarkScope.closeAddressScreen() {
        // To close the Address screen and disconnect from SDK Synchronizer
        device.pressBack()
    }

    private fun MacrobenchmarkScope.waitForAddressScreen() {
        val timeoutSeconds = 5.seconds
        check(
            waitForAddressAppear(unifiedAddressPattern, timeoutSeconds) &&
                waitForAddressAppear(saplingAddressPattern, timeoutSeconds) &&
                waitForAddressAppear(transparentAddressPattern, timeoutSeconds)
        ) {
            "Some of the addresses didn't appear before $timeoutSeconds seconds timeout."
        }
    }

    private fun MacrobenchmarkScope.waitForAddressAppear(
        addressPattern: Pattern,
        timeout: Duration
    ): Boolean = device.waitFor(Until.hasObject(By.text(addressPattern)), timeout)

    // TODO [#808]: Add demo-ui-lib module (and reference the hardcoded texts here)
    // TODO [#808]: https://github.com/zcash/zcash-android-wallet-sdk/issues/808

    private fun MacrobenchmarkScope.gotoAddressScreen() {
        // Open drawer menu
        device
            .findObject(By.desc("Open navigation drawer")) // NON-NLS
            .clickAndWaitFor(Until.newWindow(), 2.seconds)
        // Navigate to Addresses screen
        device.findObject(By.text("Get Address")).click() // NON-NLS
    }

    private fun MacrobenchmarkScope.startLegacyActivityAndWait() {
        val intent =
            Intent(Intent.ACTION_MAIN).apply {
                component = ComponentName(APP_TARGET_PACKAGE_NAME, APP_TARGET_ACTIVITY_NAME)
            }

        startActivityAndWait(intent)
    }
}
