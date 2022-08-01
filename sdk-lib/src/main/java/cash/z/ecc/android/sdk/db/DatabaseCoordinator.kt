package cash.z.ecc.android.sdk.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import cash.z.ecc.android.sdk.exception.InitializerException
import cash.z.ecc.android.sdk.internal.Files
import cash.z.ecc.android.sdk.internal.LazyWithArgument
import cash.z.ecc.android.sdk.internal.NoBackupContextWrapper
import cash.z.ecc.android.sdk.internal.ext.deleteSuspend
import cash.z.ecc.android.sdk.internal.ext.existsSuspend
import cash.z.ecc.android.sdk.internal.ext.getDatabasePathSuspend
import cash.z.ecc.android.sdk.internal.ext.renameToSuspend
import cash.z.ecc.android.sdk.internal.twig
import cash.z.ecc.android.sdk.type.ZcashNetwork
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/**
 * Wrapper class for various SDK databases operations. It always guaranties exclusive access to
 * provided operations.
 *
 * @param context the application context
 */
@SuppressWarnings("TooManyFunctions")
internal class DatabaseCoordinator private constructor(context: Context) {

    /*
     * This implementation is thread-safe but is not multi-process safe.
     *
     * The mutex helps to ensure that two instances of the SDK being initialized in the same
     * process do not have conflicts with regard to moving the databases around.  However if an
     * application decides to use multiple processes this could cause a problem during the one-time
     * the database path migration.
     */

    private val applicationContext = context.applicationContext
    private val createFileMutex = Mutex()
    private val deleteFileMutex = Mutex()

    companion object {
        const val DB_DATA_NAME = "Data.db" // $NON-NLS
        const val DB_CACHE_NAME = "Cache.db" // $NON-NLS
        const val DB_PENDING_TRANSACTIONS_NAME = "PendingTransactions.db" // $NON-NLS

        const val DATABASE_FILE_JOURNAL_SUFFIX = "journal" // $NON-NLS
        const val DATABASE_FILE_WAL_SUFFIX = "wal" // $NON-NLS

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
    internal suspend fun cacheDbFile(
        network: ZcashNetwork,
        alias: String
    ): File {
        val dbLocationsPair = prepareDbFiles(
            applicationContext,
            network,
            alias,
            DB_CACHE_NAME
        )

        createFileMutex.withLock {
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
    internal suspend fun dataDbFile(
        network: ZcashNetwork,
        alias: String
    ): File {
        val dbLocationsPair = prepareDbFiles(
            applicationContext,
            network,
            alias,
            DB_DATA_NAME
        )

        createFileMutex.withLock {
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
    ): File {
        val legacyLocationDbFile = newDatabaseFilePointer(
            null,
            null,
            DB_PENDING_TRANSACTIONS_NAME,
            getDatabaseParentDir(applicationContext)
        )
        val preferredLocationDbFile = newDatabaseFilePointer(
            network,
            alias,
            DB_PENDING_TRANSACTIONS_NAME,
            Files.getZcashNoBackupSubdirectory(applicationContext)
        )

        createFileMutex.withLock {
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
        deleteFileMutex.withLock {
            val dataDeleted = deleteDatabase(
                dataDbFile(network, alias)
            )
            val cacheDeleted = deleteDatabase(
                cacheDbFile(network, alias)
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
        val legacyLocationDbFile = newDatabaseFilePointer(
            network,
            alias,
            databaseName,
            getDatabaseParentDir(appContext)
        )
        val preferredLocationDbFile = newDatabaseFilePointer(
            network,
            alias,
            databaseName,
            Files.getZcashNoBackupSubdirectory(appContext)
        )

        return Pair(
            legacyLocationDbFile,
            preferredLocationDbFile
        )
    }

    /**
     * This function do actual database file move or simply validate the file and return it.
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
    ): File {
        var resultDbFile = preferredLocationDbFile

        // check if the move wasn't already performed and if it's needed
        if (!preferredLocationDbFile.existsSuspend() && legacyLocationDbFile.existsSuspend()) {
            // We check the move operation result and fallback to the legacy file, if
            // anything went wrong.
            if (!moveDatabaseFile(legacyLocationDbFile, preferredLocationDbFile)) {
                resultDbFile = legacyLocationDbFile
            }
        }

        return resultDbFile
    }

    /**
     * The purpose of this function is to move database files between the old location (given by
     * the legacyLocationDbFile parameter) and the new location (given by preferredLocationDbFile).
     * The actual move operation is performed with the renameTo function, which simply renames
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
        if (walSuffixedDbFile.existsSuspend()) {
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
    private suspend fun getDatabaseParentDir(appContext: Context): File {
        return appContext.getDatabasePathSuspend("unused.db").parentFile
            ?: throw InitializerException.DatabasePathException
    }

    /**
     * Simple helper function, which prepares a database file object by input parameters. It does
     * not create the file, just determines the file path.
     *
     * @param network the network associated with the data in the database.
     * @param alias the alias to convert into a database path
     * @param dbFileName the name of the new database file
     * @param parentDir the name of the parent directory, in which the file should be placed
     */
    private fun newDatabaseFilePointer(
        network: ZcashNetwork?,
        alias: String?,
        dbFileName: String,
        parentDir: File
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
     * @param file the path of the db to erase.
     * @return true when a file exists at the given path and was deleted.
     */
    private suspend fun deleteDatabase(file: File): Boolean {
        // Just try the journal and wal files too. Doesn't matter if they're not there.
        File("${file.absolutePath}-$DATABASE_FILE_JOURNAL_SUFFIX").deleteSuspend()
        File("${file.absolutePath}-$DATABASE_FILE_WAL_SUFFIX").deleteSuspend()

        return file.deleteSuspend()
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
 * @param context
 * @param klass The database class.
 * @param databaseFile  The database file.
 * @return A {@code RoomDatabaseBuilder<T>} which you can use to create the database.
 */
internal fun <T : RoomDatabase?> databaseBuilderNoBackupContext(
    context: Context,
    klass: Class<T>,
    databaseFile: File
): RoomDatabase.Builder<T> {
    return Room.databaseBuilder(
        NoBackupContextWrapper(context, databaseFile.parentFile),
        klass,
        databaseFile.name
    )
}
