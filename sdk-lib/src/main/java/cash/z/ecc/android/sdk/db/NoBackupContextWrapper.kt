package cash.z.ecc.android.sdk.db

import android.content.Context
import android.content.ContextWrapper
import cash.z.ecc.android.sdk.internal.twig
import java.io.File

class NoBackupContextWrapper(val context: Context) : ContextWrapper(context.applicationContext) {

    /**
     * Overriding this function gives us ability to control the result database file location.
     * We extend the input parameter with an additional package name path inside the result database
     * file.
     *
     * @param name Database file name with the full absolute path to the file
     * @return File located under [Context.getNoBackupFilesDir].
     */
    override fun getDatabasePath(name: String): File {
        twig("Database: $name")
        return File(name)
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
