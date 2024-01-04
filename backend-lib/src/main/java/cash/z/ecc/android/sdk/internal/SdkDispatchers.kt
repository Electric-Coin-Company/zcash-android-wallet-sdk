package cash.z.ecc.android.sdk.internal

import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

internal object SdkExecutors {
    /**
     * Executor used for database IO that's shared with the Rust native library.
     */
    /*
     * Based on internal discussion, keep the SDK internals confined to a single IO thread.
     *
     * We don't expect things to break, but we don't have the WAL enabled for SQLite so this
     * is a simple solution.
     */
    val DATABASE_IO =
        Executors.newSingleThreadExecutor {
            Thread(it, "zc-io").apply { isDaemon = true }
        }
}

object SdkDispatchers {
    /**
     * Dispatcher used for database IO that's shared with the Rust native library.
     */
    /*
     * Based on internal discussion, keep the SDK internals confined to a single IO thread.
     *
     * We don't expect things to break, but we don't have the WAL enabled for SQLite so this
     * is a simple solution.
     */
    // Don't use `Dispatchers.IO.limitedParallelism(1)`.
    // While it executes serially, each dispatch can be on a different thread.
    val DATABASE_IO = SdkExecutors.DATABASE_IO.asCoroutineDispatcher()
}
