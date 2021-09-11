package cash.z.ecc.android.sdk.tool

import android.content.Context
import androidx.annotation.VisibleForTesting
import cash.z.ecc.android.sdk.exception.BirthdayException
import cash.z.ecc.android.sdk.ext.twig
import cash.z.ecc.android.sdk.type.WalletBirthday
import cash.z.ecc.android.sdk.type.ZcashNetwork
import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import java.io.IOException
import java.io.InputStreamReader

/**
 * Tool for loading checkpoints for the wallet, based on the height at which the wallet was born.
 *
 * @param appContext needed for loading checkpoints from the app's assets directory.
 */
class WalletBirthdayTool(appContext: Context) {
    private val context = appContext.applicationContext

    /**
     * Load the nearest checkpoint to the given birthday height. If null is given, then this
     * will load the most recent checkpoint available.
     */
    fun loadNearest(network: ZcashNetwork, birthdayHeight: Int? = null): WalletBirthday {
        return loadBirthdayFromAssets(context, network, birthdayHeight)
    }

    companion object {

        // Behavior change implemented as a fix for issue #270.  Temporarily adding a boolean
        // that allows the change to be rolled back quickly if needed, although long-term
        // this flag should be removed.
        @VisibleForTesting
        internal val IS_FALLBACK_ON_FAILURE = true

        /**
         * Load the nearest checkpoint to the given birthday height. If null is given, then this
         * will load the most recent checkpoint available.
         */
        fun loadNearest(
            context: Context,
            network: ZcashNetwork,
            birthdayHeight: Int? = null
        ): WalletBirthday {
            // TODO: potentially pull from shared preferences first
            return loadBirthdayFromAssets(context, network, birthdayHeight)
        }

        /**
         * Useful for when an exact checkpoint is needed, like for SAPLING_ACTIVATION_HEIGHT. In
         * most cases, loading the nearest checkpoint is preferred for privacy reasons.
         */
        fun loadExact(context: Context, network: ZcashNetwork, birthdayHeight: Int) =
            loadNearest(context, network, birthdayHeight).also {
                if (it.height != birthdayHeight)
                    throw BirthdayException.ExactBirthdayNotFoundException(
                        birthdayHeight,
                        it.height
                    )
            }

        // TODO: This method performs disk IO; convert to suspending function
        // Converting this to suspending will then propagate
        @Throws(IOException::class)
        internal fun listBirthdayDirectoryContents(context: Context, directory: String) =
            context.assets.list(directory)

        /**
         * Returns the directory within the assets folder where birthday data
         * (i.e. sapling trees for a given height) can be found.
         */
        @VisibleForTesting
        internal fun birthdayDirectory(network: ZcashNetwork) =
            "saplingtree/${network.networkName.lowercase()}"

        internal fun birthdayHeight(fileName: String) = fileName.split('.').first().toInt()

        private fun Array<String>.sortDescending() =
            apply { sortByDescending { birthdayHeight(it) } }

        /**
         * Load the given birthday file from the assets of the given context. When no height is
         * specified, we default to the file with the greatest name.
         *
         * @param context the context from which to load assets.
         * @param birthdayHeight the height file to look for among the file names.
         *
         * @return a WalletBirthday that reflects the contents of the file or an exception when
         * parsing fails.
         */
        private fun loadBirthdayFromAssets(
            context: Context,
            network: ZcashNetwork,
            birthdayHeight: Int? = null
        ): WalletBirthday {
            twig("loading birthday from assets: $birthdayHeight")
            val directory = birthdayDirectory(network)
            val treeFiles = getFilteredFileNames(context, directory, birthdayHeight)

            twig("found ${treeFiles.size} sapling tree checkpoints: $treeFiles")

            return getFirstValidWalletBirthday(context, directory, treeFiles)
        }

        private fun getFilteredFileNames(
            context: Context,
            directory: String,
            birthdayHeight: Int? = null,
        ): List<String> {
            val unfilteredTreeFiles = listBirthdayDirectoryContents(context, directory)
            if (unfilteredTreeFiles.isNullOrEmpty()) {
                throw BirthdayException.MissingBirthdayFilesException(directory)
            }

            val filteredTreeFiles = unfilteredTreeFiles
                .sortDescending()
                .filter { filename ->
                    birthdayHeight?.let { birthdayHeight(filename) <= it } ?: true
                }

            if (filteredTreeFiles.isEmpty()) {
                throw BirthdayException.BirthdayFileNotFoundException(
                    directory,
                    birthdayHeight
                )
            }

            return filteredTreeFiles
        }

        /**
         * @param treeFiles A list of files, sorted in descending order based on `int` value of the first part of the filename.
         */
        @VisibleForTesting
        internal fun getFirstValidWalletBirthday(
            context: Context,
            directory: String,
            treeFiles: List<String>
        ): WalletBirthday {
            var lastException: Exception? = null
            treeFiles.forEach { treefile ->
                try {
                    context.assets.open("$directory/$treefile").use { inputStream ->
                        InputStreamReader(inputStream).use { inputStreamReader ->
                            JsonReader(inputStreamReader).use { jsonReader ->
                                return Gson().fromJson(jsonReader, WalletBirthday::class.java)
                            }
                        }
                    }
                } catch (t: Throwable) {
                    val exception = BirthdayException.MalformattedBirthdayFilesException(
                        directory,
                        treefile
                    )
                    lastException = exception

                    if (IS_FALLBACK_ON_FAILURE) {
                        // TODO: If we ever add crash analytics hooks, this would be something to report
                        twig("Malformed birthday file $t")
                    } else {
                        throw exception
                    }
                }
            }

            throw lastException!!
        }
    }
}
