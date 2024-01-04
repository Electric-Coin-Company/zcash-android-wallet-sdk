package cash.z.ecc.android.sdk.test

import android.content.Context
import androidx.annotation.StringRes
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlin.test.assertNotNull

fun getAppContext(): Context = ApplicationProvider.getApplicationContext()

fun getStringResource(
    @StringRes resId: Int
) = getAppContext().getString(resId)

fun getStringResourceWithArgs(
    @StringRes resId: Int,
    vararg formatArgs: String
) = getAppContext().getString(resId, *formatArgs)

fun readFileLinesInFlow(filePathName: String) =
    flow<String> {
        val testFile = javaClass.getResourceAsStream(filePathName)
        assertNotNull(testFile, "Test file read failure.")

        emitAll(testFile.bufferedReader().lineSequence().asFlow())
    }.flowOn(Dispatchers.IO)
