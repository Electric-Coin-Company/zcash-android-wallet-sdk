package cash.z.wallet.sdk.demoapp

import android.app.Application

class App : Application() {
    override fun onCreate() {
        instance = this
        super.onCreate()
    }

    companion object {
        lateinit var instance: App
    }
}