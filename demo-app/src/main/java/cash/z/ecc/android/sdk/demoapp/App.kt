package cash.z.ecc.android.sdk.demoapp

import androidx.multidex.MultiDexApplication
import cash.z.ecc.android.sdk.internal.TroubleshootingTwig
import cash.z.ecc.android.sdk.internal.Twig

class App : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            StrictModeHelper.enableStrictMode()
        }

        Twig.plant(TroubleshootingTwig())
    }
}
