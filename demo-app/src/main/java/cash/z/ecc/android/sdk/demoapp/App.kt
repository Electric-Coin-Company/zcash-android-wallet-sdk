package cash.z.ecc.android.sdk.demoapp

import androidx.multidex.MultiDexApplication
import cash.z.ecc.android.sdk.internal.Twig

class App : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()

        Twig.initialize(applicationContext)
        Twig.info { "Starting application…" }

        if (BuildConfig.DEBUG) {
            StrictModeHelper.enableStrictMode()
        } else {
            // In release builds, logs should be stripped by R8 rules
            Twig.assertLoggingStripped()
        }
    }
}
