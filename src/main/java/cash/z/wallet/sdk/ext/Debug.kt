package cash.z.wallet.sdk.ext

import android.util.Log

internal fun debug(message: String) {
    try {
        Log.e("DBUG", message)
    } catch(t: Throwable) {
        System.err.println(message)
    }
}