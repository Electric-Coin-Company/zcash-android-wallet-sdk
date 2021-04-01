package cash.z.ecc.android.sdk.tool

import android.content.Context
import cash.z.ecc.android.sdk.exception.BirthdayException
import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.ext.twig
import cash.z.ecc.android.sdk.type.WalletBirthday
import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import java.io.InputStreamReader
import java.util.Arrays

/**
 * Tool for loading checkpoints for the wallet, based on the height at which the wallet was born.
 *
 * @param appContext needed for loading checkpoints from the app's assets directory.
 */
class WalletBirthdayTool(appContext: Context) {
    val context = appContext.applicationContext

    /**
     * Load the nearest checkpoint to the given birthday height. If null is given, then this
     * will load the most recent checkpoint available.
     */
    fun loadNearest(birthdayHeight: Int? = null): WalletBirthday {
        return loadBirthdayFromAssets(context, birthdayHeight)
    }

    companion object {

        /**
         * Directory within the assets folder where birthday data
         * (i.e. sapling trees for a given height) can be found.
         */
        private const val BIRTHDAY_DIRECTORY = "zcash/saplingtree"

        /**
         * Load the nearest checkpoint to the given birthday height. If null is given, then this
         * will load the most recent checkpoint available.
         */
        fun loadNearest(context: Context, birthdayHeight: Int? = null): WalletBirthday {
            // TODO: potentially pull from shared preferences first
            return loadBirthdayFromAssets(context, birthdayHeight)
        }

        /**
         * Useful for when an exact checkpoint is needed, like for SAPLING_ACTIVATION_HEIGHT. In
         * most cases, loading the nearest checkpoint is preferred for privacy reasons.
         */
        fun loadExact(context: Context, birthdayHeight: Int) =
            loadNearest(context, birthdayHeight).also {
                if (it.height != birthdayHeight)
                    throw BirthdayException.ExactBirthdayNotFoundException(
                        birthdayHeight,
                        it.height
                    )
            }

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
            birthdayHeight: Int? = null
        ): WalletBirthday {
            twig("loading birthday from assets: $birthdayHeight")
            val treeFiles =
                context.assets.list(BIRTHDAY_DIRECTORY)?.apply {
                    sortByDescending { fileName ->
                        try {
                            fileName.split('.').first().toInt()
                        } catch (t: Throwable) {
                            ZcashSdk.SAPLING_ACTIVATION_HEIGHT
                        }
                    }
                }
            if (treeFiles.isNullOrEmpty()) throw BirthdayException.MissingBirthdayFilesException(
                BIRTHDAY_DIRECTORY
            )
            twig("found ${treeFiles.size} sapling tree checkpoints: ${Arrays.toString(treeFiles)}")
            val file: String
            try {
                file = if (birthdayHeight == null) treeFiles.first() else {
                    treeFiles.first {
                        it.split(".").first().toInt() <= birthdayHeight
                    }
                }
            } catch (t: Throwable) {
                throw BirthdayException.BirthdayFileNotFoundException(
                    BIRTHDAY_DIRECTORY,
                    birthdayHeight
                )
            }
            try {
                val reader = JsonReader(
                    InputStreamReader(context.assets.open("$BIRTHDAY_DIRECTORY/$file"))
                )
                return Gson().fromJson(reader, WalletBirthday::class.java)
            } catch (t: Throwable) {
                throw BirthdayException.MalformattedBirthdayFilesException(
                    BIRTHDAY_DIRECTORY,
                    treeFiles[0]
                )
            }
        }
    }
}
