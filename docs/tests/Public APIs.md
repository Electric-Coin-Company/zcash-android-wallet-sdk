# Public APIs
The SDK has a variety of public APIs that should be kept stable for SDK consumers, unless an explicit decision is made to introduce a breaking API change.  To reduce the introduction of incompatible changes, these test cases covers how to verify our own apps don't break.

# Compile Compatibility
1. Publish the SDK to mavenLocal
    1. Bump the SDK version in [gradle.properties](../../gradle.properties)
    1. Navigate to the root of the SDK checkout
    1. Run the Gradle task `./gradlew publishToMavenLocal`
1. Modify the wallet app to build against the new SDK
    1. Check out the [Zcash Android Wallet](https://github.com/zcash/zcash-android-wallet)
    1. Modify settings.gradle to additionally include `mavenLocal()` in the `dependencyResolutionManagement` block
    1. Modify [Dependencies.kt](https://github.com/zcash/zcash-android-wallet/blob/master/buildSrc/src/main/java/cash/z/ecc/android/Dependencies.kt) to use the new SDK version defined in the first step
1. Navigate to the root of the wallet app checkout
1. Build the wallet app and unit tests with the task `./gradlew assembleAndroidTest`
1. Verify the build completes successfully

# Upgrade Compatibility
1. Install the wallet app using the old SDK
    1. Ensure that no version of the Android wallet is currently installed on your test Android device or emulator
    1. Build the unmodified version of the wallet app
    1. Run the wallet app and create a new wallet
1. Publish the SDK to mavenLocal
    1. Bump the SDK version in [gradle.properties](../../gradle.properties)
    1. Navigate to the root of the SDK checkout
    1. Run the Gradle task `./gradlew publishToMavenLocal`
1. Modify the wallet app to build against the new SDK
    1. Check out the [Zcash Android Wallet](https://github.com/zcash/zcash-android-wallet)
    1. Modify settings.gradle to additionally include `mavenLocal()` in the `dependencyResolutionManagement` block
    1. Modify [Dependencies.kt](https://github.com/zcash/zcash-android-wallet/blob/master/buildSrc/src/main/java/cash/z/ecc/android/Dependencies.kt) to use the new SDK version defined in the first step
1. Install the wallet app using the new SDK
1. Verify that the wallet behaves correctly (it should not act as if it is starting fresh; user data persisted by the old version of the SDK should still be present after SDK upgrade)