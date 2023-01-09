package cash.z.ecc.android.sdk.demoapp

import androidx.multidex.MultiDexApplication
import cash.z.ecc.android.sdk.demoapp.util.Twig

class App : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()

        Twig.initialize(applicationContext)
        Twig.info { "Starting application…" }

        if (BuildConfig.DEBUG) {
            StrictModeHelper.enableStrictMode()

            // This is an internal API to the Zcash SDK to enable logging; it could change in the future
            cash.z.ecc.android.sdk.internal.Twig.enabled(true)
        } else {
            // In release builds, logs should be stripped by R8 rules
            Twig.assertLoggingStripped()
        }
    }
}
