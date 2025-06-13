package cash.z.ecc.android.sdk.internal.metrics

import android.os.Build
import androidx.tracing.Trace
import cash.z.ecc.android.sdk.internal.Twig
import kotlin.random.Random

class TraceScope {
    private val methodName: String
    private var cookie: Int?

    constructor(methodName: String) {
        val cookie = Random.nextInt()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Trace.beginAsyncSection(methodName, cookie)
        } else {
            // Best-effort for older Android versions.
            Trace.beginSection(methodName)
        }
        this.methodName = methodName
        this.cookie = cookie
    }

    fun end() {
        // Ensure we do not end a section twice.
        if (cookie == null) {
            Twig.warn { "TraceScope for $methodName ended more than once!" }
        }
        cookie?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Trace.endAsyncSection(methodName, it)
            } else {
                Trace.endSection()
            }
        }
        cookie = null
    }
}

inline fun <T> withTraceScope(name: String, block: () -> T): T {
    val scope = TraceScope(name)
    try {
        return block()
    } finally {
        scope.end()
    }
}
