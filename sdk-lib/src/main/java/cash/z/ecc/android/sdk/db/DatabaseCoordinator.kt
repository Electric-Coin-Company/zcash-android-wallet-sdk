package cash.z.ecc.android.sdk.db

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.room.Room
import androidx.room.RoomDatabase
import cash.z.ecc.android.sdk.exception.InitializerException
import cash.z.ecc.android.sdk.internal.AndroidApiVersion
import cash.z.ecc.android.sdk.internal.Files
import cash.z.ecc.android.sdk.internal.LazyWithArgument
import cash.z.ecc.android.sdk.internal.NoBackupContextWrapper
import cash.z.ecc.android.sdk.internal.ext.deleteSuspend
import cash.z.ecc.android.sdk.internal.ext.existsSuspend
import cash.z.ecc.android.sdk.internal.ext.getDatabasePathSuspend
import cash.z.ecc.android.sdk.internal.ext.getFilesDirSuspend
import cash.z.ecc.android.sdk.internal.ext.getNoBackupFilesDirSuspend
import cash.z.ecc.android.sdk.internal.ext.mkdirsSuspend
import cash.z.ecc.android.sdk.internal.ext.renameToSuspend
import cash.z.ecc.android.sdk.internal.twig
import cash.z.ecc.android.sdk.type.ZcashNetwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Wrapper class for various SDK databases operations. It always guaranties exclusive access to
 * provided operations.
 *
 * @param context the application context
 */
@SuppressWarnings("TooManyFunctions")
class DatabaseCoordinator(context: Context) {

    private val applicationContext = context.applicationContext
    private val accessMutex = Mutex()

    companion object {
        private const val DB_DATA_NAME = "Data.db" // $NON-NLS
        private const val DB_CACHE_NAME = "Cache.db" // $NON-NLS
        private const val DB_PENDING_TRANSACTIONS_NAME = "PendingTransactions.db" // $NON-NLS

        private const val DATABASE_FILE_JOURNAL_SUFFIX = "journal" // $NON-NLS
        private const val DATABASE_FILE_WAL_SUFFIX = "wal" // $NON-NLS

        const val FAKE_NO_BACKUP_FOLDER = "no_backup" // $NON-NLS

        private val lazy =
            LazyWithArgument<Context, DatabaseCoordinator> { DatabaseCoordinator(it) }

        fun getInstance(context: Context) = lazy.getInstance(context)
    }

    /**
     * Returns the path to the Cache database that would correspond to the given alias
     * and network attributes.
     *
     * @param network the network associated with the data in the cache database.
     * @param alias the alias to convert into a database path
     */
    internal suspend fun cacheDbPath(
        network: ZcashNetwork,
        alias: String
    ): String {
        val dbLocationsPair = prepareDbFiles(
            applicationContext,
            network,
            alias,
            DB_CACHE_NAME
        )

        accessMutex.withLock {
            return checkAndMoveDatabaseFiles(
                dbLocationsPair.first,
                dbLocationsPair.second
            )
        }
    }

    /**
     * Returns the path to the Data database that would correspond to the given alias
     * and network attributes.
     *
     * @param network the network associated with the data in the database.
     * @param alias the alias to convert into a database path
     */
    internal suspend fun dataDbPath(
        network: ZcashNetwork,
        alias: String
    ): String {
        val dbLocationsPair = prepareDbFiles(
            applicationContext,
            network,
            alias,
            DB_DATA_NAME
        )

        accessMutex.withLock {
            return checkAndMoveDatabaseFiles(
                dbLocationsPair.first,
                dbLocationsPair.second
            )
        }
    }

    /**
     * Returns the path to the PendingTransaction database that would correspond to the given
     * alias and network attributes. As the originally created file was called just
     * PendingTransactions.db, we choose slightly different approach, but it also leads to
     * original database files migration with additional renaming too.
     *
     * @param network the network associated with the data in the database.
     * @param alias the alias to convert into a database path
     */
    internal suspend fun pendingTransactionsDbPath(
        network: ZcashNetwork,
        alias: String
    ): String {
        val legacyLocationDbFile = getDbFile(
            null,
            null,
            DB_PENDING_TRANSACTIONS_NAME,
            getDatabaseParentDirPath(applicationContext)
        )
        val preferredLocationDbFile = if (AndroidApiVersion.isAtLeastL) {
            getDbFile(
                network,
                alias,
                DB_PENDING_TRANSACTIONS_NAME,
                getNoBackupDirPath(applicationContext)
            )
        } else {
            getDbFile(
                network,
                alias,
                DB_PENDING_TRANSACTIONS_NAME,
                getFakeNoBackupDirPath(applicationContext)
            )
        }

        accessMutex.withLock {
            return checkAndMoveDatabaseFiles(
                legacyLocationDbFile,
                preferredLocationDbFile
            )
        }
    }

