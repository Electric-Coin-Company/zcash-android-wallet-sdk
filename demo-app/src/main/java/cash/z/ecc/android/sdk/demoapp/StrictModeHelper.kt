package cash.z.ecc.android.sdk.demoapp

import android.annotation.SuppressLint
import android.os.Build
import android.os.StrictMode
import cash.z.ecc.android.sdk.demoapp.util.AndroidApiVersion

object StrictModeHelper {
    fun enableStrictMode() {
        configureStrictMode()
    }

    @SuppressLint("NewApi")
    private fun configureStrictMode() {
        StrictMode.enableDefaults()

        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder().apply {
                detectAll()
                penaltyLog()
            }.build()
        )

        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder().apply {
                if (AndroidApiVersion.isAtLeastS) {
                    detectUnsafeIntentLaunch()
                }
                detectActivityLeaks()
                detectCleartextNetwork()
                detectContentUriWithoutPermission()
                detectFileUriExposure()
                detectLeakedClosableObjects()
                detectLeakedRegistrationObjects()
                detectLeakedSqlLiteObjects()
                if (AndroidApiVersion.isAtLeastP) {
                    // Disable because this is mostly flagging Android X and Play Services
                    // builder.detectNonSdkApiUsage();
                }
            }.build()
        )
    }
}
