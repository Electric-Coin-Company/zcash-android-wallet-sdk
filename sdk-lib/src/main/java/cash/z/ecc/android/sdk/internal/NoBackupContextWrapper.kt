package cash.z.ecc.android.sdk.internal

import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.annotation.RequiresApi
import java.io.File

/**
 * A context class wrapper used for building our database classes. The advantage of this implementation
 * is that we can control actions run on this context class. This is supposed to be used only for
 * Android SDK level 27 and higher. The Room's underlying SQLite has a different implementation of
 * SQLiteOpenHelper#getDatabaseLocked() and possibly other methods for Android SDK level 26 and lower.
 * Which at the end call ContextImpl#openOrCreateDatabase(), instead of the overridden getDatabasePath(),
 * and thus is not suitable for this custom context wrapper class.
 *
 * @param context
 * @param parentDir The directory in which is the database file placed.
 * @return Wrapped context class.
 */
@RequiresApi(Build.VERSION_CODES.O_MR1)
internal class NoBackupContextWrapper(
    context: Context,
    private val parentDir: File
) : ContextWrapper(context.applicationContext) {

    /**
     * Overriding this function gives us ability to control the result database file location.
     *
     * @param name Database file name.
     * @return File located under no_backup/co.electricoin.zcash directory.
     */
    override fun getDatabasePath(name: String): File {
        twig("Database: $name in directory: ${parentDir.absolutePath}")
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
