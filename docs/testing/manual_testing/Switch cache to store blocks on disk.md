# About
This manual test case provides information on how to manually test the implemented action of switching our 
mechanism of persisting CompactBlocks cache from in database storage to on disk storage. 

The benefit of this approach is that the storing blocks on-disk does not require storing of blobs into  
the sqlite database, which can cause poor performance as the database grows large. Instead, 
we store them on disk and insert only a limited portion of a block information (metadata) into the database.

Observed result of this manual test should be:
- there should be no Cache database in the older (`/databases/`) or the newer (`/no_backup/co.electricoin.zcash/`) legacy locations
- files containing protobuf-encoded `CompactBlock`s stored in `/no_backup/co.electricoin.zcash/blocks/` location

# Prerequisites
- Install [Android Studio](https://developer.android.com/studio)
- Install an [emulator](https://developer.android.com/studio/run/emulator) to Android Studio
- Install a `git` client
- Clone the [Zcash Android SDK repository](https://github.com/zcash/zcash-android-wallet-sdk)

# Prepare steps
1. Create some existing persistent wallet state using a version prior to the introduction of the disk-based cache. To do this, check out commit [zcash/zcash-android-wallet-sdk#910](https://github.com/zcash/zcash-android-wallet-sdk/pull/910) (currently commit `a67d287e5cc90fe3a774b02174dca1b32331058c`). 
1. Update dependencies lock (if needed) and sync Gradle files
1. Select one of the **Mainnet** build variants from **Build Variant** window
1. Build and run the demo-app on selected emulator
1. Once the app is open, select e.g. _Alyssa P. Hacker_ secret phrase and then let the SDK create the Cache database 
   files with auto-start syncing on the home screen
1. Wait a moment to be sure that the sync mechanism has already been initialized and started to fill in the Cache 
   database with CompactBlocks entries.
1. Open Device File Explorer in Android Studio, select the same emulator device from the top drop-down menu
1. Go to `/data/data/cash.z.ecc.android.sdk.demoapp.mainnet/no_backup/co.electricoin.zcash/`
1. Verify there are `cache.sqlite3` and possibly some rollback files too (suffixed with `journal` or `wal`). The 
   file names can vary, depending on the current build variant.

# Check steps
1. Install the newer version of the SDK and its demo-app to the same device to check the switch to the new type of 
   the CompactBlocks cache storing
   1. Switch to the latest commit on the **Main** branch in your git client
   1. Update dependencies lock (if needed) and sync Gradle files
   1. Run the demo-app on the same emulator device as previously
1. Once the app is opened go through the same steps as previously to let the SDK apply the new cache storing
   mechanisms
1. Open the Device File Explorer in the Android Studio again
1. Go to `/data/data/cash.z.ecc.android.sdk.demoapp.mainnet/no_backup/co.electricoin.zcash/` 
   1. Verify there is a new `zcash_sdk_[network_name]_fs_cache` directory in place
   1. Verify it contains a `blockmeta.sqlite` block metadata database file
   1. Also verify that this directory contains a `blocks` directory with new block cache files in it
   1. Verify there is no `cache.sqlite3` database file, and check that no rollback files (suffixed with `journal` or
   `wal`) are present. The file names can vary, depending on the current build variant.
1. Inspect older legacy database folder `/data/data/cash.z.ecc.android.sdk.demoapp.mainnet/databases/`, which 
   also should not contain `cache.sqlite3` or rollback files.
1. Once the whole sync process is done, verify that the temporary `blocks` directory is removed from the device
   storage with all its blocks metadata files, or is empty, as it's automatically created by the new blocks polling
   mechanism.
1. Verify also that the `blockmeta.sqlite` still preserves
