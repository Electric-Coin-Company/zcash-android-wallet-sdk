# About
This manual test case provides information on how to manually test an implemented action of moving both of our 
sapling params files (`sapling-spend.params`, `sapling-output.params`) from legacy location `/cache/params/` to 
the preferred location `/no_backup/co.electricoin.zcash/`. The benefit of this approach is that the content of 
`no_backup` folder is not part of automatic user data backup to user's cloud storage. Our sapling files are quite big 
(up to 50MB).

# Prerequisite
- Installed [Android Studio](https://developer.android.com/studio)
- Ideally two emulators with min and max supported API level
- A working git client
- Cloned [Zcash Android SDK repository](https://github.com/zcash/zcash-android-wallet-sdk)
- A wallet seed phrase with available funds  

# Prepare steps
1. Install a previous version of the SDK and its demo-app to create sapling files in the original `cache/params` folder
2. Switch back to commit **Check sapling files size [12c23dd0]** from Aug 26 2022 on the **Main** branch in your 
git client, or with this git command `git checkout 12c23dd054c687431aaf51bfc5f67d5dbc08625b`  
3. Update dependencies lock (if needed) and sync Gradle files
4. Run the demo-app on selected emulator
5. Once it's opened on the Home screen, change the wallet seed phrase to your preferred one to have some funds 
available, which can be spent for the purpose of this test
6. Go to the Send screen and wait for Downloading and Syncing processes to finish
7. Then type the ZEC amount you want to send and the Address to which you want the Zec amount sent 
8. Wait for send confirmation
9. Sapling params files should be now moved to the original location. Open Device File 
   Explorer from Android Studio bottom-left corner, select the same emulator device from the top 
drop-down menu. Go to `/data/data/cash.z.ecc.android.sdk.demoapp.mainnet/cache/params`
10. Verify there are `sapling-spend.params` and `sapling-output.params`

# Move steps
1. Install the newer version of the SDK and its demo-app to the same device to check the database files move operation
result
   1. Switch to the latest commit on the **Main** branch in your git client
   2. Update dependencies lock (if needed) and sync Gradle files
   3. Run the demo-app on the same emulator device as previously
2. Once the app is opened, go to the Device File Explorer from Android Studio bottom-left corner again
3. Go to `/data/data/cash.z.ecc.android.sdk.demoapp.mainnet/cache/params` again, now there shouldn't be our sapling 
   params files placed in the folder and the folder `/params/` should be missing
4. Go to `/data/data/cash.z.ecc.android.sdk.demoapp.mainnet/no_backup/co.electricoin.zcash`, which should be created 
automatically
5. Now verify there are the same files placed in the `no_backup/co.electricoin.zcash` folder as in `cache/params` were

# Check result
Ideally run this test (Prepare and Move steps) for both emulators (min and max supported API level) to ensure the 
correct functionality on both Android version. There is a difference in implementation for these Android versions, 
but the result should be the same.