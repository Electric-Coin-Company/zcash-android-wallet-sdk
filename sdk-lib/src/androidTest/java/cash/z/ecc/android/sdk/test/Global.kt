package cash.z.ecc.android.sdk.test

import android.content.Context
import androidx.annotation.StringRes
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlin.test.assertNotNull

fun getAppContext(): Context = ApplicationProvider.getApplicationContext()

fun getStringResource(@StringRes resId: Int) = getAppContext().getString(resId)

fun getStringResourceWithArgs(@StringRes resId: Int, vararg formatArgs: String) = getAppContext().getString(resId, *formatArgs)

fun readLinesInFlow(filePathName: String) = callbackFlow {
    val seedFile = javaClass.getResourceAsStream(filePathName)
    assertNotNull(seedFile, "Test seed file read failure.")

    val reader = seedFile.bufferedReader()
    reader.forEachLine { line ->
        trySend(line)
    }.also {
        close()
    }
    awaitClose { /* nothing to close here */ }
}
