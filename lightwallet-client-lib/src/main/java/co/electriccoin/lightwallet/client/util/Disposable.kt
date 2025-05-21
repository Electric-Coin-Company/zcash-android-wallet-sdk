package co.electriccoin.lightwallet.client.util

interface Disposable {
    suspend fun dispose()
}

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
