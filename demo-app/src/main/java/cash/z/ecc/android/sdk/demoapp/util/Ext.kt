package cash.z.ecc.android.sdk.demoapp.util

import android.content.Context
import android.text.format.DateUtils
import androidx.fragment.app.Fragment
import cash.z.ecc.android.sdk.demoapp.MainActivity

/**
 * Lazy extensions to make demo life easier.
 */

fun Fragment.mainActivity() = context as? MainActivity

/**
 * Add locale-specific commas to a number, if it exists.
 */
fun Number?.withCommas() = this?.let { "%,d".format(it) } ?: "Unknown"

/**
 * Convert date time in seconds to relative time like (4 days ago).
 */
@Suppress("MagicNumber")
fun Int?.toRelativeTime(context: Context) =
    this?.let { timeInSeconds ->
        DateUtils
            .getRelativeDateTimeString(
                context,
                timeInSeconds * 1000L,
                DateUtils.SECOND_IN_MILLIS,
                DateUtils.WEEK_IN_MILLIS,
                DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_ABBREV_MONTH
            ).toString()
    } ?: "Unknown"
