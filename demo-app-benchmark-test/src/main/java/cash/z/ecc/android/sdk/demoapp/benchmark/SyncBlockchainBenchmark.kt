package cash.z.ecc.android.sdk.demoapp.benchmark

import android.content.ComponentName
import android.content.Intent
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import cash.z.ecc.android.sdk.demoapp.test.UiTestPrerequisites
import cash.z.ecc.android.sdk.demoapp.test.clickAndWaitFor
import cash.z.ecc.android.sdk.demoapp.test.waitFor
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

// TODO [#809]: Enable macrobenchmark on CI
// TODO [#809]: https://github.com/zcash/zcash-android-wallet-sdk/issues/809

/**
 * This benchmark class provides measurements and captured custom traces for investigating SDK syncing mechanisms
 * with restricted blockchain range. It always resets the SDK before the next sync iteration. It uses UIAutomator to
 * navigate to the Balances screen, where the whole download -> validate -> scan -> enhance process is performed and
 * thus measured by this test.
 *
 * Run this benchmark from Android Studio only against the Benchmark build type set in all modules.
 *
 * We ideally run this on a physical device with Android SDK level 29, at least, as profiling is provided by this
 * version and later on.
 */
class SyncBlockchainBenchmark : UiTestPrerequisites() {
    companion object {
        private const val APP_TARGET_PACKAGE_NAME = "cash.z.ecc.android.sdk.demoapp.mainnet" // NON-NLS
        private const val APP_TARGET_ACTIVITY_NAME = "cash.z.ecc.android.sdk.demoapp.MainActivity" // NON-NLS

        private const val BALANCE_SCREEN_SECTION = "BALANCE_SCREEN" // NON-NLS
        private const val BLOCKCHAIN_SYNC_SECTION = "BLOCKCHAIN_SYNC" // NON-NLS
        private const val DOWNLOAD_SECTION = "DOWNLOAD" // NON-NLS
        private const val VALIDATION_SECTION = "VALIDATION" // NON-NLS
        private const val SCAN_SECTION = "SCAN" // NON-NLS
    }

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    /**
     * Advanced trace events test, which starts the Demo-app on the Home screen and then navigates to the Get Balance
     * screen. SDK sync phases with restricted blockchain range are measured during the overall sync mechanism here.
     */
    @Test
    @OptIn(ExperimentalMetricApi::class)
    fun tracesSyncBlockchain() =
        benchmarkRule.measureRepeated(
            packageName = APP_TARGET_PACKAGE_NAME,
            metrics =
                listOf(
                    TraceSectionMetric(
                        sectionName = BALANCE_SCREEN_SECTION,
                        mode = TraceSectionMetric.Mode.First,
                        targetPackageOnly = false
                    ),
                    TraceSectionMetric(
                        sectionName = BLOCKCHAIN_SYNC_SECTION,
                        mode = TraceSectionMetric.Mode.First,
                        targetPackageOnly = false
                    ),
                    TraceSectionMetric(
                        sectionName = DOWNLOAD_SECTION,
                        mode = TraceSectionMetric.Mode.First,
                        targetPackageOnly = false
                    ),
                    TraceSectionMetric(
                        sectionName = VALIDATION_SECTION,
                        mode = TraceSectionMetric.Mode.First,
                        targetPackageOnly = false
                    ),
                    TraceSectionMetric(
                        sectionName = SCAN_SECTION,
                        mode = TraceSectionMetric.Mode.First,
                        targetPackageOnly = false
                    )
                ),
            compilationMode = CompilationMode.Full(),
            startupMode = StartupMode.COLD,
            iterations = 3,
            measureBlock = {
                startLegacyActivityAndWait()
                resetSDK()
                gotoBalanceScreen()
                waitForBalanceScreen()
                closeBalanceScreen()
            }
        )

    // TODO [#808]: Add demo-ui-lib module (and reference the hardcoded texts here)
    // TODO [#808]: https://github.com/zcash/zcash-android-wallet-sdk/issues/808

    private fun MacrobenchmarkScope.resetSDK() {
        // Open toolbar overflow menu
        device.findObject(By.desc("More options")).clickAndWaitFor(Until.newWindow(), 2.seconds) // NON-NLS
        // Click on the reset sdk menu item
        device.findObject(By.text("Reset SDK")).clickAndWaitFor(Until.newWindow(), 2.seconds) // NON-NLS
        device.waitForIdle()
    }

    private fun MacrobenchmarkScope.waitForBalanceScreen() {
        device.waitFor(Until.hasObject(By.text("Status: SYNCED")), 5.minutes) // NON-NLS
    }

    private fun MacrobenchmarkScope.closeBalanceScreen() {
        // To close the Balance screen and disconnect from SDK Synchronizer
        device.pressBack()
    }

    private fun MacrobenchmarkScope.gotoBalanceScreen() {
        // Open drawer menu
        device.findObject(By.desc("Open navigation drawer")).clickAndWaitFor(Until.newWindow(), 2.seconds) // NON-NLS
        // Navigate to Balances screen
        device.findObject(By.text("Get Balance")).click() // NON-NLS
    }

    private fun MacrobenchmarkScope.startLegacyActivityAndWait() {
        val intent =
            Intent(Intent.ACTION_MAIN).apply {
                component = ComponentName(APP_TARGET_PACKAGE_NAME, APP_TARGET_ACTIVITY_NAME)
            }

        startActivityAndWait(intent)
    }
}
