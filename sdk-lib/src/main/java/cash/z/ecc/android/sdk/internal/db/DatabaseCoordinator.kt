package cash.z.ecc.android.sdk.internal.db

import android.content.Context
import androidx.annotation.VisibleForTesting
import cash.z.ecc.android.sdk.exception.InitializeException
import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.internal.Files
import cash.z.ecc.android.sdk.internal.LazyWithArgument
import cash.z.ecc.android.sdk.internal.Twig
import cash.z.ecc.android.sdk.internal.ext.deleteRecursivelySuspend
import cash.z.ecc.android.sdk.internal.ext.deleteSuspend
import cash.z.ecc.android.sdk.internal.ext.existsSuspend
import cash.z.ecc.android.sdk.internal.ext.getDatabasePathSuspend
import cash.z.ecc.android.sdk.internal.ext.renameToSuspend
import cash.z.ecc.android.sdk.model.ZcashNetwork
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/**
 * Wrapper class for various SDK databases operations. It always guaranties exclusive access to
 * provided operations.
 *
 * @param context the application context
 */
@Suppress("TooManyFunctions")
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
        @VisibleForTesting
        internal const val DB_DATA_NAME_LEGACY = "Data.db" // $NON-NLS
        const val DB_DATA_NAME = "data.sqlite3" // $NON-NLS

        @VisibleForTesting
        internal const val DB_CACHE_OLDER_NAME_LEGACY = "Cache.db" // $NON-NLS
        internal const val DB_CACHE_NEWER_NAME_LEGACY = "cache.sqlite3" // $NON-NLS
        const val DB_FS_BLOCK_DB_ROOT_NAME = "fs_cache" // $NON-NLS

        @VisibleForTesting
        internal const val DB_PENDING_TRANSACTIONS_NAME_LEGACY = "PendingTransactions.db" // $NON-NLS
        const val DB_PENDING_TRANSACTIONS_NAME = "pending_transactions.sqlite3" // $NON-NLS

        const val DATABASE_FILE_JOURNAL_SUFFIX = "journal" // $NON-NLS
        const val DATABASE_FILE_WAL_SUFFIX = "wal" // $NON-NLS

        @VisibleForTesting
        internal const val ALIAS_LEGACY = "ZcashSdk" // $NON-NLS

        private val lazy =
            LazyWithArgument<Context, DatabaseCoordinator> { DatabaseCoordinator(it) }

        fun getInstance(context: Context) = lazy.getInstance(context)
    }

    /**
     * Returns the root folder of the cache database files that would correspond to the given
     * alias and network attributes.
     *
     * @param network the network associated with the data in the cache
     * @param alias the alias to convert into a cache path
     *
     * @return the cache database folder
     */
    internal suspend fun fsBlockDbRoot(
        network: ZcashNetwork,
        alias: String
    ): File {
        // First we deal with the legacy Cache database files (rollback included) on both older and newer path. In
        // case of deletion failure caused by any reason, we try it on the next time again.
        val legacyDbFilesDeleted = deleteLegacyCacheDbFiles(network, alias)
        val result =
            if (legacyDbFilesDeleted) {
                "are successfully deleted"
            } else {
                "failed to be deleted. Will be retried it on the next time"
            }
        Twig.debug { "Legacy Cache database files $result." }

        return newDatabaseFilePointer(
            network,
            alias,
            DB_FS_BLOCK_DB_ROOT_NAME,
            Files.getZcashNoBackupSubdirectory(applicationContext)
        )
    }

    /**
     * Returns the file of the Data database that would correspond to the given alias
     * and network attributes.
     *
     * @param network the network associated with the data in the database
     * @param alias the alias to convert into a database path
     *
     * @return the Data database file
     */
    internal suspend fun dataDbFile(
        network: ZcashNetwork,
        alias: String
    ): File {
        val dbLocationsPair =
            prepareDbFiles(
                applicationContext,
                network,
                alias,
                DB_DATA_NAME_LEGACY,
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
     * Returns the file of the PendingTransaction database that would correspond to the given
     * alias and network attributes. As the originally created file was called just
     * PendingTransactions.db, we choose slightly different approach, but it also leads to
     * original database files migration with additional renaming too.
     *
     * @param network the network associated with the data in the database
     * @param alias the alias to convert into a database path
     *
     * @return the PendingTransaction database file
     */
    internal suspend fun pendingTransactionsDbFile(
        network: ZcashNetwork,
        alias: String
    ): File {
        val legacyLocationDbFile =
            newDatabaseFilePointer(
                null,
                null,
                DB_PENDING_TRANSACTIONS_NAME_LEGACY,
                getDatabaseParentDir(applicationContext)
            )
        val preferredLocationDbFile =
            newDatabaseFilePointer(
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
     * @param network the network associated with the data in the database
     * @param alias the alias to convert into a database path
     *
     * @return true only if any database deleted, false otherwise
     */
    internal suspend fun deleteDatabases(
        network: ZcashNetwork,
        alias: String
    ): Boolean {
        deleteFileMutex.withLock {
            val dataDeleted = deleteDatabase(dataDbFile(network, alias))

            val cacheDeleted = fsBlockDbRoot(network, alias).deleteRecursivelySuspend()

            Twig.info { "Delete databases result: ${dataDeleted || cacheDeleted}" }

            return dataDeleted || cacheDeleted
        }
    }

    /**
     * Function for common deletion of pending transaction database files. It also checks and deletes
     * additional journal and wal files, if they exist.
     *
     * @param network the network associated with the data in the database
     * @param alias the alias to convert into a database path
     *
     * @return true only if any database deleted, false otherwise
     */
    internal suspend fun deletePendingTransactionDatabase(
        network: ZcashNetwork,
        alias: String
    ): Boolean {
        deleteFileMutex.withLock {
            return deleteDatabase(pendingTransactionsDbFile(network, alias))
        }
    }

    /**
     * This checks and potentially deletes all the legacy Cache database files, which correspond to the given alias and
     * network attributes, as we recently switched to the store blocks on disk mechanism instead of putting them into
     * the Cache database.
     *
     * This function deals with database rollback files too.
     *
     * @param network the network associated with the data in the Cache database
     * @param alias the alias to convert into a database path
     *
     * @return true in case of successful deletion of all the files, false otherwise
     */
    private suspend fun deleteLegacyCacheDbFiles(
        network: ZcashNetwork,
        alias: String
    ): Boolean {
        val legacyDatabaseLocationPair =
            prepareDbFiles(
                applicationContext,
                network,
                alias,
                DB_CACHE_OLDER_NAME_LEGACY,
                DB_CACHE_NEWER_NAME_LEGACY
            )

        var olderLegacyCacheDbDeleted = true
        var newerLegacyCacheDbDeleted = true

        if (legacyDatabaseLocationPair.first.existsSuspend()) {
            olderLegacyCacheDbDeleted = deleteDatabase(legacyDatabaseLocationPair.first)
        }
        if (legacyDatabaseLocationPair.second.existsSuspend()) {
            newerLegacyCacheDbDeleted = deleteDatabase(legacyDatabaseLocationPair.second)
        }

        return olderLegacyCacheDbDeleted && newerLegacyCacheDbDeleted
    }

    /**
     * This helper function prepares a legacy (i.e. previously created) database file, as well
     * as the preferred (i.e. newly created) file for subsequent use (and eventually move).
     *
     * @param appContext the application context
     * @param network the network associated with the data in the database
     * @param alias the alias to convert into a database path
     * @param databaseName the name of the new database file
     */
    private suspend fun prepareDbFiles(
        appContext: Context,
        network: ZcashNetwork,
        alias: String,
        databaseNameLegacy: String,
        databaseName: String
    ): Pair<File, File> {
        // Here we change the alias to be lowercase and underscored only if we work with the default
        // Zcash alias, otherwise we need to keep an SDK caller alias the same to avoid the database
        // files move breakage.
        val aliasLegacy =
            if (ZcashSdk.DEFAULT_ALIAS == alias) {
                ALIAS_LEGACY
            } else {
                alias
            }

        val legacyLocationDbFile =
            newDatabaseFilePointer(
                network,
                aliasLegacy,
                databaseNameLegacy,
                getDatabaseParentDir(appContext)
            )
        val preferredLocationDbFile =
            newDatabaseFilePointer(
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
     * The purpose of this function is to move database files between the old location (given by the {@code
     * legacyLocationDbFile} parameter) and the new location (given by {@code preferredLocationDbFile}). The actual
     * move operation is performed with the renameTo function, which simply renames a file path and persists the
     * metadata information. The mechanism deals with the additional database files -journal and -wal too, if they
     * exist.
     *
     * @param legacyLocationDbFile the previously used file location (rename from)
     * @param preferredLocationDbFile the newly used file location (rename to)
     */
    private suspend fun moveDatabaseFile(
        legacyLocationDbFile: File,
        preferredLocationDbFile: File
    ): Boolean {
        val filesToBeRenamed =
            mutableListOf<Pair<File, File>>().apply {
                add(Pair(legacyLocationDbFile, preferredLocationDbFile))
            }

        // add journal database file, if exists
        val journalSuffixedDbFile =
            File(
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
        val walSuffixedDbFile =
            File(
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
            Twig.warn(it) { "Failed while renaming database files" }
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
            ?: throw InitializeException.DatabasePathException
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
        val aliasPrefix =
            if (alias == null) {
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
