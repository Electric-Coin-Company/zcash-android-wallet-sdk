package cash.z.ecc.android.sdk.demoapp.util

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.IntRange

internal object AndroidApiVersion {
    /**
     * @param sdk SDK version number to test against the current environment.
     * @return `true` if [android.os.Build.VERSION.SDK_INT] is greater than or equal to
     * [sdk].
     */
    @ChecksSdkIntAtLeast(parameter = 0)
    fun isAtLeast(
        @IntRange(from = Build.VERSION_CODES.BASE.toLong()) sdk: Int
    ): Boolean = Build.VERSION.SDK_INT >= sdk

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.P)
    val isAtLeastP = isAtLeast(Build.VERSION_CODES.P)

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.Q)
    val isAtLeastQ = isAtLeast(Build.VERSION_CODES.Q)

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
    val isAtLeastR = isAtLeast(Build.VERSION_CODES.R)

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
    val isAtLeastS = isAtLeast(Build.VERSION_CODES.S)

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
    val isAtLeastTiramisu = isAtLeast(Build.VERSION_CODES.TIRAMISU)

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    val isAtLeastUpsideDownCake = isAtLeast(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    val isAtLeastVanillaIceCream = isAtLeast(Build.VERSION_CODES.VANILLA_ICE_CREAM)

    /**
     * This property indicates a preview version of the current device Android SDK.
     */
    val isPreview = 0 != Build.VERSION.PREVIEW_SDK_INT
}
