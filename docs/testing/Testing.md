# Testing
This documentation outlines our approach to testing. By running tests against our app consistently, we verify the 
SDK's correctness, functional behavior, and usability before releasing it publicly.

## Automated testing

- TBD
<!-- TODO [#807]: Testing documentation update --> 
<!-- TODO [#807]: https://github.com/zcash/zcash-android-wallet-sdk/issues/807 --> 

## Manual testing

We aim to automate as much as we possibly can. Still manual testing is really important for Quality Assurance.

Here you'll find our manual testing scripts. When developing a new feature you can add your own that provide the proper steps to properly test it.

## Gathering Code Coverage
The app consists of different Gradle module types (e.g. Kotlin Multiplatform, Android).  Generating coverage for these different module types requires different command line invocations.

### Kotlin Multiplatform
Kotlin Multiplatform does not support coverage for all platforms.  Most of our code lives under commonMain, with a JVM target.  This effectively allows generation of coverage reports with Jacoco.  Coverage is enabled by default when running `./gradlew check`.

### Android
The Android Gradle plugin supports code coverage with Jacoco.  This integration can sometimes be buggy.  For that reason, coverage is disabled by default and can be enabled on a case-by-case basis, by passing `-PIS_ANDROID_INSTRUMENTATION_TEST_COVERAGE_ENABLED=true` as a command line argument for Gradle builds.  For example: `./gradlew connectedCheck -PIS_ANDROID_INSTRUMENTATION_TEST_COVERAGE_ENABLED=true`.

When coverage is enabled, running instrumentation tests will automatically generate coverage reports stored under `$module/build/reports/coverage`.

## Benchmarking
This section provides information about available benchmark tests integrated in this project as well as how to use them. Currently, we support macrobenchmark tests run locally as described in the Android [documentation](https://developer.android.com/topic/performance/benchmarking/benchmarking-overview).

We provide dedicated benchmark test module `demo-app-benchmark-test` for this. If you want to run these benchmark
tests against our demo application, make sure you have a physical device connected with Android SDK level 29, at least.
Select `benchmark` build variant for this module. Make sure that other modules are set to benchmark
type too. The benchmark tests can be run with Android Studio run configuration
`ui-benchmark-test:connectedZcashmainnetBenchmarkAndroidTest`. Running the benchmark test this way automatically
provides benchmarking results in Run panel. Or you can run the tests manually from the terminal with `./gradlew connectedZcashmainnetBenchmarkAndroidTest` and analyze results with Android Studio's Profiler or [Perfetto](https://ui.perfetto.dev/) tool, as described in this Android [documentation](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview#access-trace).

**Note**: We've enabled benchmarking also for emulators, although it's always better to run the tests on a real physical device. Emulator benchmark improvements might not carry over to a real user's experience (or even regress real device performance).

### Referential benchmark tests results 
Every few months, or before a major SDK release, we run and compare benchmark test results to avoid making the SDK's mechanisms significantly slower.

**Note**: If possible, run the benchmark tests on a physical device with sufficient empty disk space, connected to the
internet and charged or plugged-in to a charger. It's always better to restart the device before approaching to 
running the benchmark tests. Also, please, ensure you're running it on the latest main branch
commits of that date. Generate tests results with the Android Studio run configuration
`ui-benchmark-test:connectedZcashmainnetBenchmarkAndroidTest` and gather results from the Run panel.

#### Dec 7, 2022:

- SDK version: `1.10.0-beta01`
- Git branch: `789-benchmark-demo-app`
- Device: 
  - Pixel 6 - Android 13:
    ```
    Starting 3 tests on Pixel 6 - 13
    
    StartupBenchmark_appStartup
    timeToInitialDisplayMs   min 257.6,   median 278.9,   max 293.9
    Traces: Iteration 0 1 2 3 4
    
    StartupBenchmark_tracesSdkStartup
    ADDRESS_SCREENMs   min 690.0,   median 844.8,   max 941.3
    SAPLING_ADDRESSMs   min   4.4,   median   5.3,   max   6.0
    TRANSPARENT_ADDRESSMs   min   2.4,   median   4.7,   max   5.7
    UNIFIED_ADDRESSMs   min   1.4,   median   1.7,   max   4.7
    Traces: Iteration 0 1 2 3 4
    
    SyncBlockchainBenchmark_tracesSyncBlockchain
    BALANCE_SCREENMs   min 15,909.1,   median 16,225.6,   max 16,388.2
    BLOCKCHAIN_SYNCMs   min 15,255.1,   median 15,623.1,   max 15,757.6
    DOWNLOADMs   min 14,345.5,   median 14,371.9,   max 14,704.2
    SCANMs   min    788.2,   median    981.1,   max  1,183.1
    VALIDATIONMs   min     67.1,   median     71.2,   max    115.4
    Traces: Iteration 0 1 2
    
    BUILD SUCCESSFUL in 2m 17s
    ```
  - Pixel 3a - Android 12:
    ```
    Starting 3 tests on Pixel 3a - 12
    
    StartupBenchmark_appStartup
    timeToInitialDisplayMs   min 514.0,   median 515.6,   max 530.8
    Traces: Iteration 0 1 2 3 4
    
    StartupBenchmark_tracesSdkStartup
    ADDRESS_SCREENMs   min   810.9,   median   897.5,   max 1,409.6
    SAPLING_ADDRESSMs   min     3.1,   median     5.9,   max     7.7
    TRANSPARENT_ADDRESSMs   min     4.9,   median     7.6,   max    11.5
    UNIFIED_ADDRESSMs   min     2.6,   median     4.5,   max     5.4
    Traces: Iteration 0 1 2 3 4
    
    SyncBlockchainBenchmark_tracesSyncBlockchain
    BALANCE_SCREENMs   min 15,442.0,   median 15,471.3,   max 15,681.0
    BLOCKCHAIN_SYNCMs   min 14,808.8,   median 14,837.3,   max 15,019.5
    DOWNLOADMs   min 13,843.8,   median 13,899.8,   max 14,016.8
    SCANMs   min    867.6,   median    883.3,   max    921.1
    VALIDATIONMs   min     67.9,   median     79.7,   max     80.0
    Traces: Iteration 0 1 2
    
    BUILD SUCCESSFUL in 2m 50s
    ```






