package cash.z.ecc.android.sdk.demoapp.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

/**
 * Purpose of this class is to provide a basic startup measurements, and captured system traces for investigating the
 * app's performance. It navigates to the device's home screen, and launches the default activity.
 *
 * Run this benchmark from Android Studio only against the Benchmark build type set in all modules.
 *
 * We ideally run this against a physical device with Android SDK level 29, at least, as profiling is provided by this
 * version and later on.
 */
class StartupBenchmark {

    companion object {
        private const val APP_TARGET_PACKAGE_NAME = "cash.z.ecc.android.sdk.demoapp.mainnet"    // NON-NLS

        private const val ADDRESS_SCREEN_SECTION = "ADDRESS_SCREEN" // NON-NLS
        private const val UNIFIED_ADDRESS_SECTION = "UNIFIED_ADDRESS"   // NON-NLS
        private const val SAPLING_ADDRESS_SECTION = "SAPLING_ADDRESS"   // NON-NLS
        private const val TRANSPARENT_ADDRESS_SECTION = "TRANSPARENT_ADDRESS"   // NON-NLS
    }

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    /**
     * This test starts the Demo-app on Home screen and measures its metrics.
     */
    @Test
    fun appStartup() = benchmarkRule.measureRepeated(
        packageName = APP_TARGET_PACKAGE_NAME,
        metrics = listOf(StartupTimingMetric()),
        iterations = 5,
        startupMode = StartupMode.COLD,
        setupBlock = {
            // Press home button before each run to ensure the starting activity isn't visible
            pressHome()
        }
    ) {
        startActivityAndWait()
    }

    /**
     * Advanced trace events startup test, which starts the Demo-app on the Home screen and then navigates to the
     * Get Address screen. Logic for providing addresses from SDK is measured here.
     */
    @Test
    @OptIn(ExperimentalMetricApi::class)
    fun tracesSdkStartup() = benchmarkRule.measureRepeated(
        packageName = APP_TARGET_PACKAGE_NAME,
        metrics = listOf(
            TraceSectionMetric(sectionName = ADDRESS_SCREEN_SECTION, mode = TraceSectionMetric.Mode.First),
            TraceSectionMetric(sectionName = UNIFIED_ADDRESS_SECTION, mode = TraceSectionMetric.Mode.First),
            TraceSectionMetric(sectionName = SAPLING_ADDRESS_SECTION, mode = TraceSectionMetric.Mode.First),
            TraceSectionMetric(sectionName = TRANSPARENT_ADDRESS_SECTION, mode = TraceSectionMetric.Mode.First)
        ),
        compilationMode = CompilationMode.Full(),
        startupMode = StartupMode.COLD,
        iterations = 5,
        measureBlock = {
            startActivityAndWait()
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
            device.run {
                val ua = Until.hasObject(By.text("^[a-z0-9]{141}$".toPattern())) // NON-NLS (Unified address condition)
                val sa = Until.hasObject(By.text("^[a-z0-9]{78}$".toPattern())) // NON-NLS (Sapling address condition)
                val ta = Until.hasObject(By.text("^[a-zA-Z0-9]{35}$".toPattern())) // NON-NLS (Transparent address)
                // condition

                wait(ua, timeoutSeconds.inWholeMilliseconds) != null &&
                    wait(sa, timeoutSeconds.inWholeMilliseconds) &&
                    wait(ta, timeoutSeconds.inWholeMilliseconds)
            }
        ) {
            "Some of the addresses didn't show before $timeoutSeconds seconds timeout."
        }
    }

    private fun MacrobenchmarkScope.gotoAddressScreen() {
        // Open drawer menu
        device.findObject(By.desc("Open navigation drawer"))    // NON-NLS
            .clickAndWait(Until.newWindow(), 2.seconds.inWholeMilliseconds)
        // Navigate to Addresses screen
        device.findObject(By.text("Get Address")).click()   // NON-NLS
    }
}
