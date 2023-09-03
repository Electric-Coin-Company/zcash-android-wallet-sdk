@file:Suppress("ktlint:standard:filename")

package cash.z.ecc.android.sdk.demoapp.ext

import android.content.Context
import androidx.fragment.app.Fragment

/**
 * A safer alternative to [Fragment.requireContext], as it avoids leaking Fragment or Activity context
 * when Application context is often sufficient.
 */
fun Fragment.requireApplicationContext(): Context = requireContext().applicationContext
