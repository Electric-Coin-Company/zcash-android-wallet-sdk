package cash.z.wallet.sdk.demoapp

import android.app.Application

class App : Application() {

    var defaultConfig = DemoConfig()

    override fun onCreate() {
        instance = this
        super.onCreate()
    }

    companion object {
        lateinit var instance: App
    }
}