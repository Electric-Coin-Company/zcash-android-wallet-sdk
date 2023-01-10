package cash.z.ecc.android.sdk.demoapp.util

import android.os.Looper
import androidx.tracing.Trace
import cash.z.ecc.android.sdk.internal.twig
import co.electriccoin.lightwallet.client.ext.BenchmarkingExt

interface BenchmarkTrace {
    fun checkMainThread() {
        check(Looper.getMainLooper().thread === Thread.currentThread()) {
            "Should be called from the main thread, not ${Thread.currentThread()}."
        }
    }

    interface Event {
        val section: String
        val cookie: Int
    }
}

object SyncBlockchainBenchmarkTrace : BenchmarkTrace {
    fun writeEvent(event: BenchmarkTrace.Event?) {
        twig("New SyncBlockchain event: $event arrived.")
        if (!BenchmarkingExt.isBenchmarking()) {
            return
        }
        checkMainThread()
        when (event) {
            Event.BALANCE_SCREEN_START -> {
                Trace.beginAsyncSection(Event.BALANCE_SCREEN_START.section, Event.BALANCE_SCREEN_START.cookie)
            }
            Event.BALANCE_SCREEN_END -> {
                Trace.endAsyncSection(Event.BALANCE_SCREEN_END.section, Event.BALANCE_SCREEN_END.cookie)
            }
            Event.BLOCKCHAIN_SYNC_START -> {
                Trace.beginAsyncSection(Event.BLOCKCHAIN_SYNC_START.section, Event.BLOCKCHAIN_SYNC_START.cookie)
            }
            Event.BLOCKCHAIN_SYNC_END -> {
                Trace.endAsyncSection(Event.BLOCKCHAIN_SYNC_END.section, Event.BLOCKCHAIN_SYNC_END.cookie)
            }
            Event.DOWNLOAD_START -> {
                Trace.beginAsyncSection(Event.DOWNLOAD_START.section, Event.DOWNLOAD_START.cookie)
            }
            Event.DOWNLOAD_END -> {
                Trace.endAsyncSection(Event.DOWNLOAD_END.section, Event.DOWNLOAD_END.cookie)
            }
            Event.VALIDATION_START -> {
                Trace.beginAsyncSection(Event.VALIDATION_START.section, Event.VALIDATION_START.cookie)
            }
            Event.VALIDATION_END -> {
                Trace.endAsyncSection(Event.VALIDATION_END.section, Event.VALIDATION_END.cookie)
            }
            Event.SCAN_START -> {
                Trace.beginAsyncSection(Event.SCAN_START.section, Event.SCAN_START.cookie)
            }
            Event.SCAN_END -> {
                Trace.endAsyncSection(Event.SCAN_END.section, Event.SCAN_END.cookie)
            }
            else -> { /* nothing to write */ }
        }
    }

    @Suppress("MagicNumber")
    enum class Event : BenchmarkTrace.Event {
        BALANCE_SCREEN_START {
            override val section: String = "BALANCE_SCREEN" // NON-NLS
            override val cookie: Int = 100
        },
        BALANCE_SCREEN_END {
            override val section: String = "BALANCE_SCREEN" // NON-NLS
            override val cookie: Int = 100
        },
        BLOCKCHAIN_SYNC_START {
            override val section: String = "BLOCKCHAIN_SYNC" // NON-NLS
            override val cookie: Int = 200
        },
        BLOCKCHAIN_SYNC_END {
            override val section: String = "BLOCKCHAIN_SYNC" // NON-NLS
            override val cookie: Int = 200
        },
        DOWNLOAD_START {
            override val section: String = "DOWNLOAD" // NON-NLS
            override val cookie: Int = 300
        },
        DOWNLOAD_END {
            override val section: String = "DOWNLOAD" // NON-NLS
            override val cookie: Int = 300
        },
        VALIDATION_START {
            override val section: String = "VALIDATION" // NON-NLS
            override val cookie: Int = 400
        },
        VALIDATION_END {
            override val section: String = "VALIDATION" // NON-NLS
            override val cookie: Int = 400
        },
        SCAN_START {
            override val section: String = "SCAN" // NON-NLS
            override val cookie: Int = 500
        },
        SCAN_END {
            override val section: String = "SCAN" // NON-NLS
            override val cookie: Int = 500
        }
    }
}

