package cash.z.ecc.android.sdk.demoapp

import android.app.Application
import cash.z.ecc.android.sdk.internal.TroubleshootingTwig
import cash.z.ecc.android.sdk.internal.Twig

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            StrictModeHelper.enableStrictMode()
        }

        Twig.plant(TroubleshootingTwig())
    }
}