    /**
     * Function for common deletion of Data and Cache database files. It also checks and deletes
     * additional journal and wal files, if they exist.
     *
     * @param network the network associated with the data in the database.
     * @param alias the alias to convert into a database path
     */
    internal suspend fun deleteDatabases(
        network: ZcashNetwork,
        alias: String
    ): Boolean {
        accessMutex.withLock {
            val dataDeleted = deleteDatabase(
                dataDbPath(network, alias)
            )
            val cacheDeleted = deleteDatabase(
                dataDbPath(network, alias)
            )

            return dataDeleted || cacheDeleted
        }
    }

    /**
     * This helper function prepares a legacy (i.e. previously created) database file, as well
     * as the preferred (i.e. newly created) file for subsequent use (and eventually move).
     *
     * Note: the database file placed under the fake no_backup folder for devices with Android SDK
     * level lower than 21.
     *
     * @param appContext the application context
     * @param network the network associated with the data in the database.
     * @param alias the alias to convert into a database path
     * @param databaseName the name of the new database file
     */
    private suspend fun prepareDbFiles(
        appContext: Context,
        network: ZcashNetwork,
        alias: String,
        databaseName: String
    ): Pair<File, File> {
        val legacyLocationDbFile = getDbFile(
            network,
            alias,
            databaseName,
            getDatabaseParentDirPath(appContext)
        )
        val preferredLocationDbFile = if (AndroidApiVersion.isAtLeastL) {
            getDbFile(
                network,
                alias,
                databaseName,
                getNoBackupDirPath(appContext)
            )
        } else {
            getDbFile(
                network,
                alias,
                databaseName,
                getFakeNoBackupDirPath(appContext)
            )
        }

        return Pair(
            legacyLocationDbFile,
            preferredLocationDbFile
        )
    }

    /**
     * This function do actual database file copy or simply validate the file and return it.
     * From the Android SDK level 21 it places database files into no_backup folder, as it does
     * not allow automatic backup. On older APIs it places database files into databases folder,
     * which allows automatic backup. It also copies database files between these two folders,
     * if older folder usage is detected.
     *
     * @param legacyLocationDbFile the previously used file location
     * @param preferredLocationDbFile the newly used file location
     */
    private suspend fun checkAndMoveDatabaseFiles(
        legacyLocationDbFile: File,
        preferredLocationDbFile: File
    ): String {
        var resultDbFile = preferredLocationDbFile

        // check if the copy wasn't already performed and if it's needed
        if (!preferredLocationDbFile.existsSuspend() && legacyLocationDbFile.existsSuspend()) {
            // We check the move operation result and fallback to the legacy file, if
            // anything went wrong.
            if (!moveDatabaseFile(legacyLocationDbFile, preferredLocationDbFile)) {
                resultDbFile = legacyLocationDbFile
            }
        }

        return resultDbFile.absolutePath
    }

    /**
     * The purpose of this function is to copy database files between the old location (given by
     * the legacyLocationDbFile parameter) and the new location (given by preferredLocationDbFile).
     * The actual copy operation is performed with the renameTo function, which simply renames
     * a file path and persists the metadata information. The mechanism deals with the additional
     * database files -journal and -wal too, if they exist.
     *
     * @param legacyLocationDbFile the previously used file location (rename from)
     * @param preferredLocationDbFile the newly used file location (rename to)
     */
    private suspend fun moveDatabaseFile(
        legacyLocationDbFile: File,
        preferredLocationDbFile: File
    ): Boolean {
        val filesToBeRenamed = mutableListOf<Pair<File, File>>().apply {
            add(Pair(legacyLocationDbFile, preferredLocationDbFile))
        }

        // add journal database file, if exists
        val journalSuffixedDbFile = File(
            "${legacyLocationDbFile.absolutePath}-$DATABASE_FILE_JOURNAL_SUFFIX"
        )
        if (journalSuffixedDbFile.existsSuspend()) {
            filesToBeRenamed.add(
                Pair(
                    journalSuffixedDbFile,
                    File(
                        "${preferredLocationDbFile.absolutePath}-$DATABASE_FILE_JOURNAL_SUFFIX"
                    )
                )
            )
        }

        // add wal database file, if exists
        val walSuffixedDbFile = File(
            "${legacyLocationDbFile.absolutePath}-$DATABASE_FILE_WAL_SUFFIX"
        )
        if (walSuffixedDbFile.exists()) {
            filesToBeRenamed.add(
                Pair(
                    walSuffixedDbFile,
                    File(
                        "${preferredLocationDbFile.absolutePath}-$DATABASE_FILE_WAL_SUFFIX"
                    )
                )
            )
        }

        return runCatching {
            return@runCatching filesToBeRenamed.all {
                it.first.renameToSuspend(it.second)
            }
        }.onFailure {
            twig("Failed while renaming database files with: $it")
        }.getOrDefault(false)
    }

    /**
     * This function returns previously used database folder path (i.e. databases). The databases
     * folder is deprecated now, as it allows automatic data backup, which is not permitted for
     * our database files.
     *
     * @param appContext the application context
     */
    private suspend fun getDatabaseParentDirPath(appContext: Context): String {
        return appContext.getDatabasePathSuspend("unused.db").parentFile?.absolutePath
            ?: throw InitializerException.DatabasePathException
    }

