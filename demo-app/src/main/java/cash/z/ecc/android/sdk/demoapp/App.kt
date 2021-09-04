package cash.z.ecc.android.sdk.demoapp

import android.app.Application
import cash.z.ecc.android.sdk.ext.TroubleshootingTwig
import cash.z.ecc.android.sdk.ext.Twig

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        Twig.plant(TroubleshootingTwig())
    }
}
