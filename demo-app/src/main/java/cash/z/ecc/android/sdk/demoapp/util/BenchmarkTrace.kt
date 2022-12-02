package cash.z.ecc.android.sdk.demoapp.util

import android.os.Looper
import androidx.tracing.Trace
import cash.z.ecc.android.sdk.ext.BenchmarkingExt
import cash.z.ecc.android.sdk.internal.twig
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface BenchmarkTrace {
    fun checkMainThread() {
        check(Looper.getMainLooper().thread === Thread.currentThread()) {
            "Should be called from the main thread, not ${Thread.currentThread()}."
        }
    }

    val writeEventMutex: Mutex
        get() = Mutex()

    interface Event
}

object SyncBlockchainBenchmarkTrace : BenchmarkTrace {
    private const val SECTION_BALANCE_SCREEN = "BALANCE_SCREEN" // NON-NLS
    private const val SECTION_BLOCKCHAIN_SYNC = "BLOCKCHAIN_SYNC"   // NON-NLS
    private const val SECTION_DOWNLOAD = "DOWNLOAD" // NON-NLS
    private const val SECTION_VALIDATION = "VALIDATION" // NON-NLS
    private const val SECTION_SCAN = "SCAN" // NON-NLS

    @Suppress("MagicNumber")
    suspend fun writeEvent(event: BenchmarkTrace.Event?) {
        twig("New SyncBlockchain event: $event arrived.")
        if (!BenchmarkingExt.isBenchmarking()) {
            return
        }
        writeEventMutex.withLock {
            checkMainThread()
            when (event) {
                Event.BALANCE_SCREEN_START -> Trace.beginAsyncSection(SECTION_BALANCE_SCREEN, 100)
                Event.BALANCE_SCREEN_END -> Trace.endAsyncSection(SECTION_BALANCE_SCREEN, 100)
                Event.BLOCKCHAIN_SYNC_START -> Trace.beginAsyncSection(SECTION_BLOCKCHAIN_SYNC, 200)
                Event.BLOCKCHAIN_SYNC_END -> Trace.endAsyncSection(SECTION_BLOCKCHAIN_SYNC, 200)
                Event.DOWNLOAD_START -> Trace.beginAsyncSection(SECTION_DOWNLOAD, 300)
                Event.DOWNLOAD_END -> Trace.endAsyncSection(SECTION_DOWNLOAD, 300)
                Event.VALIDATION_START -> Trace.beginAsyncSection(SECTION_VALIDATION, 400)
                Event.VALIDATION_END -> Trace.endAsyncSection(SECTION_VALIDATION, 400)
                Event.SCAN_START -> Trace.beginAsyncSection(SECTION_SCAN, 500)
                Event.SCAN_END -> Trace.endAsyncSection(SECTION_SCAN, 500)
                else -> { /* nothing to write */ }
            }
        }
    }

    enum class Event : BenchmarkTrace.Event {
        BALANCE_SCREEN_START,
        BALANCE_SCREEN_END,
        BLOCKCHAIN_SYNC_START,
        BLOCKCHAIN_SYNC_END,
        DOWNLOAD_START,
        DOWNLOAD_END,
        VALIDATION_START,
        VALIDATION_END,
        SCAN_START,
        SCAN_END
    }
}

object ProvideAddressBenchmarkTrace : BenchmarkTrace {
    private const val ADDRESS_SCREEN_SECTION = "ADDRESS_SCREEN" // NON-NLS
    private const val UNIFIED_ADDRESS_SECTION = "UNIFIED_ADDRESS"   // NON-NLS
    private const val SAPLING_ADDRESS_SECTION = "SAPLING_ADDRESS"   // NON-NLS
    private const val TRANSPARENT_ADDRESS_SECTION = "TRANSPARENT_ADDRESS"   // NON-NLS

    @Suppress("MagicNumber")
    suspend fun writeEvent(event: BenchmarkTrace.Event?) {
        twig("New ProvideAddress event: $event arrived.")
        if (!BenchmarkingExt.isBenchmarking()) {
            return
        }
        writeEventMutex.withLock {
            checkMainThread()
            when (event) {
                Event.ADDRESS_SCREEN_START -> Trace.beginAsyncSection(ADDRESS_SCREEN_SECTION, 100)
                Event.ADDRESS_SCREEN_END -> Trace.endAsyncSection(ADDRESS_SCREEN_SECTION, 100)
                Event.UNIFIED_ADDRESS_START -> Trace.beginAsyncSection(UNIFIED_ADDRESS_SECTION, 200)
                Event.UNIFIED_ADDRESS_END -> Trace.endAsyncSection(UNIFIED_ADDRESS_SECTION, 200)
                Event.SAPLING_ADDRESS_START -> Trace.beginAsyncSection(SAPLING_ADDRESS_SECTION, 300)
                Event.SAPLING_ADDRESS_END -> Trace.endAsyncSection(SAPLING_ADDRESS_SECTION, 300)
                Event.TRANSPARENT_ADDRESS_START -> Trace.beginAsyncSection(TRANSPARENT_ADDRESS_SECTION, 400)
                Event.TRANSPARENT_ADDRESS_END -> Trace.endAsyncSection(TRANSPARENT_ADDRESS_SECTION, 400)
                else -> { /* nothing to write */ }
            }
        }
    }

    enum class Event : BenchmarkTrace.Event {
        ADDRESS_SCREEN_START,
        ADDRESS_SCREEN_END,
        UNIFIED_ADDRESS_START,
        UNIFIED_ADDRESS_END,
        SAPLING_ADDRESS_START,
        SAPLING_ADDRESS_END,
        TRANSPARENT_ADDRESS_START,
        TRANSPARENT_ADDRESS_END
    }
}
