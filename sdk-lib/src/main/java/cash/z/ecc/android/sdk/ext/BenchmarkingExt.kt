package cash.z.ecc.android.sdk.ext

import cash.z.ecc.android.sdk.BuildConfig

object BenchmarkingExt {
    private const val TARGET_BUILD_TYPE = "benchmark"

    fun isBenchmarking(): Boolean = TARGET_BUILD_TYPE == BuildConfig.BUILD_TYPE
}
