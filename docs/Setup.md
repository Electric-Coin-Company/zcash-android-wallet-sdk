# Setup Guide
The SDK is built with Gradle and can be compiled on macOS, Windows, and Linux.  Development is typically done with the latest stable release of Android Studio.

Tip: On macOS and Linux, Gradle is invoked with `./gradlew`.  On Windows, Gradle is invoked with `gradlew`.  Throughout the documentation, the macOS and Linux syntax is used by default.

Tip: You do NOT need to install Gradle yourself.  Running it from the command line or building the application within Android Studio will download the necessary dependencies automatically.

## Step by Step
To get set up for development, there are several steps that you need to go through.  Going through these steps in order is important, as each step in the progression builds on the prior steps.   These steps are written assuming a fresh development environment.

Start by making sure the command line with Gradle works first, because **all the Android Studio run configurations use Gradle internally.**  The run configurations are not magic—they map directly to command line invocations with different arguments.

1. Install Java
    1. Install JVM 11 or greater on your system.  Our setup has been tested with Java 11-17.  Although a variety of JVM distributions are available and should work, we have settled on recommending [Adoptium/Temurin](https://adoptium.net), because this is the default distribution used by Gradle toolchains.  For Windows or Linux, be sure that the `JAVA_HOME` environment variable points to the right Java version.  Note: If you switch from a newer to an older JVM version, you may see an error like the following `> com.android.ide.common.signing.KeytoolException: Failed to read key AndroidDebugKey from store "~/.android/debug.keystore": Integrity check failed: java.security.NoSuchAlgorithmException: Algorithm HmacPBESHA256 not available`.  A solution is to delete the debug keystore and allow it to be re-generated.
    1. Android Studio has an embedded JVM, although running Gradle tasks from the command line requires a separate JVM to be installed.  Our Gradle scripts are configured to use toolchains to automatically install the correct JVM version.
1. Configure Rust
    1. [Install Rust](https://www.rust-lang.org/learn/get-started). You will need Rust 1.60 or greater. If you install with `rustup` then you are guaranteed to get a compatible Rust version. If you use system packages, check the provided version.
        1. macOS with Homebrew
            1. `brew install rustup`
            1. `rustup-init`
    1. Add Android targets
        ```bash
        rustup target add armv7-linux-androideabi aarch64-linux-android i686-linux-android x86_64-linux-android
        ```
1. Install python 2.7 
   1. macOS with Homebrew
      1. `brew install pyenv`
      1. `pyenv install 2.7.18`
      1. To enable pyenv in your bash shell run: `eval "$(pyenv init -)"`
      1. Get the path to python 2: `which python2`
      1. Add `rust.pythonCommand=PYTHON2 PATH` in `${sdkRootDir}/local.properties`
1. Install Android Studio and the Android SDK
    1. Download [Android Studio](https://developer.android.com/studio/).  We typically use the stable version of Android Studio, unless specifically noted due to short-term known issues.
    1. During the Android Studio setup wizard, choose the "Standard" setup option
    1. Note the file path where Android Studio will install the Android developer tools, as you will need this path later
    1. Continue and let Android Studio download and install the rest of the Android developer tools
    1. Do not open the project in Android Studio yet.  That happens in a subsequent step below.  At this stage, we're just using Android Studio to install the Android SDK.
    1. Configure `ANDROID_HOME` environment variable using the path noted above.  This will allow easily running Android development commands, like `adb logcat` or `adb install myapp.apk`
        1. macOS
            1. Add the following to `~/.zprofile`

                ```
                export ANDROID_HOME=THE_PATH_NOTED_ABOVE
                export PATH=${PATH}:${ANDROID_HOME}/tools
                export PATH=${PATH}:${ANDROID_HOME}/tools/bin
                export PATH=${PATH}:${ANDROID_HOME}/platform-tools
                ```
    1. Install the Android NDK
        1. Go to the Android SDK Manager inside Android Studio. Select the "SDK Tools" tab.
        1. Click the checkbox for "Show Package Details"
        1. Install the exact NDK version listed in [gradle.properties](../gradle.properties) under `ANDROID_NDK_VERSION`
    1. Configure a device for development and testing
        1. Android virtual device
            1. Inside Android Studio, the small More Actions button has an option to open the Virtual Device Manager
            1. When configuring an Android Virtual Device (AVD),  choose an Android version that is within the range of our min and target SDK versions, defined in [gradle.properties](../gradle.properties).
        1. Physical Android device
            1. Enable developer mode
                1. Go into the Android settings
                1. Go to About phone
                1. Tap on the build number seven times (some devices hide this under "Software information")
                1. Go back and navigate to the newly enabled Developer options.  This may be a top-level item or under System > Developer options
                1. Enable USB debugging
                1. Connect your device to your computer, granting permission to the USB MAC address
1. Check out the code.  _Use the command line (instead of Android Studio) to check out the code. This will ensure that your command line environment is set up correctly and avoids a few pitfalls with trying to use Android Studio directly.  Android Studio's built-in git client is not as robust as standalone clients_
    1. To check out a git repo from GitHub, there are three authentication methods: SSH, HTTPS, and GitHub API.  We recommend SSH.
    1. Create a new SSH key, following [GitHub's instructions](https://docs.github.com/en/authentication/connecting-to-github-with-ssh/generating-a-new-ssh-key-and-adding-it-to-the-ssh-agent)
    1. Add the SSH key under [GitHub account settings](https://github.com/settings/keys)
    1. Clone repo in a terminal on your computer `git clone git@github.com:zcash/zcash-android-wallet-sdk.git`
1. Compile from the command line
    1. Navigate to the repo checkout in a terminal
    1. Compile the SDK and Demo App with the gradle command `./gradlew assemble`
1. Compile from Android Studio
    1. Open Android Studio
    1. From within Android Studio, choose to open an existing project and navigate to the root of the checked out repo.  Point Android Studio to the root of the git repo as (do not point it to the `sdk-lib` or `demo-app` modules, as that those are just a subset of the project and cannot be opened by themselves)
        1. Note: When first opening the project, Android Studio will warn that Gradle checksums are not fully supported.  Choose the "Use checksum" option.  This is a security feature that we have explicitly enabled.
        1. Shortly after opening the project, Android Studio may prompt about updating the Android Gradle Plugin.  DO NOT DO THIS.  If you do so, the build will fail because the project also has dependency locking enabled as a security feature.  To learn more, see [Build integrity.md](Build%20Integrity.md)
        1. Android Studio may prompt about updating the Kotlin plugin.  Do this.  Our application often uses a newer version of Kotlin than is bundled with Android Studio.
        1. Note that some versions of Android Studio on Intel machines have trouble with dependency locks.  If you experience this issue, the workaround is to add the following line to `~/.gradle/gradle.properties` `ZCASH_IS_DEPENDENCY_LOCKING_ENABLED=false`
    1. After Android Studio finishes syncing with Gradle, look for the green "play" run button in the toolbar.  To the left of it, choose the "demo-app" run configuration under the dropdown menu.  Then hit the run button.
        1. Note: The SDK supports both testnet and mainnet.  The decision to switch between them is made at the application level.  To switch between build variants, look for "Build Variants" which is usually a small button in the left gutter of the Android Studio window.

## Troubleshooting
1. Verify that the Git repo has not been modified.  Due to strict dependency locking (for security reasons), the build will fail unless the locks are also updated
1. Try running from the command line instead of Android Studio, to rule out Android Studio issues.  If it works from the command line, try this step to reset Android Studio
   1. Quit Android Studio
   1. Delete the invisible `.idea` in the root directory of the project.  This directory is partially ignored by Git, so deleting it will remove the files that are untracked
   1. Restore the missing files in `.idea` folder from Git
   1. Relaunch Android Studio
1. Clean the individual Gradle project by running `./gradlew clean` which will purge local build outputs.
1. Run Gradle with the argument `--rerun-tasks` which will effectively disable the build cache by re-running tasks and repopulating the cache.  E.g. `./gradlew assemble --rerun-tasks`
1. Reboot your computer, which will ensure that Gradle and Kotlin daemons are completely killed and relaunched
1. Delete the global Gradle cache under `~/.gradle/caches`
1. If adding a new dependency or updating a dependency, a warning that a dependency cannot be found may indicate the Maven repository restrictions need adjusting

## Gradle Tasks
A variety of Gradle tasks are set up within the project, and these tasks are also accessible in Android Studio as run configurations.
 * `assemble` - Compiles the SDK and demo application but does not deploy it
 * `sdk-lib:test` - Runs unit tests in the SDK that don't require Android.  This is generally a small number of tests against plain Kotlin code without Android dependencies.
 * `sdk-lib:connectedAndroidTest` - Runs the tests against the SDK that require integration with Android.
 * `darkside-test-lib:connectedAndroidTest` - Runs the tests against the SDK which require a localhost lightwalletd server running in darkside mode
 * `assembleAndroidTest` - Compiles the application and tests, but does not deploy the application or run the tests.  The Android Studio run configuration actually runs all of these tasks because the debug APKs are necessary to run the tests: `assembleDebug assembleZcashmainnetDebug assembleZcashtestnetDebug assembleAndroidTest`
 * `detektAll` - Performs static analysis with Detekt
 * `ktlintFormat` - Performs code formatting checks with ktlint
 * `lint` - Performs static analysis with Android lint
 * `dependencyUpdates` - Checks for available dependency updates.  It will only suggest final releases, unless a particular dependency is already using a non-final release (e.g. alpha, beta, RC).

Gradle Managed Devices are also configured with our build scripts.  We have found best results running tests one module at a time, rather than trying to run them all at once.  For example: `./gradlew :sdk-lib:pixel2TargetDebugAndroidTest` will run the SDK tests on a Pixel 2 sized device using our target API version.

## Gradle Properties
A variety of Gradle properties can be used to configure the build.  Most of these properties are optional and help with advanced configuration.  If you're just doing local development or making a small pull request contribution, you likely do not need to worry about these.

### Debug Signing
By default, the application is signed by the developers automatically generated debug signing key.  In a team of developers, it may be advantageous to share a debug key so that debug builds can access key-restricted services such as Firebase or Google Maps.  For such a setup, the path to a shared debug signing key can be set with the property `ZCASH_DEBUG_KEYSTORE_PATH`.

### Firebase Test Lab
This section is optional.

For Continuous Integration, see [CI.md](CI.md).  The rest of this section is regarding local development.

1. Configure or request access to a Firebase Test Lab project
    1. If you are an Electric Coin Co team member: Make an IT request to add your Google account to the existing Firebase Test Lab project 
    1. If you are an open source contributor: set up your own Firebase project for the purpose of running Firebase Test Lab
1. Set the Firebase Google Cloud project name as a global Gradle property `ZCASH_FIREBASE_TEST_LAB_PROJECT` under `~/.gradle/gradle.properties`
1. Run the Gradle task `flankAuth` to generate a Firebase authentication token on your machine. Make sure you have Editor role, at
   least in the Firebase project, to be able to authenticate successfully. Note that Gradle 
   may report the task failed yet still successfully store the token.

Tests can now be run on Firebase Test Lab from your local machine.

The Firebase Test Lab tasks DO NOT build the app, so they rely on existing build outputs.  This means you should:
1. Build the debug and test APKs: `./gradlew assembleDebug assembleZcashmainnetDebug`
1. Run the tests: `./gradlew runFlank`

### Emulator WTF
This section is optional.  

For Continuous Integration, see [CI.md](CI.md).  The rest of this section is regarding local development.

1. Configure or request access to emulator.wtf
    1. If you are an Electric Coin Co team member: We are still setting up a process for this, because emulator.wtf does not yet support individual API tokens
    1. If you are an open source contributor: Visit http://emulator.wtf and request an API key
1. Set the emulator.wtf API key as a global Gradle property `ZCASH_EMULATOR_WTF_API_KEY` under `~/.gradle/gradle.properties` 
1. Run the Gradle task `./gradlew testDebugWithEmulatorWtf` (emulator.wtf tasks do build the tests and test APKs, so you don't need to build them beforehand.  This is a different behavior compared to Firebase Test Lab)