    /**
     * This function returns a newly used database folder path (i.e. no_backup). The databases
     * folder is deprecated now and we use no_backup to avoid automatic backup of large possibly
     * sensitive data. It creates the needed folders on the path and then returns the absolute path
     * to the directory.
     *
     * @param appContext the application context
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private suspend fun getNoBackupDirPath(appContext: Context): String {
        return withContext(Dispatchers.IO) {
            val noBackupDir = appContext.getNoBackupFilesDirSuspend().absolutePath
                ?: throw InitializerException.DatabasePathException
            val noBackupFile = File(
                "$noBackupDir/${Files.NO_BACKUP_SUBDIRECTORY}"
            )

            if (noBackupFile.exists()) {
                return@withContext noBackupFile.absolutePath
            }

            return@withContext noBackupFile.mkdirsSuspend().let {
                if (!it) {
                    throw InitializerException.DatabasePathException
                }
                noBackupFile.absolutePath
            }
        }
    }

    /**
     * This function returns a newly used database folder path (i.e. no_backup). The databases
     * folder is deprecated now and we use no_backup to avoid automatic backup of large possibly
     * sensitive data. It creates the needed folders on the path and then returns the absolute path
     * to the directory.
     *
     * Note: this returns a new fake no_backup folder, as we are on older Android SDK level than 21,
     * which does not provide it by default.
     *
     * @param appContext the application context
     */
    private suspend fun getFakeNoBackupDirPath(appContext: Context): String {
        val filesDir = appContext.getFilesDirSuspend().absolutePath
            ?: throw InitializerException.DatabasePathException
        val fakeNoBackupFile = File(
            "$filesDir/$FAKE_NO_BACKUP_FOLDER/${Files.NO_BACKUP_SUBDIRECTORY}"
        )

        if (fakeNoBackupFile.existsSuspend()) {
            return fakeNoBackupFile.absolutePath
        }

        return fakeNoBackupFile.mkdirsSuspend().let {
            if (!it) {
                throw InitializerException.DatabasePathException
            }
            fakeNoBackupFile.absolutePath
        }
    }

    /**
     * Simple helper function, which prepares a database file object by input parameters.
     *
     * @param network the network associated with the data in the database.
     * @param alias the alias to convert into a database path
     * @param dbFileName the name of the new database file
     * @param parentDir the name of the parent directory, in which the file should be placed
     */
    private fun getDbFile(
        network: ZcashNetwork?,
        alias: String?,
        dbFileName: String,
        parentDir: String
    ): File {
        val aliasPrefix = if (alias == null) {
            ""
        } else if (alias.endsWith('_')) {
            alias
        } else {
            "${alias}_"
        }

        val networkPrefix = network?.networkName ?: ""

        return if (aliasPrefix.isNotEmpty()) {
            File(parentDir, "$aliasPrefix${networkPrefix}_$dbFileName")
        } else {
            File(parentDir, dbFileName)
        }
    }

    /**
     * Delete a database and its potential journal and wal file at the given path.
     *
     * The rollback journal (or newer wal) file is a temporary file used to implement atomic commit
     * and rollback capabilities in SQLite.
     *
     * @param filePath the path of the db to erase.
     * @return true when a file exists at the given path and was deleted.
     */
    private suspend fun deleteDatabase(filePath: String): Boolean {
        // Just try the journal and wal files too. Doesn't matter if they're not there.
        delete("$filePath-$DATABASE_FILE_JOURNAL_SUFFIX")
        delete("$filePath-$DATABASE_FILE_WAL_SUFFIX")

        return delete(filePath)
    }

    /**
     * Delete the file at the given path.
     *
     * @param filePath the path of the file to erase.
     * @return true when a file exists at the given path and was deleted.
     */
    private suspend fun delete(filePath: String): Boolean {
        return File(filePath).let {
            if (it.existsSuspend()) {
                twig("Deleting ${it.name}!")
                it.deleteSuspend()
                true
            } else {
                false
            }
        }
    }
}

/**
 * The purpose of this function is to provide Room.Builder via a static Room.databaseBuilder with
 * an injection of our NoBackupContextWrapper to override the behavior of getDatabasePath().
 *
 * Note: ideally we'd make this extension function or override the Room.databaseBuilder function,
 * but it's not possible, as it's a static function on Room class, which does not allow its
 * instantiation.
 *
 * @param name    The name of the database file.
 * @param <T>     The type of the database class.
 * @return A {@code RoomDatabaseBuilder<T>} which you can use to create the database.
 */
internal fun <T : RoomDatabase?> databaseBuilderNoBackupContext(
    context: Context,
    klass: Class<T>,
    name: String
): RoomDatabase.Builder<T> {
    return Room.databaseBuilder(
        NoBackupContextWrapper(context),
        klass,
        name
    )
}
