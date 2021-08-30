package cash.z.ecc.android.sdk.demoapp.util

import android.text.format.DateUtils
import androidx.fragment.app.Fragment
import cash.z.ecc.android.sdk.demoapp.App
import cash.z.ecc.android.sdk.demoapp.MainActivity
import cash.z.wallet.sdk.rpc.CompactFormats

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
fun Int?.toRelativeTime() =
    this?.let { timeInSeconds ->
        DateUtils.getRelativeDateTimeString(
            App.instance,
            timeInSeconds * 1000L,
            DateUtils.SECOND_IN_MILLIS,
            DateUtils.WEEK_IN_MILLIS,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_ABBREV_MONTH
        ).toString()
    } ?: "Unknown"


fun List<CompactFormats.CompactTx>?.toHtml() =
    this.takeUnless { it.isNullOrEmpty() }?.let { txs ->
        buildString {
            append("<br/><b>transactions (shielded INs / OUTs):</b>")
            txs.forEach { append("<br/><b>&nbsp;&nbsp;tx${it.index}:</b> ${it.spendsCount} / ${it.outputsCount}") }
        }
    } ?: ""