object ProvideAddressBenchmarkTrace : BenchmarkTrace {
    fun writeEvent(event: BenchmarkTrace.Event?) {
        twig("New ProvideAddress event: $event arrived.")
        if (!BenchmarkingExt.isBenchmarking()) {
            return
        }
        checkMainThread()
        when (event) {
            Event.ADDRESS_SCREEN_START -> {
                Trace.beginAsyncSection(Event.ADDRESS_SCREEN_START.section, Event.ADDRESS_SCREEN_START.cookie)
            }
            Event.ADDRESS_SCREEN_END -> {
                Trace.endAsyncSection(Event.ADDRESS_SCREEN_END.section, Event.ADDRESS_SCREEN_END.cookie)
            }
            Event.UNIFIED_ADDRESS_START -> {
                Trace.beginAsyncSection(Event.UNIFIED_ADDRESS_START.section, Event.UNIFIED_ADDRESS_START.cookie)
            }
            Event.UNIFIED_ADDRESS_END -> {
                Trace.endAsyncSection(Event.UNIFIED_ADDRESS_END.section, Event.UNIFIED_ADDRESS_END.cookie)
            }
            Event.SAPLING_ADDRESS_START -> {
                Trace.beginAsyncSection(Event.SAPLING_ADDRESS_START.section, Event.SAPLING_ADDRESS_START.cookie)
            }
            Event.SAPLING_ADDRESS_END -> {
                Trace.endAsyncSection(Event.SAPLING_ADDRESS_END.section, Event.SAPLING_ADDRESS_END.cookie)
            }
            Event.TRANSPARENT_ADDRESS_START -> {
                Trace.beginAsyncSection(
                    Event.TRANSPARENT_ADDRESS_START.section,
                    Event.TRANSPARENT_ADDRESS_START.cookie
                )
            }
            Event.TRANSPARENT_ADDRESS_END -> {
                Trace.endAsyncSection(Event.TRANSPARENT_ADDRESS_END.section, Event.TRANSPARENT_ADDRESS_END.cookie)
            }
            else -> { /* nothing to write */ }
        }
    }

    @Suppress("MagicNumber")
    enum class Event : BenchmarkTrace.Event {
        ADDRESS_SCREEN_START {
            override val section: String = "ADDRESS_SCREEN" // NON-NLS
            override val cookie: Int = 100
        },
        ADDRESS_SCREEN_END {
            override val section: String = "ADDRESS_SCREEN" // NON-NLS
            override val cookie: Int = 100
        },
        UNIFIED_ADDRESS_START {
            override val section: String = "UNIFIED_ADDRESS" // NON-NLS
            override val cookie: Int = 200
        },
        UNIFIED_ADDRESS_END {
            override val section: String = "UNIFIED_ADDRESS" // NON-NLS
            override val cookie: Int = 200
        },
        SAPLING_ADDRESS_START {
            override val section: String = "SAPLING_ADDRESS" // NON-NLS
            override val cookie: Int = 300
        },
        SAPLING_ADDRESS_END {
            override val section: String = "SAPLING_ADDRESS" // NON-NLS
            override val cookie: Int = 300
        },
        TRANSPARENT_ADDRESS_START {
            override val section: String = "TRANSPARENT_ADDRESS" // NON-NLS
            override val cookie: Int = 400
        },
        TRANSPARENT_ADDRESS_END {
            override val section: String = "TRANSPARENT_ADDRESS" // NON-NLS
            override val cookie: Int = 400
        }
    }
}
