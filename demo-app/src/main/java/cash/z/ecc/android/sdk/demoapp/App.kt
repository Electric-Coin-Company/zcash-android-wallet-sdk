package cash.z.ecc.android.sdk.demoapp

import android.app.Application
import cash.z.ecc.android.sdk.ext.TroubleshootingTwig
import cash.z.ecc.android.sdk.ext.Twig

class App : Application() {

    var defaultConfig = DemoConfig()

    override fun onCreate() {
        instance = this
        super.onCreate()
        Twig.plant(TroubleshootingTwig())
    }

    companion object {
        lateinit var instance: App
    }
}
