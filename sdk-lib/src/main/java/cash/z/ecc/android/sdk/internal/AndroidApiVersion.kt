package cash.z.ecc.android.sdk.internal

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.IntRange

object AndroidApiVersion {
    /**
     * @param sdk SDK version number to test against the current environment.
     * @return `true` if [android.os.Build.VERSION.SDK_INT] is greater than or equal to
     * [sdk].
     */
    @ChecksSdkIntAtLeast(parameter = 0)
    fun isAtLeast(@IntRange(from = Build.VERSION_CODES.BASE.toLong()) sdk: Int): Boolean {
        return Build.VERSION.SDK_INT >= sdk
    }

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.LOLLIPOP)
    val isAtLeastL = isAtLeast(Build.VERSION_CODES.LOLLIPOP)

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.M)
    val isAtLeastM = isAtLeast(Build.VERSION_CODES.M)

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.N)
    val isAtLeastN = isAtLeast(Build.VERSION_CODES.N)

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.O)
    val isAtLeastO = isAtLeast(Build.VERSION_CODES.O)

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.P)
    val isAtLeastP = isAtLeast(Build.VERSION_CODES.P)

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.Q)
    val isAtLeastQ = isAtLeast(Build.VERSION_CODES.Q)

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
    val isAtLeastR = isAtLeast(Build.VERSION_CODES.R)

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
    val isAtLeastS = isAtLeast(Build.VERSION_CODES.S)

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
    val isAtLeastT = isAtLeast(Build.VERSION_CODES.TIRAMISU)

    val isPreview = 0 != Build.VERSION.PREVIEW_SDK_INT
}
