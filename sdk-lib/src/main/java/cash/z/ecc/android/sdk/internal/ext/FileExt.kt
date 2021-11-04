package cash.z.ecc.android.sdk.internal.ext

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

suspend fun File.deleteSuspend() = withContext(Dispatchers.IO) { delete() }