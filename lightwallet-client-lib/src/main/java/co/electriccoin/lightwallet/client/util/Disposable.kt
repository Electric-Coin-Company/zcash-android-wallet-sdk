package co.electriccoin.lightwallet.client.util

import java.io.IOException

/**
 * A [Disposable] is an object that can be closed/disposed.
 * The dispose method is invoked to release resources that the object is holding (such as open files).
 */
interface Disposable {
    /**
     * Closes this object and releases any system resources associated
     * with it. If the object is already closed then invoking this
     * method has no effect.
     *
     * In cases where the close may fail require careful attention. It is strongly advised
     * to relinquish the underlying resources and to internally the [Disposable] as closed, prior to throwing
     * the [IOException].
     *
     * @throws IOException if an I/O error occurs
     */
    @Throws(IOException::class)
    suspend fun dispose()
}

/**
 * Executes the given [block] function on this object and then closes it down correctly whether an exception
 * is thrown or not.
 *
 * @param block a function to process this [Disposable] object.
 * @return the result of [block] function invoked on this object.
 */
suspend inline fun <T : Disposable?, R> T.use(block: (T) -> R): R {
    try {
        return block(this)
    } finally {
        try {
            this?.dispose()
        } catch (_: Throwable) {
            // ignored
        }
    }
}
