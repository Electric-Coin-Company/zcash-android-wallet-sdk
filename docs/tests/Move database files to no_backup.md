# About
This manual test case provides information on how to manually test an implemented action of moving all of our databases 
files from default `/databases/` to preferred `/no_backup/co.electricoin.zcash` directory. The benefit of this approach
is that the content `no_backup` folder is not part of automatic user data backup to user's cloud storage. Our databases 
can contain potentially big and sensitive data.

The move feature takes all related files (database file itself as well as `journal` and `wal` rollback files) and moves 
them only once on app start (before first database access) when a client app uses an updated version of this SDK.

# Prerequisite
- Installed [Android Studio](https://developer.android.com/studio)
- Ideally two emulators with min and max supported API level
- A working git client
- Cloned [Zcash Android SDK repository](https://github.com/zcash/zcash-android-wallet-sdk)

# Prepare steps
1. Install a previous version of the SDK and its demo-app to create database files in the original `database` folder
2. Switch back to commit **Bump version to 1.8.0-beta01 [3fda6da]** from Jul 11 2022 on the **Main** branch in your 
   git client, or with this git command `git checkout 3fda6da1cae5b83174e5b1e020c91dfe95d93458`  
3. Update dependencies lock (if needed) and sync Gradle files
4. Run the demo-app on selected emulator
5. Once it's opened go through the app to let the SDK create all the database files. Visit these screens step by step 
   from the side menu:
   1. Get Balance
   2. List Transactions
   3. List UTXOs
6. Open Device File Explorer from Android Studio bottom-left corner, select the same emulator device from the top 
drop-down menu
7. Go to `/data/data/cash.z.ecc.android.sdk.demoapp.mainnet/databases`
8. Verify there are `data.db`, `cache.db` and `utxos.db` files (their names can vary, depends on the current build 
   variant). There can be several rollback files created. 

# Move steps
1. Install the newer version of the SDK and its demo-app to the same device to check the database files move operation
result
   1. Switch to the latest commit on the **Main** branch in your git client
   2. Update dependencies lock (if needed) and sync Gradle files
   3. Run the demo-app on the same emulator device as previously
2. Once the app is opened go through the same steps as previously to let the SDK apply the move mechanisms to all our 
database files. Visit these screens step by step from the side menu:
   1. Get Balance
   2. List Transactions
   3. List UTXOs
3. Go to the Device File Explorer from Android Studio bottom-left corner again
4. Go to `/data/data/cash.z.ecc.android.sdk.demoapp.mainnet/databases` again, now there shouldn't be any files placed 
in the `database` folder
5. Go to `/data/data/cash.z.ecc.android.sdk.demoapp.mainnet/no_backup/co.electricoin.zcash`, which should be created 
automatically
6. Now verify there are the same files placed in the `no_backup/co.electricoin.zcash` folder as in `databases` were
7. To be sure everything is alright, just visit several screens from the side-menu and see no unexpected behavior 

# Check result
Ideally run this test (Prepare and Move steps) for both emulators (min and max supported API level) to ensure the 
correct functionality on both Android version. There is a difference in implementation for these Android versions, but
the result should be the same. 