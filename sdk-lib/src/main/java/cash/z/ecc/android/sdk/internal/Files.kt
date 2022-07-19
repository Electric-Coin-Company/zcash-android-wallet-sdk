package cash.z.ecc.android.sdk.internal

/**
 * Because the filesystem is a shared resource, this declares the filenames that the SDK is using
 * in one centralized place.
 */
internal object Files {
    /**
     * Subdirectory under the Android "no backup" directory which is owned by the SDK.
     */
    const val NO_BACKUP_SUBDIRECTORY = "co.electricoin.zcash" // $NON-NLS
}