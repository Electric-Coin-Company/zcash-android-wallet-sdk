# About
This manual test case provides information on how to manually test an implemented action of downloading both of our 
sapling params files (`sapling-spend.params`, `sapling-output.params`) to  the preferred location 
`/no_backup/co.electricoin.zcash/`. The benefit of this approach is that the content of `no_backup` folder is not part 
of automatic user data backup to user's cloud storage. Our sapling files are quite big (up to 50MB).

# Prerequisite
- Installed [Android Studio](https://developer.android.com/studio)
- Ideally two emulators with min and max supported API level
- A working git client
- Cloned [Zcash Android SDK repository](https://github.com/zcash/zcash-android-wallet-sdk)
- A wallet seed phrase with available funds

# Download files steps
1. Remove a previous version of the demo-app from the emulator, if there is any 
2. Install the latest version of the demo-app from the latest commit on the **Main** branch
3. Run the demo-app on selected emulator
4. Once it's opened on the Home screen, change the wallet seed phrase to your preferred one to have some funds
available, which can be spent for the purpose of this test
5. Go to the Send screen and wait for Downloading and Syncing processes to finish
6. Then type the ZEC amount you want to send and the Address to which you want the Zec amount sent
7. Wait for send confirmation
8. Sapling params files should be now downloaded in the preferred location. Open Device File Explorer from Android 
Studio bottom-left corner, select the same emulator device from the top. Go to
`/data/data/cash.z.ecc.android.sdk.demoapp.mainnet/no_backup/co.electricoin.zcash`, which should be created 
automatically
9. Now verify there both of our sapling params files (`sapling-spend.params`, `sapling-output.params`) placed in the 
`no_backup/co.electricoin.zcash` folder

# Check result
Ideally run this test for both emulators (min and max supported API level) to ensure the correct functionality on both
Android version. There is a difference in implementation for these Android versions, but the result should be the same.