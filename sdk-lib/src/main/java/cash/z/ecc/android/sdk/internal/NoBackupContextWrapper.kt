package cash.z.ecc.android.sdk.internal

import android.content.Context
import android.content.ContextWrapper
import java.io.File

/**
 * Context class wrapped used for building our database classes. The advantage of this implementation
 * is that we can control actions run on this context class.
 *
 * @param context
 * @param parentDir The directory in which is the database file placed.
 * @return Wrapped context class.
 */
internal class NoBackupContextWrapper(
    context: Context,
    private val parentDir: File?
) : ContextWrapper(context.applicationContext) {

    /**
     * Overriding this function gives us ability to control the result database file location.
     *
     * @param name Database file name.
     * @return File located under no_backup/co.electricoin.zcash directory.
     */
    override fun getDatabasePath(name: String): File {
        twig("Database: $name in directory: ${parentDir?.absolutePath}")
        assert(parentDir != null) { "Null database parent file." }
        return File(parentDir, name)
    }

    override fun getApplicationContext(): Context {
        // Prevent breakout
        return this
    }

    override fun getBaseContext(): Context {
        // Prevent breakout
        return this
    }
}
