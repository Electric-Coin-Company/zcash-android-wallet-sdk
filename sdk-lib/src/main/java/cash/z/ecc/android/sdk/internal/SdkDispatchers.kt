package cash.z.ecc.android.sdk.internal

import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

internal object SdkDispatchers {
    /*
     * Based on internal discussion, keep the SDK internals confined to a single IO thread.
     *
     * We don't expect things to break, but we don't have the WAL enabled for SQLite so this
     * is a simple solution.
     */
    val IO = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
}