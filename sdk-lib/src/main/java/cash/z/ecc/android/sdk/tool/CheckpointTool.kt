package cash.z.ecc.android.sdk.tool

import android.content.Context
import androidx.annotation.VisibleForTesting
import cash.z.ecc.android.sdk.exception.BirthdayException
import cash.z.ecc.android.sdk.internal.Twig
import cash.z.ecc.android.sdk.internal.model.Checkpoint
import cash.z.ecc.android.sdk.internal.model.ext.from
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.ZcashNetwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.IOException
import java.util.Locale

/**
 * Tool for loading checkpoints for the wallet, based on the height at which the wallet was born.
 */
internal object CheckpointTool {
    // Behavior change implemented as a fix for issue #270.  Temporarily adding a boolean
    // that allows the change to be rolled back quickly if needed, although long-term
    // this flag should be removed.
    @VisibleForTesting
    internal val IS_FALLBACK_ON_FAILURE = true

    /**
     * Load the nearest checkpoint to the given birthday height. If null is given, then this
     * will load the most recent checkpoint available.
     */
    suspend fun loadNearest(
        context: Context,
        network: ZcashNetwork,
        birthdayHeight: BlockHeight?
    ): Checkpoint {
        // TODO [#684]: potentially pull from shared preferences first
        // TODO [#684]: https://github.com/zcash/zcash-android-wallet-sdk/issues/684
        return loadCheckpointFromAssets(context, network, birthdayHeight)
    }

    /**
     * Useful for when an exact checkpoint is needed, like for SAPLING_ACTIVATION_HEIGHT. In
     * most cases, loading the nearest checkpoint is preferred for privacy reasons.
     */
    suspend fun loadExact(
        context: Context,
        network: ZcashNetwork,
        birthday: BlockHeight
    ) = loadNearest(context, network, birthday).also {
        if (it.height != birthday) {
            throw BirthdayException.ExactBirthdayNotFoundException(
                birthday,
                it
            )
        }
    }

    // Converting this to suspending will then propagate
    @Throws(IOException::class)
    internal suspend fun listCheckpointDirectoryContents(
        context: Context,
        directory: String
    ) = withContext(Dispatchers.IO) {
        context.assets.list(directory)
    }

    /**
     * Returns the directory within the assets folder where birthday data
     * (i.e. sapling trees for a given height) can be found.
     */
    @VisibleForTesting
    internal fun checkpointDirectory(network: ZcashNetwork) =
        "co.electriccoin.zcash/checkpoint/${network.networkName.lowercase(Locale.ROOT)}"

    internal fun checkpointHeightFromFilename(
        zcashNetwork: ZcashNetwork,
        fileName: String
    ) = BlockHeight.new(zcashNetwork, fileName.split('.').first().toLong())

    private fun Array<String>.sortDescending(zcashNetwork: ZcashNetwork) =
        apply { sortByDescending { checkpointHeightFromFilename(zcashNetwork, it).value } }

    /**
     * Load the given birthday file from the assets of the given context. When no height is
     * specified, we default to the file with the greatest name.
     *
     * @param context the context from which to load assets.
     * @param birthday the height file to look for among the file names.
     *
     * @return a WalletBirthday that reflects the contents of the file or an exception when
     * parsing fails.
     */
    private suspend fun loadCheckpointFromAssets(
        context: Context,
        network: ZcashNetwork,
        birthday: BlockHeight?
    ): Checkpoint {
        Twig.debug { "loading checkpoint from assets: $birthday" }
        val directory = checkpointDirectory(network)
        val treeFiles = getFilteredFileNames(context, network, directory, birthday)

        Twig.debug { "found ${treeFiles.size} sapling tree checkpoints: $treeFiles" }

        return getFirstValidWalletBirthday(context, network, directory, treeFiles)
    }

    private suspend fun getFilteredFileNames(
        context: Context,
        network: ZcashNetwork,
        directory: String,
        birthday: BlockHeight?
    ): List<String> {
        val unfilteredTreeFiles = listCheckpointDirectoryContents(context, directory)
        if (unfilteredTreeFiles.isNullOrEmpty()) {
            throw BirthdayException.MissingBirthdayFilesException(directory)
        }

        val filteredTreeFiles =
            unfilteredTreeFiles
                .sortDescending(network)
                .filter { filename ->
                    birthday?.let { checkpointHeightFromFilename(network, filename) <= it } ?: true
                }

        if (filteredTreeFiles.isEmpty()) {
            throw BirthdayException.BirthdayFileNotFoundException(
                directory,
                birthday
            )
        }

        return filteredTreeFiles
    }

    /**
     * @param treeFiles A list of files, sorted in descending order based on `int` value of the first part of
     * the filename.
     */
    @VisibleForTesting
    internal suspend fun getFirstValidWalletBirthday(
        context: Context,
        network: ZcashNetwork,
        directory: String,
        treeFiles: List<String>
    ): Checkpoint {
        var lastException: Exception? = null
        treeFiles.forEach { treefile ->
            @Suppress("TooGenericExceptionCaught")
            try {
                val jsonString =
                    withContext(Dispatchers.IO) {
                        context.assets.open("$directory/$treefile").use { inputStream ->
                            inputStream.reader().use { inputStreamReader ->
                                BufferedReader(inputStreamReader).use { bufferedReader ->
                                    bufferedReader.readText()
                                }
                            }
                        }
                    }

                return Checkpoint.from(network, jsonString)
            } catch (t: Throwable) {
                val exception =
                    BirthdayException.MalformattedBirthdayFilesException(
                        directory,
                        treefile,
                        t
                    )
                lastException = exception

                if (IS_FALLBACK_ON_FAILURE) {
                    // TODO [#684]: If we ever add crash analytics hooks, this would be something to report
                    // TODO [#684]: https://github.com/zcash/zcash-android-wallet-sdk/issues/684
                    Twig.debug(t) { "Malformed birthday file $t" }
                } else {
                    throw exception
                }
            }
        }

        throw lastException!!
    }
}
