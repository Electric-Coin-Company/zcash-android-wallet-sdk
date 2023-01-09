package cash.z.ecc.android.sdk.demoapp.ui.common

import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.lifecycleScope
import cash.z.ecc.android.sdk.ext.collectWith
import kotlinx.coroutines.flow.map

@Composable
fun ComponentActivity.BindCompLocalProvider(content: @Composable () -> Unit) {
    val screenTimeout = ScreenTimeout()
    observeScreenTimeoutFlag(screenTimeout)

    CompositionLocalProvider(
        LocalScreenTimeout provides screenTimeout,
        content = content
    )
}

private fun ComponentActivity.observeScreenTimeoutFlag(screenTimeout: ScreenTimeout) {
    screenTimeout.referenceCount.map { it > 0 }.collectWith(lifecycleScope) { disableTimeout ->
        if (disableTimeout) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}
