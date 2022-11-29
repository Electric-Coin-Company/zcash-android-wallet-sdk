package cash.z.ecc.android.sdk.jni

import cash.z.ecc.android.sdk.internal.twig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.measureTimeMillis

/**
 * Loads a native library once.  This class is thread-safe.
 *
 * To use this class, create a singleton instance for each given [libraryName].
 *
 * @param libraryName Name of the library to load.
 */
internal class NativeLibraryLoader(private val libraryName: String) {
    private val isLoaded = AtomicBoolean(false)
    private val mutex = Mutex()

    suspend fun load() {
        // Double-checked locking to avoid the Mutex unless necessary, as the hot path is
        // for the library to be loaded since this should only run once for the lifetime
        // of the application
        if (!isLoaded.get()) {
            mutex.withLock {
                if (!isLoaded.get()) {
                    loadRustLibrary()
                }
            }
        }
    }

    private suspend fun loadRustLibrary() {
        runCatching {
            twig("Loading native library $libraryName")

            val loadTimeMillis = measureTimeMillis {
                loadLibrarySuspend(libraryName)
            }

            twig("Loading native library took $loadTimeMillis milliseconds")

            isLoaded.set(true)
        }.onFailure {
            // Fail fast, because this is not a recoverable error
            throw AssertionError("Failed loading native library $libraryName", it)
        }
    }
}

private suspend fun loadLibrarySuspend(libraryName: String) = withContext(Dispatchers.IO) {
    System.loadLibrary(libraryName)
}
